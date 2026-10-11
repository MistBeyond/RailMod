package com.mistbeyond.rail.client.rail.model;

import com.mistbeyond.rail.Ids;
import com.mistbeyond.rail.api.rail.graph.GridDirection;
import com.mistbeyond.rail.api.rail.section.SignalAspect;
import com.mistbeyond.rail.block.rail.RailSignalBlock;
import com.mojang.math.OctahedralGroup;
import com.mojang.math.Quadrant;
import com.mojang.math.Transformation;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.CuboidRotation;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Baked-model route for the {@link RailSignalBlock} (ADR 0012): a custom unbaked block-state model that mirrors the
 * fixed signal-light template {@code assets/mrail/models/block/block_signal.json} element by element and resolves
 * the per-state appearance from the block state. The template faces north; the facing property rotates the whole
 * model about the block center (90-degree steps through {@link BlockModelRotation}, 45-degree diagonals through an
 * arbitrary Y rotation, exactly like the diagonal track geometry). The RED/GREEN lens materials are selected by
 * the aspect property; the lit lens is emitted light so it stays bright at night, and the dark lens reuses the
 * trim texture.
 *
 * <p>Quads are baked through the vanilla {@link FaceBakery} with the same conventions as the track dynamic model,
 * including the template's element-level rotations: the octagonal housing facets are single 45-degree Y rotations
 * and the lens facets are 45-degree Z rotations about their pixel origins (applied as {@link CuboidRotation} in
 * normalized coordinates).
 */
public final class RailSignalBlockModel implements CustomUnbakedBlockStateModel {
    public static final RailSignalBlockModel INSTANCE = new RailSignalBlockModel();
    public static final MapCodec<RailSignalBlockModel> CODEC = MapCodec.unit(INSTANCE);

    private static final Material BODY_MATERIAL = new Material(Identifier.withDefaultNamespace("block/gray_concrete"));
    private static final Material TRIM_MATERIAL = new Material(Identifier.withDefaultNamespace("block/black_concrete"));
    private static final Material RED_LENS_MATERIAL = new Material(Identifier.withDefaultNamespace("block/redstone_block"));
    private static final Material GREEN_LENS_MATERIAL = new Material(Identifier.withDefaultNamespace("block/emerald_block"));
    private static final ModelDebugName DEBUG_NAME = () -> Ids.MOD_ID + ":rail_block_signal";

    /**
     * Light emission of the lit lens, so the active lamp is readable in the dark. The dark lens uses the trim
     * texture with no emission.
     */
    private static final int LIT_LENS_EMISSION = 15;

    /**
     * Which template texture an element samples.
     */
    enum Lens {
        BODY,
        TRIM,
        RED,
        GREEN
    }

    /**
     * One template element rotation: a single-axis 45-degree tilt about a pixel origin, exactly the element-level
     * {@code rotation} of the Blockbench template.
     */
    record Tilt(Direction.Axis axis, float angleDegrees, float originX, float originY, float originZ) {
    }

    /**
     * One axis-aligned element box of the fixed template, in pixel coordinates, with an optional tilt.
     */
    record Element(
            float fromX,
            float fromY,
            float fromZ,
            float toX,
            float toY,
            float toZ,
            Lens lens,
            @Nullable Tilt tilt
    ) {
    }

    /**
     * The fixed template geometry, mirrored from {@code block_signal.json} (base, pole, red and green heads with
     * octagonal housing facets, lens facets and visors). All models face north; the facing rotation is applied on
     * top through the model state.
     */
    static final List<Element> ELEMENTS = List.of(
            // 基座底层 / 基座上层 / 信号柱
            new Element(5.0F, 0.0F, 5.0F, 11.0F, 0.75F, 11.0F, Lens.BODY, null),
            new Element(6.25F, 0.75F, 6.25F, 9.75F, 1.75F, 9.75F, Lens.BODY, null),
            new Element(7.5F, 1.75F, 7.5F, 8.5F, 13.0F, 8.5F, Lens.BODY, null),
            // 红灯机构（下）
            new Element(6.25F, 3.0F, 6.25F, 9.75F, 6.75F, 9.75F, Lens.BODY, null),
            new Element(6.25F, 2.995F, 6.25F, 9.75F, 6.755F, 9.75F, Lens.BODY, new Tilt(Direction.Axis.Y, 45.0F, 8.0F, 4.875F, 8.0F)),
            new Element(6.9F, 3.575F, 5.0F, 9.1F, 5.775F, 6.9F, Lens.RED, null),
            new Element(6.9F, 3.575F, 5.01F, 9.1F, 5.775F, 6.89F, Lens.RED, new Tilt(Direction.Axis.Z, 45.0F, 8.0F, 4.675F, 8.0F)),
            new Element(6.5F, 6.325F, 4.9F, 9.5F, 6.725F, 6.6F, Lens.TRIM, null),
            // 绿灯机构（上）
            new Element(6.25F, 7.15F, 6.25F, 9.75F, 10.9F, 9.75F, Lens.BODY, null),
            new Element(6.25F, 7.145F, 6.25F, 9.75F, 10.905F, 9.75F, Lens.BODY, new Tilt(Direction.Axis.Y, 45.0F, 8.0F, 9.025F, 8.0F)),
            new Element(6.9F, 7.725F, 5.0F, 9.1F, 9.925F, 6.9F, Lens.GREEN, null),
            new Element(6.9F, 7.725F, 5.01F, 9.1F, 9.925F, 6.89F, Lens.GREEN, new Tilt(Direction.Axis.Z, 45.0F, 8.0F, 8.825F, 8.0F)),
            new Element(6.5F, 10.475F, 4.9F, 9.5F, 10.875F, 6.6F, Lens.TRIM, null)
    );

    private RailSignalBlockModel() {
    }

    @Override
    public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
        return CODEC;
    }

    @Override
    public void resolveDependencies(ResolvableModel.Resolver resolver) {
        // The template textures live in the block atlas, which is always loaded; there are no model-file
        // dependencies.
    }

    @Override
    public BlockStateModel bake(ModelBaker baker) {
        Material.Baked body = baker.materials().get(BODY_MATERIAL, DEBUG_NAME);
        Material.Baked trim = baker.materials().get(TRIM_MATERIAL, DEBUG_NAME);
        Material.Baked red = baker.materials().get(RED_LENS_MATERIAL, DEBUG_NAME);
        Material.Baked green = baker.materials().get(GREEN_LENS_MATERIAL, DEBUG_NAME);
        return new Baked(body, trim, red, green, baker);
    }

    /**
     * The Y rotation (degrees, joml {@code rotationY} convention) that turns the north-facing template towards
     * {@code facing}. NORTH is the unrotated template.
     */
    static float facingDegrees(GridDirection facing) {
        return switch (facing) {
            case NORTH -> 0.0F;
            case NORTH_EAST -> 315.0F;
            case EAST -> 270.0F;
            case SOUTH_EAST -> 225.0F;
            case SOUTH -> 180.0F;
            case SOUTH_WEST -> 135.0F;
            case WEST -> 90.0F;
            case NORTH_WEST -> 45.0F;
        };
    }

    /**
     * Per-face UVs in the same box-extent convention the template and the track geometry use ({@code [0, 0,
     * width, height]} with the face's extent in its two tangent axes).
     */
    private static CuboidFace.UVs faceUvs(Element element, Direction face) {
        float dx = element.toX() - element.fromX();
        float dy = element.toY() - element.fromY();
        float dz = element.toZ() - element.fromZ();
        return switch (face.getAxis()) {
            case X -> new CuboidFace.UVs(0.0F, 0.0F, dz, dy);
            case Y -> new CuboidFace.UVs(0.0F, 0.0F, dx, dz);
            case Z -> new CuboidFace.UVs(0.0F, 0.0F, dx, dy);
        };
    }

    /**
     * The model state for one facing angle: 90-degree multiples map to {@link BlockModelRotation} (with its UV
     * transform and winding recalculation, exactly like a vanilla variant rotation), diagonal angles use a pure Y
     * rotation about the block center (no winding recalculation), mirroring the diagonal track handling.
     */
    private static ModelState modelState(float degrees) {
        int rounded = Math.round(degrees) % 360;
        if (rounded < 0) {
            rounded += 360;
        }
        return switch (rounded) {
            case 0 -> BlockModelRotation.IDENTITY;
            case 90 -> BlockModelRotation.get(OctahedralGroup.ROT_90_Y_POS);
            case 180 -> BlockModelRotation.get(OctahedralGroup.ROT_180_FACE_XZ);
            case 270 -> BlockModelRotation.get(OctahedralGroup.ROT_90_Y_NEG);
            default -> new ModelState() {
                @Override
                public Transformation transformation() {
                    return new Transformation(new Matrix4f().rotationY((float) Math.toRadians(degrees)));
                }
            };
        };
    }

    /**
     * The per-state baked model. Immutable and thread-safe once baked; the facing and aspect enter through the
     * block state during chunk meshing.
     */
    private record Baked(
            Material.Baked body,
            Material.Baked trim,
            Material.Baked redLens,
            Material.Baked greenLens,
            ModelBaker baker
    ) implements BlockStateModel {
        @Override
        public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
                                 List<BlockStateModelPart> parts) {
            GridDirection facing = state.getValue(RailSignalBlock.FACING);
            boolean greenLit = state.getValue(RailSignalBlock.ASPECT) == SignalAspect.GREEN;
            parts.add(new BakedPart(bakeQuads(facing, greenLit), body));
        }

        @Override
        public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
            // Deprecated fallback without state context; the chunk mesher uses the level-aware overload.
        }

        @Override
        public Material.Baked particleMaterial() {
            return body;
        }

        @Override
        public int materialFlags() {
            return 0;
        }

        private List<BakedQuad> bakeQuads(GridDirection facing, boolean greenLit) {
            ModelState modelState = modelState(facingDegrees(facing));
            List<BakedQuad> quads = new ArrayList<>(ELEMENTS.size() * 6);
            for (Element element : ELEMENTS) {
                boolean lit = element.lens() == Lens.RED ? !greenLit : element.lens() == Lens.GREEN && greenLit;
                Material.Baked material = switch (element.lens()) {
                    case BODY -> body;
                    case TRIM -> trim;
                    case RED -> lit ? redLens : trim;
                    case GREEN -> lit ? greenLens : trim;
                };
                int lightEmission = lit ? LIT_LENS_EMISSION : 0;
                CuboidRotation rotation = element.tilt() == null ? null : cuboidRotation(element.tilt());
                Vector3f from = new Vector3f(element.fromX(), element.fromY(), element.fromZ());
                Vector3f to = new Vector3f(element.toX(), element.toY(), element.toZ());
                for (Direction face : Direction.values()) {
                    CuboidFace cuboidFace = new CuboidFace(null, -1, "mrail:signal", faceUvs(element, face), Quadrant.R0);
                    quads.add(FaceBakery.bakeQuad(
                            baker, from, to, cuboidFace, material, face, modelState, rotation, true, lightEmission));
                }
            }
            return quads;
        }

        /**
         * The template's element rotation as a vanilla {@link CuboidRotation}. Origins are normalized to the
         * 0..1 model space, matching the vanilla element rotation semantics.
         */
        private static CuboidRotation cuboidRotation(Tilt tilt) {
            return new CuboidRotation(
                    new Vector3f(tilt.originX() / 16.0F, tilt.originY() / 16.0F, tilt.originZ() / 16.0F),
                    new CuboidRotation.SingleAxisRotation(tilt.axis(), tilt.angleDegrees()),
                    false
            );
        }
    }

    private record BakedPart(List<BakedQuad> quads, Material.Baked particle) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction side) {
            if (side == null) {
                return quads;
            }
            List<BakedQuad> result = new ArrayList<>(4);
            for (BakedQuad quad : quads) {
                if (quad.direction() == side) {
                    result.add(quad);
                }
            }
            return result;
        }

        @Override
        public boolean useAmbientOcclusion() {
            return true;
        }

        @Override
        public Material.Baked particleMaterial() {
            return particle;
        }

        @Override
        public int materialFlags() {
            return 0;
        }
    }
}

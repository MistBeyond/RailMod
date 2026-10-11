package com.mistbeyond.rail.client.rail;

import com.mistbeyond.rail.api.rail.graph.GridDirection;
import com.mistbeyond.rail.block.rail.RailSignalSide;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.CustomBlockOutlineRenderer;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;

/**
 * Ghost preview of the block signal placement (ADR 0012): while aiming at a track cell with the block signal item,
 * renders the actual signal model translucently on both candidate positions — the active ghost side bright, the
 * opposite side faint, a blocked active position tinted red with a wire frame — plus an arrow along the facing
 * direction. The preview quads come from the signal block's own baked model, so the ghost is exactly what will be
 * placed.
 */
public class RailSignalGhostRenderer implements CustomBlockOutlineRenderer {
    private static final float LINE_WIDTH = 1.0F;
    private static final int ACTIVE_FREE = 0xFF40C860;
    private static final int ACTIVE_BLOCKED = 0xFFE04040;
    private static final int INACTIVE_FREE = 0x50FFFFFF;
    private static final int INACTIVE_BLOCKED = 0x50E04040;
    private static final int FULL_BRIGHT = 15728880;

    private final List<Ghost> ghosts;
    private final Vec3 arrowStart;
    private final Vec3 arrowTip;
    private final Vec3 arrowHeadLeft;
    private final Vec3 arrowHeadRight;
    private final int arrowColor;

    /**
     * @param model      the baked signal block model (state-independent)
     * @param states     the two candidate ghost states in {@code RailSignalSide#ordinal} order
     * @param positions  the two candidate world positions in the same order
     * @param activeSide the stack's ghost-preview side
     * @param freeBySide whether each candidate position is free (air)
     * @param facing     the snapped placement facing
     * @param cellCenter world-space center of the guarded track cell
     */
    public RailSignalGhostRenderer(
            BlockStateModel model,
            List<BlockState> states,
            List<BlockPos> positions,
            RailSignalSide activeSide,
            boolean[] freeBySide,
            GridDirection facing,
            Vec3 cellCenter
    ) {
        List<Ghost> ghosts = new ArrayList<>(2);
        for (int i = 0; i < 2; i++) {
            RailSignalSide side = i == 0 ? RailSignalSide.LEFT : RailSignalSide.RIGHT;
            ghosts.add(new Ghost(model, states.get(i), positions.get(i), side == activeSide, freeBySide[i]));
        }
        this.ghosts = List.copyOf(ghosts);
        double length = Math.sqrt((double) facing.dx() * facing.dx() + (double) facing.dz() * facing.dz());
        Vec3 forward = new Vec3(facing.dx() / length, 0.0, facing.dz() / length);
        Vec3 perpendicular = new Vec3(forward.z, 0.0, -forward.x);
        this.arrowStart = cellCenter.add(0.0, 0.9, 0.0);
        this.arrowTip = arrowStart.add(forward.scale(0.6));
        this.arrowHeadLeft = arrowTip.subtract(forward.scale(0.16)).subtract(perpendicular.scale(0.12));
        this.arrowHeadRight = arrowTip.subtract(forward.scale(0.16)).add(perpendicular.scale(0.12));
        Ghost active = ghosts.get(activeSide.ordinal());
        this.arrowColor = active.free() ? ACTIVE_FREE : ACTIVE_BLOCKED;
    }

    @Override
    public boolean render(
            BlockOutlineRenderState renderState,
            MultiBufferSource.BufferSource buffer,
            PoseStack poseStack,
            boolean translucentPass,
            LevelRenderState levelRenderState
    ) {
        Vec3 camera = levelRenderState.cameraRenderState.pos;
        RenderType ghostType = Sheets.translucentBlockItemSheet();
        VertexConsumer ghostConsumer = buffer.getBuffer(ghostType);
        for (Ghost ghost : ghosts) {
            if (ghost.active() && !ghost.free()) {
                emitQuads(poseStack, ghostConsumer, ghost, 1.0F, 0.42F, 0.42F, 0.55F, camera);
            } else if (ghost.active()) {
                emitQuads(poseStack, ghostConsumer, ghost, 1.0F, 1.0F, 1.0F, 0.60F, camera);
            } else {
                emitQuads(poseStack, ghostConsumer, ghost, 1.0F, 1.0F, 1.0F, 0.22F, camera);
            }
        }
        buffer.endBatch(ghostType);

        VertexConsumer lines = buffer.getBuffer(RenderTypes.lines());
        Ghost active = ghosts.get(0).active() ? ghosts.get(0) : ghosts.get(1);
        if (!active.free()) {
            drawBoxWire(lines, poseStack, active.pos(), ACTIVE_BLOCKED, camera);
        }
        drawSegment(lines, poseStack, arrowStart, arrowTip, arrowColor, camera);
        drawSegment(lines, poseStack, arrowTip, arrowHeadLeft, arrowColor, camera);
        drawSegment(lines, poseStack, arrowTip, arrowHeadRight, arrowColor, camera);
        buffer.endLastBatch();
        return false;
    }

    /**
     * Emits one ghost state's quads: the baked model parts are collected for the candidate position and written as
     * translucent, full-bright quads tinted by the ghost color, so the preview reads as the exact block that would
     * be placed.
     */
    private void emitQuads(
            PoseStack poseStack,
            VertexConsumer consumer,
            Ghost ghost,
            float r,
            float g,
            float b,
            float a,
            Vec3 camera
    ) {
        List<BlockStateModelPart> parts = new ArrayList<>();
        // The signal model ignores the level parameter (state-driven geometry), so the empty accessor suffices.
        ghost.model().collectParts(BlockAndTintGetter.EMPTY, ghost.pos(), ghost.state(), RandomSource.create(), parts);
        PoseStack.Pose pose = poseStack.last();
        for (BlockStateModelPart part : parts) {
            for (BakedQuad quad : part.getQuads(null)) {
                float shade = quad.materialInfo().shade() ? faceShade(quad.direction()) : 1.0F;
                Vector3f normal = quadNormal(quad);
                for (int v = 0; v < BakedQuad.VERTEX_COUNT; v++) {
                    Vector3fc p = quad.position(v);
                    consumer.addVertex(
                                    pose,
                                    (float) (ghost.pos().getX() + p.x() - camera.x()),
                                    (float) (ghost.pos().getY() + p.y() - camera.y()),
                                    (float) (ghost.pos().getZ() + p.z() - camera.z())
                            )
                            .setColor(r * shade, g * shade, b * shade, a)
                            .setUv(UVPair.unpackU(quad.packedUV(v)), UVPair.unpackV(quad.packedUV(v)))
                            .setLight(FULL_BRIGHT)
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setNormal(pose, normal.x, normal.y, normal.z);
                }
            }
        }
    }

    /**
     * The vanilla per-face diffuse shading table, so the ghost model reads with the same depth as the real block.
     */
    private static float faceShade(Direction direction) {
        return switch (direction) {
            case DOWN -> 0.5F;
            case UP -> 1.0F;
            case NORTH, SOUTH -> 0.8F;
            case WEST, EAST -> 0.6F;
        };
    }

    /**
     * The quad's outward normal, computed from the vertex winding the same way the vanilla bakery derives the face
     * direction.
     */
    private static Vector3f quadNormal(BakedQuad quad) {
        Vector3fc p0 = quad.position(0);
        Vector3fc p1 = quad.position(1);
        Vector3fc p2 = quad.position(2);
        Vector3f a = new Vector3f(p1).sub(p0);
        Vector3f c = new Vector3f(p2).sub(p0);
        Vector3f normal = a.cross(c);
        if (normal.lengthSquared() > 1.0E-9F) {
            normal.normalize();
        } else {
            normal.set(quad.direction().getStepX(), quad.direction().getStepY(), quad.direction().getStepZ());
        }
        return normal;
    }

    private static void drawBoxWire(VertexConsumer consumer, PoseStack poseStack, BlockPos pos, int color, Vec3 camera) {
        Vec3[] c = boxCorners(pos);
        int[][] edges = {{0, 1}, {1, 3}, {3, 2}, {2, 0}, {4, 5}, {5, 7}, {7, 6}, {6, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
        for (int[] edge : edges) {
            drawSegment(consumer, poseStack, c[edge[0]], c[edge[1]], color, camera);
        }
    }

    private static Vec3[] boxCorners(BlockPos pos) {
        return new Vec3[]{
                new Vec3(pos.getX(), pos.getY(), pos.getZ()),
                new Vec3(pos.getX() + 1, pos.getY(), pos.getZ()),
                new Vec3(pos.getX(), pos.getY(), pos.getZ() + 1),
                new Vec3(pos.getX() + 1, pos.getY(), pos.getZ() + 1),
                new Vec3(pos.getX(), pos.getY() + 1, pos.getZ()),
                new Vec3(pos.getX() + 1, pos.getY() + 1, pos.getZ()),
                new Vec3(pos.getX(), pos.getY() + 1, pos.getZ() + 1),
                new Vec3(pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1)
        };
    }

    private static void drawSegment(VertexConsumer consumer, PoseStack poseStack, Vec3 a, Vec3 b, int color, Vec3 camera) {
        PoseStack.Pose pose = poseStack.last();
        Vector3f direction = new Vector3f((float) (b.x - a.x), (float) (b.y - a.y), (float) (b.z - a.z));
        if (direction.lengthSquared() > 1.0E-9F) {
            direction.normalize();
        }
        consumer.addVertex(pose, (float) (a.x - camera.x), (float) (a.y - camera.y), (float) (a.z - camera.z))
                .setColor(color)
                .setNormal(pose, direction)
                .setLineWidth(LINE_WIDTH);
        consumer.addVertex(pose, (float) (b.x - camera.x), (float) (b.y - camera.y), (float) (b.z - camera.z))
                .setColor(color)
                .setNormal(pose, direction)
                .setLineWidth(LINE_WIDTH);
    }

    private record Ghost(BlockStateModel model, BlockState state, BlockPos pos, boolean active, boolean free) {
    }
}

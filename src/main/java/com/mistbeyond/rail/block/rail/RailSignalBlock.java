package com.mistbeyond.rail.block.rail;

import com.mistbeyond.rail.api.rail.graph.GridDirection;
import com.mistbeyond.rail.api.rail.graph.GridPos;
import com.mistbeyond.rail.api.rail.graph.SignalPlacement;
import com.mistbeyond.rail.api.rail.graph.TrackAxis;
import com.mistbeyond.rail.api.rail.graph.TrackCellData;
import com.mistbeyond.rail.api.rail.graph.TrackPlacement;
import com.mistbeyond.rail.api.rail.section.SignalAspect;
import com.mistbeyond.rail.api.rail.section.SignalType;
import com.mistbeyond.rail.core.rail.RailNetworkManager;
import com.mistbeyond.registry.RegisterBlock;
import com.mistbeyond.registry.SubscribeRegistration;
import com.mistbeyond.registry.impl.BlockRegistration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The world block of a rail block signal (ADR 0012). It stands in the World Grid cell adjacent to the guarded
 * track cell, on the {@link RailSignalSide} of its {@link #FACING} direction, and is removed by breaking; breaking
 * it also removes the matching {@link SignalPlacement} from the backing track cell. The {@link #ASPECT} property
 * carries the server-resolved RED/GREEN indication, so aspect changes flow through the vanilla block update path.
 * The signal survives only while its backing track cell exists.
 *
 * <p>The model renders through the dynamic baked-model route ({@code RailSignalBlockModel}) keyed by the block
 * state, mirroring the fixed signal-light template {@code assets/mrail/models/block/block_signal.json}. The block
 * has no BlockItem: placement goes through {@code RailBlockSignalItem}, which manages the track cell data next to
 * the world block.
 */
@RegisterBlock(registerBlockItem = false)
public class RailSignalBlock extends Block {
    /**
     * Facing direction the signal guards along; the signal's lights face this direction. Full 8-direction
     * {@link GridDirection} like the signal placement data (sections.md).
     */
    public static final EnumProperty<GridDirection> FACING = EnumProperty.create("facing", GridDirection.class);
    /**
     * Which side of the facing direction the signal stands on; selects the adjacent cell.
     */
    public static final EnumProperty<RailSignalSide> SIDE = EnumProperty.create("side", RailSignalSide.class);
    /**
     * Server-resolved indication. A misconfigured signal behaves as RED and currently renders as RED; the
     * dedicated ERROR overlay is deferred (ADR 0010 revision, ADR 0012).
     */
    public static final EnumProperty<SignalAspect> ASPECT = EnumProperty.create("aspect", SignalAspect.class);

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(5.0, 0.0, 5.0, 11.0, 1.75, 11.0),
            Block.box(7.0, 1.75, 7.0, 9.0, 13.0, 9.0)
    );

    public RailSignalBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, GridDirection.NORTH)
                .setValue(SIDE, RailSignalSide.LEFT)
                .setValue(ASPECT, SignalAspect.RED));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SIDE, ASPECT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /**
     * The signal stands only while its backing track cell exists; when the track is removed the signal block
     * disappears with it (and its data removal runs through {@link #affectNeighborsAfterRemoval}).
     */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        GridPos backing = backingCell(pos, state);
        return level.getBlockState(new BlockPos(backing.x(), backing.y(), backing.z())).getBlock()
                instanceof RailTrackCellBlock;
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState,
            RandomSource random
    ) {
        if (!state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    /**
     * Breaking the signal (or replacing it with another block) removes the matching block-signal placement from
     * the backing track cell, so section splitting and the indication state stay in sync with the visible world.
     * Same-block state updates (aspect refresh) do not trigger this hook.
     */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        GridDirection facing = state.getValue(FACING);
        GridPos cell = backingCell(pos, facing, state.getValue(SIDE));
        SignalPlacement placement = findPlacement(level, cell, facing);
        if (placement == null) {
            return;
        }
        RailSignalCellOps.removeSignal(level, cell, placement.id());
        RailNetworkManager.of(level).markCellChanged(cell);
    }

    /**
     * The block-signal placement this block represents in its backing cell, or {@code null} when the cell carries
     * no matching placement (for example legacy data without a world block).
     */
    private static @Nullable SignalPlacement findPlacement(ServerLevel level, GridPos cell, GridDirection facing) {
        BlockEntity blockEntity = level.getBlockEntity(new BlockPos(cell.x(), cell.y(), cell.z()));
        if (!(blockEntity instanceof RailTrackCellBlockEntity complex) || !complex.cell().equals(cell)) {
            return null;
        }
        for (SignalPlacement placement : complex.signals()) {
            if (placement.type() == SignalType.BLOCK && placement.direction() == facing) {
                return placement;
            }
        }
        return null;
    }

    /**
     * The World Grid cell of the track this signal guards: the adjacent cell on the opposite side of the facing
     * direction.
     */
    public static GridPos backingCell(BlockPos pos, BlockState state) {
        return backingCell(pos, state.getValue(FACING), state.getValue(SIDE));
    }

    /**
     * Pure placement math: the guarded track cell of a signal block at {@code pos}.
     */
    public static GridPos backingCell(BlockPos pos, GridDirection facing, RailSignalSide side) {
        return new GridPos(
                pos.getX() + side.opposite().offsetX(facing),
                pos.getY(),
                pos.getZ() + side.opposite().offsetZ(facing)
        );
    }

    /**
     * The world position of the signal block for a signal placed at {@code cell} facing {@code direction} on
     * {@code side}.
     */
    public static BlockPos signalPos(GridPos cell, GridDirection direction, RailSignalSide side) {
        return new BlockPos(
                cell.x() + side.offsetX(direction),
                cell.y(),
                cell.z() + side.offsetZ(direction)
        );
    }

    /**
     * Both possible world positions of a signal block guarding {@code cell} along {@code direction}: the left and
     * the right adjacent cell.
     */
    public static List<BlockPos> candidatePositions(GridPos cell, GridDirection direction) {
        return List.of(
                signalPos(cell, direction, RailSignalSide.LEFT),
                signalPos(cell, direction, RailSignalSide.RIGHT)
        );
    }

    /**
     * Snaps the player's look direction to the guarded cell's track direction: a signal always faces along the
     * track it guards, so the result is one of the two (or, on a crossing, four) directions of the cell's track
     * axes — never an arbitrary direction. Empty when the cell holds no track placements at all.
     */
    public static Optional<GridDirection> trackFacingAt(Level level, GridPos cell, GridDirection look) {
        TrackCellData data = RailTrackCellBlock.source(level).cellDataAt(cell);
        Set<GridDirection> allowed = new HashSet<>();
        for (TrackPlacement placement : data.placements()) {
            allowed.addAll(TrackAxis.from(placement.direction()).directions());
        }
        if (allowed.isEmpty()) {
            return Optional.empty();
        }
        GridDirection best = null;
        int bestDot = Integer.MIN_VALUE;
        for (GridDirection direction : allowed) {
            int dot = direction.dx() * look.dx() + direction.dz() * look.dz();
            if (dot > bestDot) {
                bestDot = dot;
                best = direction;
            }
        }
        return Optional.ofNullable(best);
    }

    @SubscribeRegistration
    @SuppressWarnings("unused")
    private static void register(BlockRegistration registration) {
        registration.register(
                "rail_block_signal",
                RailSignalBlock::new,
                properties -> properties.strength(0.5F).noOcclusion().sound(SoundType.METAL)
        );
    }
}

package com.mistbeyond.rail.item.rail;

import com.mistbeyond.rail.api.rail.graph.GridDirection;
import com.mistbeyond.rail.api.rail.graph.GridPos;
import com.mistbeyond.rail.api.rail.graph.SignalPlacement;
import com.mistbeyond.rail.api.rail.section.SignalId;
import com.mistbeyond.rail.api.rail.section.SignalType;
import com.mistbeyond.rail.block.rail.RailSignalBlock;
import com.mistbeyond.rail.block.rail.RailSignalCellOps;
import com.mistbeyond.rail.block.rail.RailTrackCellBlock;
import com.mistbeyond.rail.block.rail.RailTrackCellBlockEntity;
import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Base item for placing rail signals onto {@link RailTrackCellBlock} cells. The item adds a {@link SignalPlacement}
 * to the track cell's data; the block signal additionally places its world block ({@code RailSignalBlock}, ADR
 * 0012). The signal's facing direction is the player's look direction at placement (sections.md). Sneak + use is
 * type specific: the path signal is removed (it has no world block), the block signal toggles the ghost preview
 * side of its item stack.
 */
public abstract class RailSignalItem extends Item {
    private final SignalType signalType;

    protected RailSignalItem(Properties properties, SignalType signalType) {
        super(properties);
        this.signalType = signalType;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof RailTrackCellBlock)) {
            if (!level.isClientSide() && context.getPlayer() != null) {
                context.getPlayer().sendSystemMessage(Component.literal("Signal must be placed on a rail track cell."));
            }
            return InteractionResult.FAIL;
        }
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            return onSneakUse(level, pos, player, context.getItemInHand());
        }
        return tryPlace(context, level, pos);
    }

    /**
     * Sneak + use behaviour. The path signal has no world block, so it is removed; the block signal overrides this
     * to toggle its ghost preview side instead (removal happens by breaking the signal block, ADR 0012).
     */
    protected InteractionResult onSneakUse(Level level, BlockPos pos, Player player, ItemStack stack) {
        return tryRemove(level, pos, player);
    }

    private InteractionResult tryPlace(UseOnContext context, Level level, BlockPos pos) {
        Player player = context.getPlayer();
        GridPos cell = new GridPos(pos.getX(), pos.getY(), pos.getZ());
        // A signal always faces along the track it guards: snap the look direction onto the cell's track axes.
        Optional<GridDirection> snapped = RailSignalBlock.trackFacingAt(level, cell, directionFromContext(context, player));
        if (snapped.isEmpty()) {
            if (!level.isClientSide() && player != null) {
                player.sendSystemMessage(Component.literal("This cell has no track direction to guard."));
            }
            return InteractionResult.FAIL;
        }
        GridDirection direction = snapped.get();
        Set<SignalPlacement> existing = existingSignals(level, cell);
        // Enforce placement constraint: at most one signal per cell per facing direction (ADR 0008, sections.md).
        for (SignalPlacement signal : existing) {
            if (signal.direction() == direction) {
                if (signal.type() == signalType) {
                    return onSameSignalPlaced(context, level, cell, direction);
                }
                if (!level.isClientSide() && player != null) {
                    player.sendSystemMessage(Component.literal("A signal already faces " + direction + " in this cell."));
                }
                return InteractionResult.FAIL;
            }
        }
        // Opposite-direction sharing is allowed, but we cap at 8 distinct directions for sanity.
        if (existing.size() >= 8) {
            if (!level.isClientSide() && player != null) {
                player.sendSystemMessage(Component.literal("Too many signals in this cell."));
            }
            return InteractionResult.FAIL;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.FAIL;
        }
        String id = "signal-" + cell.x() + "-" + cell.y() + "-" + cell.z() + "-" + direction.name().toLowerCase()
                + "-" + signalType.name().toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 6);
        SignalPlacement placement = new SignalPlacement(new SignalId(id), direction, signalType);
        RailSignalCellOps.addSignal(serverLevel, cell, placement);
        InteractionResult result = onPlaced(context, serverLevel, cell, direction, placement);
        if (result == InteractionResult.FAIL) {
            // Roll the placement data back so the graph never keeps a signal whose world form could not be created.
            RailSignalCellOps.removeSignal(serverLevel, cell, placement.id());
            return InteractionResult.FAIL;
        }
        if (player == null || !player.hasInfiniteMaterials()) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    /**
     * Hook after the signal placement data was written. The path signal has no world representation; the block
     * signal places its world block. Returning FAIL rolls the placement data back.
     */
    protected InteractionResult onPlaced(
            UseOnContext context,
            ServerLevel level,
            GridPos cell,
            GridDirection direction,
            SignalPlacement placement
    ) {
        if (context.getPlayer() != null) {
            context.getPlayer().sendSystemMessage(Component.literal(
                    "Path signal placed facing " + direction + "."));
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    /**
     * Right-clicking a cell that already holds this item's signal type. The path signal rejects (its data can only
     * be removed); the block signal heals: it re-places a missing world block and refreshes its indication.
     */
    protected InteractionResult onSameSignalPlaced(
            UseOnContext context,
            Level level,
            GridPos cell,
            GridDirection direction
    ) {
        if (!level.isClientSide() && context.getPlayer() != null) {
            context.getPlayer().sendSystemMessage(Component.literal(
                    "This cell already has a " + signalType.name().toLowerCase() + " signal facing " + direction + "."));
        }
        return InteractionResult.FAIL;
    }

    private static Set<SignalPlacement> existingSignals(Level level, GridPos cell) {
        BlockEntity blockEntity = level.getBlockEntity(new BlockPos(cell.x(), cell.y(), cell.z()));
        if (blockEntity instanceof RailTrackCellBlockEntity complex && complex.cell().equals(cell)) {
            return complex.signals();
        }
        return Set.of();
    }

    protected InteractionResult tryRemove(Level level, BlockPos pos, Player player) {
        BlockEntity be = level.getBlockEntity(pos);
        GridPos cell = new GridPos(pos.getX(), pos.getY(), pos.getZ());
        if (!(be instanceof RailTrackCellBlockEntity complex) || !complex.cell().equals(cell)) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(Component.literal("No signal data at this cell."));
            }
            return InteractionResult.FAIL;
        }
        if (complex.signals().isEmpty()) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(Component.literal("No signals to remove in this cell."));
            }
            return InteractionResult.FAIL;
        }
        GridDirection look = directionFromPlayer(player);
        // Prefer removing a signal that matches the player's look direction; otherwise remove an arbitrary one.
        SignalPlacement toRemove = null;
        for (SignalPlacement signal : complex.signals()) {
            if (signal.direction() == look && signal.type() == signalType) {
                toRemove = signal;
                break;
            }
        }
        if (toRemove == null) {
            for (SignalPlacement signal : complex.signals()) {
                if (signal.direction() == look) {
                    toRemove = signal;
                    break;
                }
            }
        }
        if (toRemove == null) {
            // Fallback: remove any signal of our type, else any signal.
            for (SignalPlacement signal : complex.signals()) {
                if (signal.type() == signalType) {
                    toRemove = signal;
                    break;
                }
            }
            if (toRemove == null) {
                toRemove = complex.signals().iterator().next();
            }
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.FAIL;
        }
        RailSignalCellOps.removeSignal(serverLevel, cell, toRemove.id());
        player.sendSystemMessage(Component.literal("Removed " + toRemove.type() + " signal facing " + toRemove.direction() + "."));
        return InteractionResult.SUCCESS_SERVER;
    }

    private static GridDirection directionFromContext(UseOnContext context, @Nullable Player player) {
        if (player != null) {
            return directionFromPlayer(player);
        }
        // Fallback to horizontal direction of the click face; EAST covers both the EAST case and UP/DOWN.
        return switch (context.getHorizontalDirection()) {
            case NORTH -> GridDirection.NORTH;
            case SOUTH -> GridDirection.SOUTH;
            case WEST -> GridDirection.WEST;
            default -> GridDirection.EAST;
        };
    }

    /**
     * The signal's facing is the player's look direction (sections.md, ADR 0010).
     */
    protected static GridDirection directionFromPlayer(Player player) {
        return RailTrackCellBlock.directionFromYaw(player.getYRot());
    }
}

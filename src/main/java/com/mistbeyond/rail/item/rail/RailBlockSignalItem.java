package com.mistbeyond.rail.item.rail;

import com.mistbeyond.rail.Ids;
import com.mistbeyond.rail.Init;
import com.mistbeyond.rail.api.rail.graph.GridDirection;
import com.mistbeyond.rail.api.rail.graph.GridPos;
import com.mistbeyond.rail.api.rail.graph.SignalPlacement;
import com.mistbeyond.rail.api.rail.section.SignalAspect;
import com.mistbeyond.rail.api.rail.section.SignalId;
import com.mistbeyond.rail.api.rail.section.SignalState;
import com.mistbeyond.rail.api.rail.section.SignalType;
import com.mistbeyond.rail.block.rail.RailSignalBlock;
import com.mistbeyond.rail.block.rail.RailSignalSide;
import com.mistbeyond.rail.block.rail.RailTrackCellBlockEntity;
import com.mistbeyond.rail.core.rail.RailNetworkManager;
import com.mistbeyond.registry.RegisterItem;
import com.mistbeyond.registry.SubscribeRegistration;
import com.mistbeyond.registry.impl.ItemRegistration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

/**
 * The block signal item. Right-click on a track cell places the signal data plus the {@link RailSignalBlock} world
 * block beside the cell, on the stack's ghost-preview side (ADR 0012). Shift + use toggles the ghost side of the
 * stack; removal is done by breaking the signal block. Right-clicking a cell that already holds this signal heals
 * a missing world block and refreshes its indication.
 */
@RegisterItem
public class RailBlockSignalItem extends RailSignalItem {
    public RailBlockSignalItem(Properties properties) {
        super(properties, SignalType.BLOCK);
    }

    /**
     * The stack's ghost-preview side; LEFT until toggled.
     */
    public static RailSignalSide sideOf(ItemStack stack) {
        DataComponentType<RailSignalSide> type = Init.RAIL_SIGNAL_SIDE.get();
        RailSignalSide side = stack.get(type);
        return side == null ? RailSignalSide.LEFT : side;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            toggleGhostSide(player.getItemInHand(hand), level, player);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult onSneakUse(Level level, BlockPos pos, Player player, ItemStack stack) {
        toggleGhostSide(stack, level, player);
        return InteractionResult.SUCCESS;
    }

    private static void toggleGhostSide(ItemStack stack, Level level, Player player) {
        RailSignalSide next = sideOf(stack).opposite();
        stack.set(Init.RAIL_SIGNAL_SIDE.get(), next);
        if (level.isClientSide()) {
            player.sendSystemMessage(Component.literal("Signal ghost side: " + next.getSerializedName()));
        }
    }

    @Override
    protected InteractionResult onPlaced(
            UseOnContext context,
            ServerLevel level,
            GridPos cell,
            GridDirection direction,
            SignalPlacement placement
    ) {
        Optional<RailSignalSide> placed = placeSignalBlock(level, cell, direction, placement, sideOf(context.getItemInHand()));
        if (placed.isEmpty()) {
            if (context.getPlayer() != null) {
                context.getPlayer().sendSystemMessage(Component.literal(
                        "No free space beside the track for the signal block; placement cancelled."));
            }
            return InteractionResult.FAIL;
        }
        if (context.getPlayer() != null) {
            context.getPlayer().sendSystemMessage(Component.literal(
                    "Block signal placed facing " + direction + " on the " + placed.get().getSerializedName() + " side."));
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected InteractionResult onSameSignalPlaced(
            UseOnContext context,
            Level level,
            GridPos cell,
            GridDirection direction
    ) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.FAIL;
        }
        // Heal: if the world block is missing (for example legacy data or a rolled-back placement), re-place it.
        Optional<SignalPlacement> placement = findBlockPlacement(serverLevel, cell, direction);
        if (placement.isEmpty()) {
            return InteractionResult.FAIL;
        }
        if (findSignalBlock(serverLevel, cell, direction).isPresent()) {
            if (context.getPlayer() != null) {
                context.getPlayer().sendSystemMessage(Component.literal(
                        "Block signal already stands beside this cell facing " + direction + "."));
            }
            return InteractionResult.FAIL;
        }
        Optional<RailSignalSide> placed = placeSignalBlock(
                serverLevel, cell, direction, placement.get(), sideOf(context.getItemInHand()));
        if (placed.isEmpty()) {
            if (context.getPlayer() != null) {
                context.getPlayer().sendSystemMessage(Component.literal(
                        "No free space beside the track for the signal block."));
            }
            return InteractionResult.FAIL;
        }
        if (context.getPlayer() != null) {
            context.getPlayer().sendSystemMessage(Component.literal(
                    "Re-placed the missing block signal facing " + direction + "."));
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    /**
     * Places the world block on {@code preferred}, falling back to the opposite side when the preferred cell is
     * blocked. Returns the side used, or empty when neither side is free.
     */
    private static Optional<RailSignalSide> placeSignalBlock(
            ServerLevel level,
            GridPos cell,
            GridDirection direction,
            SignalPlacement placement,
            RailSignalSide preferred
    ) {
        RailSignalSide[] sides = {preferred, preferred.opposite()};
        for (RailSignalSide side : sides) {
            BlockPos pos = RailSignalBlock.signalPos(cell, direction, side);
            BlockState target = level.getBlockState(pos);
            if (!(target.isAir() || target.canBeReplaced())) {
                continue;
            }
            BlockState state = BuiltInRegistries.BLOCK
                    .getValue(Identifier.fromNamespaceAndPath(Ids.MOD_ID, "rail_block_signal"))
                    .defaultBlockState()
                    .setValue(RailSignalBlock.FACING, direction)
                    .setValue(RailSignalBlock.SIDE, side)
                    .setValue(RailSignalBlock.ASPECT, resolvedAspect(level, placement.id()));
            level.setBlock(pos, state, Block.UPDATE_ALL);
            return Optional.of(side);
        }
        return Optional.empty();
    }

    /**
     * The server-resolved indication at placement time; RED until the dispatch state is known or when the signal
     * is misconfigured (ERROR behaves as RED, sections.md).
     */
    private static SignalAspect resolvedAspect(ServerLevel level, SignalId id) {
        for (SignalState state : RailNetworkManager.of(level).snapshot().signalStates()) {
            if (state.id().equals(id)) {
                return state.error() ? SignalAspect.RED : state.aspect();
            }
        }
        return SignalAspect.RED;
    }

    private static Optional<SignalPlacement> findBlockPlacement(ServerLevel level, GridPos cell, GridDirection direction) {
        BlockEntity blockEntity = level.getBlockEntity(new BlockPos(cell.x(), cell.y(), cell.z()));
        if (!(blockEntity instanceof RailTrackCellBlockEntity complex) || !complex.cell().equals(cell)) {
            return Optional.empty();
        }
        return complex.signals().stream()
                .filter(placement -> placement.type() == SignalType.BLOCK && placement.direction() == direction)
                .findFirst();
    }

    private static Optional<BlockPos> findSignalBlock(ServerLevel level, GridPos cell, GridDirection direction) {
        for (BlockPos candidate : RailSignalBlock.candidatePositions(cell, direction)) {
            BlockState state = level.getBlockState(candidate);
            if (state.getBlock() instanceof RailSignalBlock && state.getValue(RailSignalBlock.FACING) == direction) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    @SubscribeRegistration
    @SuppressWarnings("unused")
    private static void register(ItemRegistration registration) {
        registration.register(
                "rail_block_signal",
                RailBlockSignalItem::new,
                props -> props.stacksTo(64)
        );
    }
}

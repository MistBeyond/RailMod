package com.mistbeyond.rail.client.rail;

import com.mistbeyond.rail.Ids;
import com.mistbeyond.rail.api.rail.graph.GridDirection;
import com.mistbeyond.rail.api.rail.graph.GridPos;
import com.mistbeyond.rail.block.rail.RailSignalBlock;
import com.mistbeyond.rail.block.rail.RailSignalSide;
import com.mistbeyond.rail.block.rail.RailTrackCellBlock;
import com.mistbeyond.rail.item.rail.RailBlockSignalItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;

import java.util.List;
import java.util.Optional;

/**
 * Attaches the block-signal ghost preview while aiming at a track cell with the block signal item (ADR 0012): the
 * facing snaps onto the cell's track direction exactly like the placement does, and the ghost renders the real
 * signal model on both candidate side cells with the active side highlighted.
 */
@EventBusSubscriber(modid = Ids.MOD_ID, value = Dist.CLIENT)
public class RailSignalGhostEvents {
    private RailSignalGhostEvents() {
    }

    @SubscribeEvent
    public static void onExtractOutline(ExtractBlockOutlineRenderStateEvent event) {
        if (!(event.getBlockState().getBlock() instanceof RailTrackCellBlock)) {
            return;
        }
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof RailBlockSignalItem)) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getBlockPos();
        GridPos cell = new GridPos(pos.getX(), pos.getY(), pos.getZ());
        GridDirection look = RailTrackCellBlock.directionFromYaw(player.getYRot());
        Optional<GridDirection> facing = RailSignalBlock.trackFacingAt(level, cell, look);
        if (facing.isEmpty()) {
            return;
        }
        RailSignalSide activeSide = RailBlockSignalItem.sideOf(stack);
        BlockState base = BuiltInRegistries.BLOCK
                .getValue(Identifier.fromNamespaceAndPath(Ids.MOD_ID, "rail_block_signal"))
                .defaultBlockState();
        BlockState left = base
                .setValue(RailSignalBlock.FACING, facing.get())
                .setValue(RailSignalBlock.SIDE, RailSignalSide.LEFT);
        BlockState right = base
                .setValue(RailSignalBlock.FACING, facing.get())
                .setValue(RailSignalBlock.SIDE, RailSignalSide.RIGHT);
        List<BlockPos> positions = RailSignalBlock.candidatePositions(cell, facing.get());
        boolean[] freeBySide = new boolean[RailSignalSide.values().length];
        for (RailSignalSide candidate : RailSignalSide.values()) {
            freeBySide[candidate.ordinal()] = level.getBlockState(positions.get(candidate.ordinal())).isAir();
        }
        var model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(left);
        event.addCustomRenderer(new RailSignalGhostRenderer(
                level,
                model,
                List.of(left, right),
                positions,
                activeSide,
                freeBySide,
                facing.get(),
                new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5)
        ));
    }
}

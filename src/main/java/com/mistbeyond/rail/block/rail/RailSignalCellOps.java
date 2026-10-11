package com.mistbeyond.rail.block.rail;

import com.mistbeyond.rail.api.rail.graph.GridPos;
import com.mistbeyond.rail.api.rail.graph.SignalPlacement;
import com.mistbeyond.rail.api.rail.graph.TrackPlacement;
import com.mistbeyond.rail.api.rail.graph.TrackType;
import com.mistbeyond.rail.api.rail.section.SignalId;
import com.mistbeyond.rail.core.rail.RailNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashSet;
import java.util.Set;

/**
 * Server-side data operations on one track cell's signal placement set, shared by the signal items and the signal
 * block. A simple cell (one axis, no signal) has no block entity; adding the first signal upgrades it to a complex
 * cell, and removing the last signal downgrades it again (runtime contract, acceptance scenarios 16-19).
 *
 * <p>Every change rebuilds the affected graph component immediately ({@code markCellChangedAndValidate}) and pushes
 * the block entity data to all players tracking the chunk, so the client-side graph (F3 overlay, ghost previews)
 * sees the new signal placements at once.
 */
public final class RailSignalCellOps {
    private RailSignalCellOps() {
    }

    /**
     * Adds {@code placement} to the cell's signal set, creating the complex-cell block entity first when the cell
     * is still simple. Existing placements are preserved.
     */
    public static void addSignal(ServerLevel level, GridPos cell, SignalPlacement placement) {
        BlockPos pos = new BlockPos(cell.x(), cell.y(), cell.z());
        BlockEntity existing = level.getBlockEntity(pos);
        if (existing instanceof RailTrackCellBlockEntity complex && complex.cell().equals(cell)) {
            Set<SignalPlacement> next = new HashSet<>(complex.signals());
            next.add(placement);
            complex.setData(complex.placements(), Set.copyOf(next));
            finishChange(level, cell, pos);
            return;
        }
        // Simple cell without a block entity: upgrade to complex, preserving the BlockState placements.
        if (!(level.getBlockState(pos).getBlock() instanceof RailTrackCellBlock)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        Set<TrackPlacement> placements = RailTrackCellBlock.placementsFor(cell, state.getValue(RailTrackCellBlock.DIRECTION));
        RailTrackCellBlockEntity created = new RailTrackCellBlockEntity(pos, state);
        created.setData(placements, Set.of(placement));
        // Install the new block entity into the chunk (LevelChunk#setBlockEntity is the manual-creation path).
        LevelChunk chunk = (LevelChunk) level.getChunk(pos);
        chunk.setBlockEntity(created);
        finishChange(level, cell, pos);
    }

    /**
     * Removes the signal with {@code id} from the cell's signal set. When the set becomes empty and the remaining
     * placements are a simple axis pair, the cell is downgraded back to a simple cell (block entity removed).
     */
    public static void removeSignal(ServerLevel level, GridPos cell, SignalId id) {
        BlockPos pos = new BlockPos(cell.x(), cell.y(), cell.z());
        if (!(level.getBlockEntity(pos) instanceof RailTrackCellBlockEntity complex) || !complex.cell().equals(cell)) {
            return;
        }
        Set<SignalPlacement> next = new HashSet<>(complex.signals());
        next.removeIf(placement -> placement.id().equals(id));
        BlockState state = level.getBlockState(pos);
        if (next.isEmpty() && isSimplePlacements(complex.placements())) {
            // Downgrade complex to simple: remove the block entity, the BlockState already stores the axis.
            level.removeBlockEntity(pos);
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
        } else {
            complex.setData(complex.placements(), Set.copyOf(next));
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
        }
        finishChange(level, cell, pos);
    }

    private static void finishChange(ServerLevel level, GridPos cell, BlockPos pos) {
        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), Block.UPDATE_ALL);
        sendBlockEntityData(level, pos);
        RailNetworkManager.of(level).markCellChangedAndValidate(cell);
    }

    /**
     * The vanilla block update path does not carry block entity data in this version, so the update packet is sent
     * to every player tracking the chunk explicitly.
     */
    private static void sendBlockEntityData(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof RailTrackCellBlockEntity complex)) {
            return;
        }
        Packet<ClientGamePacketListener> packet = complex.getUpdatePacket();
        if (packet == null) {
            return;
        }
        for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4), false)) {
            player.connection.send(packet);
        }
    }

    /**
     * Whether the placements form a simple cell: exactly the two opposite directions of one straight or
     * diagonal 45 axis. Mirrors the runtime contract's simple-cell definition.
     */
    public static boolean isSimplePlacements(Set<TrackPlacement> placements) {
        if (placements.size() != 2) {
            return false;
        }
        var iterator = placements.iterator();
        TrackPlacement first = iterator.next();
        TrackPlacement second = iterator.next();
        if (first.trackType() != second.trackType()) {
            return false;
        }
        if (first.trackType() != TrackType.STRAIGHT && first.trackType() != TrackType.DIAGONAL_45) {
            return false;
        }
        return first.direction().opposite() == second.direction() && first.originCell().equals(second.originCell());
    }
}

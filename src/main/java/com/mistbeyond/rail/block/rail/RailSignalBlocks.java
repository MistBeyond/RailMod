package com.mistbeyond.rail.block.rail;

import com.mistbeyond.rail.api.rail.RailNetworkSnapshot;
import com.mistbeyond.rail.api.rail.graph.RailGraphView;
import com.mistbeyond.rail.api.rail.section.SignalAspect;
import com.mistbeyond.rail.api.rail.section.SignalId;
import com.mistbeyond.rail.api.rail.section.SignalState;
import com.mistbeyond.rail.api.rail.section.SignalType;
import com.mistbeyond.rail.api.rail.section.SignalView;
import com.mistbeyond.rail.core.rail.RailNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

/**
 * Keeps the {@link RailSignalBlock} ASPECT property in sync with the server-resolved signal states: whenever a
 * signal's RED/GREEN indication changes, the world block of that signal is updated through the vanilla block
 * update path, so the chunk remeshes and the lit lamp follows the dispatch state (ADR 0012). Runs at most once
 * per second per level and only touches blocks whose indication actually changed. A misconfigured signal behaves
 * as RED and therefore renders as RED; the dedicated ERROR overlay is deferred (ADR 0010 rev.).
 */
public final class RailSignalBlocks {
    private static final Map<ResourceKey<Level>, Map<SignalId, SignalAspect>> LAST_ASPECTS = new HashMap<>();

    private RailSignalBlocks() {
    }

    public static void tick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            RailNetworkSnapshot snapshot = RailNetworkManager.of(level).snapshot();
            Map<SignalId, SignalAspect> current = new HashMap<>();
            for (SignalState state : snapshot.signalStates()) {
                current.put(state.id(), state.error() ? SignalAspect.RED : state.aspect());
            }
            Map<SignalId, SignalAspect> previous = LAST_ASPECTS.put(level.dimension(), Map.copyOf(current));
            for (Map.Entry<SignalId, SignalAspect> entry : current.entrySet()) {
                if (previous == null || !entry.getValue().equals(previous.get(entry.getKey()))) {
                    applyAspect(level, snapshot.graph(), entry.getKey(), entry.getValue());
                }
            }
        }
    }

    private static void applyAspect(ServerLevel level, RailGraphView graph, SignalId id, SignalAspect aspect) {
        for (SignalView signal : graph.signals()) {
            if (!signal.id().equals(id) || signal.type() != SignalType.BLOCK) {
                continue;
            }
            for (BlockPos candidate : RailSignalBlock.candidatePositions(signal.cell(), signal.direction())) {
                BlockState state = level.getBlockState(candidate);
                if (state.getBlock() instanceof RailSignalBlock
                        && state.getValue(RailSignalBlock.FACING) == signal.direction()
                        && state.getValue(RailSignalBlock.ASPECT) != aspect) {
                    level.setBlock(
                            candidate,
                            state.setValue(RailSignalBlock.ASPECT, aspect),
                            Block.UPDATE_ALL
                    );
                }
            }
        }
    }
}

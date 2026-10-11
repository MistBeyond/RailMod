package com.mistbeyond.rail.core.rail;

import com.mistbeyond.rail.api.rail.RailTrainSnapshot;
import com.mistbeyond.rail.api.rail.dispatch.RailControlMode;
import com.mistbeyond.rail.api.rail.dispatch.RailTrainId;
import com.mistbeyond.rail.api.rail.dispatch.RailTrainSchedule;
import com.mistbeyond.rail.api.rail.dispatch.RailTrainScheduleType;
import com.mistbeyond.rail.api.rail.dispatch.RailTrainStop;
import com.mistbeyond.rail.api.rail.graph.GridPos;
import com.mistbeyond.rail.api.rail.station.RailStationId;
import com.mistbeyond.rail.internal.rail.RailNetworkCodecs;
import com.mistbeyond.rail.internal.rail.RailNetworkState;
import com.mojang.serialization.DataResult;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RailNetworkCodecsTest {
    @Test
    void stateRoundTripsThroughNbt() {
        RailTrainSnapshot train = new RailTrainSnapshot(
                new RailTrainId("train-1"),
                RailControlMode.AUTOMATIC,
                new GridPos(1, 2, 3),
                false,
                Optional.of(new RailTrainSchedule(
                        RailTrainScheduleType.LOOP,
                        List.of(
                                new RailTrainStop(new RailStationId("a"), 20),
                                new RailTrainStop(new RailStationId("b"), 0)
                        )
                ))
        );
        RailNetworkState state = new RailNetworkState(List.of(train));

        DataResult<net.minecraft.nbt.Tag> encoded = RailNetworkCodecs.STATE.encodeStart(NbtOps.INSTANCE, state);
        RailNetworkState decoded = RailNetworkCodecs.STATE.parse(NbtOps.INSTANCE, encoded.getOrThrow()).getOrThrow();

        assertEquals(state, decoded);
    }
}

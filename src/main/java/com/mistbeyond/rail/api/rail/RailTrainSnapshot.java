package com.mistbeyond.rail.api.rail;

import com.mistbeyond.rail.api.rail.dispatch.RailControlMode;
import com.mistbeyond.rail.api.rail.dispatch.RailTrainId;
import com.mistbeyond.rail.api.rail.dispatch.RailTrainSchedule;
import com.mistbeyond.rail.api.rail.graph.GridPos;

import java.util.Optional;

public record RailTrainSnapshot(
        RailTrainId id,
        RailControlMode controlMode,
        GridPos position,
        boolean derailed,
        Optional<RailTrainSchedule> schedule
) {
}

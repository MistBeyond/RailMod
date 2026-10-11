package com.mistbeyond.rail.api.rail.dispatch;

import com.mistbeyond.rail.api.rail.station.RailStationId;

public record RailTrainStop(
        RailStationId station,
        int dwellTicks
) {
    public RailTrainStop {
        dwellTicks = Math.max(0, dwellTicks);
    }
}

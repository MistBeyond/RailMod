package com.mistbeyond.rail.api.rail.station;

import com.mistbeyond.rail.api.rail.graph.GridPos;

import java.util.Optional;

@FunctionalInterface
public interface RailStationLocator {
    Optional<GridPos> locate(RailStationId station);
}

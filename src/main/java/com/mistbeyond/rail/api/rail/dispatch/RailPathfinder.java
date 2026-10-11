package com.mistbeyond.rail.api.rail.dispatch;

import com.mistbeyond.rail.api.rail.graph.RailGraphView;

import java.util.Optional;

public interface RailPathfinder {
    Optional<RailRoute> findRoute(RailGraphView graph, RouteRequest request);
}

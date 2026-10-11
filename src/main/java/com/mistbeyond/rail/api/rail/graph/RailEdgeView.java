package com.mistbeyond.rail.api.rail.graph;

public interface RailEdgeView {
    RailEdgeId id();

    RailNodeId start();

    RailNodeId end();

    TrackPlacement placement();

    double lengthMeters();
}

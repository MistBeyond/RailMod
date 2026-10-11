package com.mistbeyond.rail.api.rail.section;

import com.mistbeyond.rail.api.rail.graph.RailEdgeId;
import com.mistbeyond.rail.api.rail.graph.TraversalDirection;

public interface SignalBoundaryView {
    SignalId id();

    RailEdgeId edgeId();

    double metersFromStart();

    TraversalDirection direction();
}

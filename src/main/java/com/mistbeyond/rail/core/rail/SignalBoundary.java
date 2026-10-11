package com.mistbeyond.rail.core.rail;

import com.mistbeyond.rail.api.rail.graph.RailEdgeId;
import com.mistbeyond.rail.api.rail.graph.TraversalDirection;
import com.mistbeyond.rail.api.rail.section.SignalBoundaryView;
import com.mistbeyond.rail.api.rail.section.SignalId;

public record SignalBoundary(
        SignalId id,
        RailEdgeId edgeId,
        double metersFromStart,
        TraversalDirection direction
) implements SignalBoundaryView {
    public SignalBoundary {
        metersFromStart = Math.max(0.0, metersFromStart);
    }
}

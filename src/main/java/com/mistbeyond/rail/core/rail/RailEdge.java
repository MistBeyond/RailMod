package com.mistbeyond.rail.core.rail;

import com.mistbeyond.rail.api.rail.graph.RailEdgeId;
import com.mistbeyond.rail.api.rail.graph.RailEdgeView;
import com.mistbeyond.rail.api.rail.graph.RailNodeId;
import com.mistbeyond.rail.api.rail.graph.TrackPlacement;

public record RailEdge(
        RailEdgeId id,
        RailNodeId start,
        RailNodeId end,
        TrackPlacement placement,
        double lengthMeters
) implements RailEdgeView {
}

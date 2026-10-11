package com.mistbeyond.rail.core.rail;

import com.mistbeyond.rail.api.rail.graph.GridPos;
import com.mistbeyond.rail.api.rail.graph.RailNodeId;
import com.mistbeyond.rail.api.rail.graph.RailNodeView;

public record RailNode(RailNodeId id, GridPos pos) implements RailNodeView {
}

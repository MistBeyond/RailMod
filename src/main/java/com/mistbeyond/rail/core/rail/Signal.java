package com.mistbeyond.rail.core.rail;

import com.mistbeyond.rail.api.rail.graph.GridDirection;
import com.mistbeyond.rail.api.rail.graph.GridPos;
import com.mistbeyond.rail.api.rail.section.SignalId;
import com.mistbeyond.rail.api.rail.section.SignalType;
import com.mistbeyond.rail.api.rail.section.SignalView;

public record Signal(
        SignalId id,
        GridPos cell,
        GridDirection direction,
        SignalType type
) implements SignalView {
}

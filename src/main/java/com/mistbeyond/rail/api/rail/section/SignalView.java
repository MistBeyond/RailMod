package com.mistbeyond.rail.api.rail.section;

import com.mistbeyond.rail.api.rail.graph.GridDirection;
import com.mistbeyond.rail.api.rail.graph.GridPos;

public interface SignalView {
    SignalId id();

    GridPos cell();

    GridDirection direction();

    SignalType type();
}

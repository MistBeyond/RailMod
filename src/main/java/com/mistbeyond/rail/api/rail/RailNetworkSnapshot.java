package com.mistbeyond.rail.api.rail;

import com.mistbeyond.rail.api.rail.dispatch.DispatchSnapshot;
import com.mistbeyond.rail.api.rail.graph.RailGraphView;
import com.mistbeyond.rail.api.rail.section.SignalState;

import java.util.Set;

public record RailNetworkSnapshot(
        RailGraphView graph,
        DispatchSnapshot dispatch,
        Set<RailTrainSnapshot> trains,
        Set<SignalState> signalStates
) {
    public RailNetworkSnapshot {
        trains = Set.copyOf(trains);
        signalStates = Set.copyOf(signalStates);
    }
}

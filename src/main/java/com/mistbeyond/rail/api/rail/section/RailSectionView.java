package com.mistbeyond.rail.api.rail.section;

import com.mistbeyond.rail.api.rail.graph.TrackSegmentId;

import java.util.Set;

public interface RailSectionView {
    RailSectionId id();

    Set<TrackSegmentId> segments();

    Set<? extends SignalBoundaryView> boundaries();
}

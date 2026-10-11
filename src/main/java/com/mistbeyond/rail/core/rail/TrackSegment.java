package com.mistbeyond.rail.core.rail;

import com.mistbeyond.rail.api.rail.graph.RailEdgeId;
import com.mistbeyond.rail.api.rail.graph.TrackSegmentId;
import com.mistbeyond.rail.api.rail.graph.TrackSegmentView;

public record TrackSegment(TrackSegmentId id, RailEdgeId edgeId, double fromMeters, double toMeters)
        implements TrackSegmentView {
}

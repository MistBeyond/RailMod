package com.mistbeyond.rail.api.rail.graph;

public interface TrackSegmentView {
    TrackSegmentId id();

    RailEdgeId edgeId();

    double fromMeters();

    double toMeters();
}

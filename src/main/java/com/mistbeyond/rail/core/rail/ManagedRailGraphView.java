package com.mistbeyond.rail.core.rail;

import com.mistbeyond.rail.api.rail.graph.GridPos;
import com.mistbeyond.rail.api.rail.graph.RailEdgeId;
import com.mistbeyond.rail.api.rail.graph.RailEdgeView;
import com.mistbeyond.rail.api.rail.graph.RailGraphView;
import com.mistbeyond.rail.api.rail.graph.RailNodeView;
import com.mistbeyond.rail.api.rail.graph.TrackPosition;
import com.mistbeyond.rail.api.rail.graph.TrackSegmentId;
import com.mistbeyond.rail.api.rail.graph.TrackSegmentView;
import com.mistbeyond.rail.api.rail.section.RailSectionView;
import com.mistbeyond.rail.api.rail.section.SignalView;

import java.util.Optional;
import java.util.Set;

class ManagedRailGraphView implements RailGraphView {
    private RailGraphView delegate = RailGraph.empty();

    void update(RailGraphView delegate) {
        this.delegate = delegate;
    }

    @Override
    public Set<RailNodeView> nodes() {
        return delegate.nodes();
    }

    @Override
    public Set<RailEdgeView> edges() {
        return delegate.edges();
    }

    @Override
    public Set<SignalView> signals() {
        return delegate.signals();
    }

    @Override
    public Set<RailSectionView> sections() {
        return delegate.sections();
    }

    @Override
    public Optional<RailNodeView> nodeAt(GridPos pos) {
        return delegate.nodeAt(pos);
    }

    @Override
    public Optional<RailEdgeView> edgeById(RailEdgeId id) {
        return delegate.edgeById(id);
    }

    @Override
    public Optional<RailSectionView> sectionAt(TrackPosition position) {
        return delegate.sectionAt(position);
    }

    @Override
    public Optional<TrackSegmentView> segmentById(TrackSegmentId id) {
        return delegate.segmentById(id);
    }
}

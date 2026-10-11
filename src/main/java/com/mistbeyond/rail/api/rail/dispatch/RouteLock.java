package com.mistbeyond.rail.api.rail.dispatch;

import com.mistbeyond.rail.api.rail.section.RailSectionId;

import java.util.List;

public record RouteLock(
        RailTrainId trainId,
        RailRoute route,
        List<RailSectionId> sections,
        boolean active
) {
    public RouteLock {
        sections = List.copyOf(sections);
    }
}

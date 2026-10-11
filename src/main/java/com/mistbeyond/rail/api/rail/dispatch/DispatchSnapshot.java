package com.mistbeyond.rail.api.rail.dispatch;

import com.mistbeyond.rail.api.rail.section.RailSectionId;

import java.util.Map;
import java.util.Set;

public record DispatchSnapshot(
        Set<Reservation> reservations,
        Set<RouteLock> locks,
        Map<RailSectionId, RailTrainId> manualClaims
) {
    public DispatchSnapshot {
        reservations = Set.copyOf(reservations);
        locks = Set.copyOf(locks);
        manualClaims = Map.copyOf(manualClaims);
    }
}

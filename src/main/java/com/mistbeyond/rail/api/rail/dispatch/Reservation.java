package com.mistbeyond.rail.api.rail.dispatch;

import com.mistbeyond.rail.api.rail.section.RailSectionId;

public record Reservation(
        RailSectionId sectionId,
        RailTrainId trainId,
        ReservationState state
) {
}

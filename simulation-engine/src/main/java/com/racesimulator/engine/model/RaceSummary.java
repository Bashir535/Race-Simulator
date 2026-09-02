package com.racesimulator.engine.model;

import java.util.List;

public record RaceSummary(
        String winner,
        double timeMarginSeconds,
        double distanceGapAtWinnerFinishMeters,
        String largestLeadVehicle,
        double largestLeadMeters,
        List<LeadChange> leadChanges) {

    public RaceSummary {
        leadChanges = List.copyOf(leadChanges);
    }
}

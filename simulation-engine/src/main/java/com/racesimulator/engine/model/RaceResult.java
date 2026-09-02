package com.racesimulator.engine.model;

import java.util.List;

public record RaceResult(
        RaceConfig config,
        VehicleRaceResult vehicleA,
        VehicleRaceResult vehicleB,
        String winner,
        double winningMarginSeconds,
        RaceSummary summary,
        List<RaceFrame> timeline) {

    public RaceResult {
        timeline = List.copyOf(timeline);
    }
}

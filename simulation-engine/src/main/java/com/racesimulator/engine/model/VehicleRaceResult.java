package com.racesimulator.engine.model;

import java.util.List;

public record VehicleRaceResult(
        VehicleSpec vehicle,
        double finishTimeSeconds,
        double finishSpeedMetersPerSecond,
        List<Milestone> milestones,
        List<ShiftEvent> shiftEvents) {

    public VehicleRaceResult {
        milestones = List.copyOf(milestones);
        shiftEvents = List.copyOf(shiftEvents);
    }
}

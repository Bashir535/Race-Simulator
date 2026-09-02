package com.racesimulator.engine.model;

public record DistanceRaceGoal(double distanceMeters) implements RaceGoal {
    public DistanceRaceGoal {
        if (!Double.isFinite(distanceMeters) || distanceMeters <= 0.0) {
            throw new IllegalArgumentException("Race distance must be finite and positive");
        }
    }
}

package com.racesimulator.engine.model;

public record SpeedRaceGoal(double targetSpeedMetersPerSecond) implements RaceGoal {
    public SpeedRaceGoal {
        if (!Double.isFinite(targetSpeedMetersPerSecond) || targetSpeedMetersPerSecond <= 0.0) {
            throw new IllegalArgumentException("Target speed must be finite and positive");
        }
    }
}

package com.racesimulator.engine.model;

import java.util.Objects;

public record RaceConfig(
        RaceGoal goal,
        double startingSpeedMetersPerSecond,
        RoadSurface roadSurface,
        double timeStepSeconds,
        double maximumDurationSeconds,
        EnvironmentConditions environment) {

    public static final double EIGHTH_MILE_METERS = 201.168;
    public static final double QUARTER_MILE_METERS = 402.336;

    public RaceConfig {
        Objects.requireNonNull(goal, "Race goal is required");
        if (!Double.isFinite(startingSpeedMetersPerSecond) || startingSpeedMetersPerSecond < 0.0) {
            throw new IllegalArgumentException("Starting speed must be finite and non-negative");
        }
        if (goal instanceof SpeedRaceGoal speedGoal
                && speedGoal.targetSpeedMetersPerSecond() <= startingSpeedMetersPerSecond) {
            throw new IllegalArgumentException("Target speed must be greater than starting speed");
        }
        Objects.requireNonNull(roadSurface, "Road surface is required");
        Objects.requireNonNull(environment, "Environment conditions are required");
        requirePositive(timeStepSeconds, "Time step");
        requirePositive(maximumDurationSeconds, "Maximum duration");
        if (timeStepSeconds > 0.1) {
            throw new IllegalArgumentException("Time step must not exceed 0.1 seconds");
        }
    }

    public RaceConfig(
            RaceGoal goal,
            double startingSpeedMetersPerSecond,
            RoadSurface roadSurface,
            double timeStepSeconds,
            double maximumDurationSeconds) {
        this(goal, startingSpeedMetersPerSecond, roadSurface, timeStepSeconds,
                maximumDurationSeconds, EnvironmentConditions.STANDARD);
    }

    public static RaceConfig quarterMile(RoadSurface roadSurface) {
        return distanceRace(QUARTER_MILE_METERS, 0.0, roadSurface);
    }

    public static RaceConfig distanceRace(
            double distanceMeters,
            double startingSpeedMetersPerSecond,
            RoadSurface roadSurface) {
        return new RaceConfig(
                new DistanceRaceGoal(distanceMeters),
                startingSpeedMetersPerSecond,
                roadSurface,
                0.01,
                60.0,
                EnvironmentConditions.STANDARD);
    }

    public static RaceConfig rollRace(
            double startingSpeedMetersPerSecond,
            double targetSpeedMetersPerSecond,
            RoadSurface roadSurface) {
        return new RaceConfig(
                new SpeedRaceGoal(targetSpeedMetersPerSecond),
                startingSpeedMetersPerSecond,
                roadSurface,
                0.01,
                60.0,
                EnvironmentConditions.STANDARD);
    }

    public RaceConfig withEnvironment(EnvironmentConditions conditions) {
        return new RaceConfig(goal, startingSpeedMetersPerSecond, roadSurface,
                timeStepSeconds, maximumDurationSeconds, conditions);
    }

    public double distanceMeters() {
        if (goal instanceof DistanceRaceGoal distanceGoal) {
            return distanceGoal.distanceMeters();
        }
        throw new IllegalStateException("This race uses a speed goal, not a distance goal");
    }

    private static void requirePositive(double value, String label) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(label + " must be finite and positive");
        }
    }
}

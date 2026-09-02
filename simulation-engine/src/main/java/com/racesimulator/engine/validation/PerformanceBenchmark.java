package com.racesimulator.engine.validation;

public record PerformanceBenchmark(
        String vehicleName,
        double zeroToSixtySeconds,
        double quarterMileSeconds,
        double quarterMileTrapSpeedMetersPerSecond,
        double rolloutSeconds,
        String source,
        double ratedHorsepower,
        double ratedPowerRpm) {

    public PerformanceBenchmark {
        if (vehicleName == null || vehicleName.isBlank()) {
            throw new IllegalArgumentException("Vehicle name is required");
        }
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("Benchmark source is required");
        }
        requirePositive(zeroToSixtySeconds, "0-60 time");
        requirePositive(quarterMileSeconds, "Quarter-mile time");
        requirePositive(quarterMileTrapSpeedMetersPerSecond, "Quarter-mile trap speed");
        if (!Double.isFinite(rolloutSeconds) || rolloutSeconds < 0.0) {
            throw new IllegalArgumentException("Rollout adjustment must be finite and non-negative");
        }
        requirePositive(ratedHorsepower, "Rated horsepower");
        requirePositive(ratedPowerRpm, "Rated power RPM");
    }

    public PerformanceBenchmark(
            String vehicleName,
            double zeroToSixtySeconds,
            double quarterMileSeconds,
            double quarterMileTrapSpeedMetersPerSecond,
            double rolloutSeconds,
            String source) {
        this(vehicleName, zeroToSixtySeconds, quarterMileSeconds,
                quarterMileTrapSpeedMetersPerSecond, rolloutSeconds, source,
                1.0, 1.0);
    }

    private static void requirePositive(double value, String label) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(label + " must be finite and positive");
        }
    }
}

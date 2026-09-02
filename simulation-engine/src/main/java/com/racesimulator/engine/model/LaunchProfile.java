package com.racesimulator.engine.model;

/** Deterministic launch calibration, independent of driver randomness. */
public record LaunchProfile(
        double launchRpm,
        double engagementDurationSeconds,
        double initialTorqueTransferFraction,
        boolean launchControlEnabled) {

    public LaunchProfile {
        requirePositive(launchRpm, "Launch RPM");
        requirePositive(engagementDurationSeconds, "Engagement duration");
        if (!Double.isFinite(initialTorqueTransferFraction)
                || initialTorqueTransferFraction <= 0.0
                || initialTorqueTransferFraction > 1.0) {
            throw new IllegalArgumentException("Initial torque transfer must be greater than zero and at most one");
        }
    }

    private static void requirePositive(double value, String label) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(label + " must be finite and positive");
        }
    }
}

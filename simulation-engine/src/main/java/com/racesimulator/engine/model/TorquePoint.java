package com.racesimulator.engine.model;

public record TorquePoint(double rpm, double torqueNewtonMeters) {
    public TorquePoint {
        if (!Double.isFinite(rpm) || rpm <= 0.0) {
            throw new IllegalArgumentException("RPM must be finite and positive");
        }
        if (!Double.isFinite(torqueNewtonMeters) || torqueNewtonMeters < 0.0) {
            throw new IllegalArgumentException("Torque must be finite and non-negative");
        }
    }
}

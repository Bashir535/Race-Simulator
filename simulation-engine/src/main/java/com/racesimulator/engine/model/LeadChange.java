package com.racesimulator.engine.model;

public record LeadChange(
        double timeSeconds,
        double distanceMeters,
        String newLeader) {

    public LeadChange {
        if (!Double.isFinite(timeSeconds) || timeSeconds < 0.0) {
            throw new IllegalArgumentException("Lead-change time must be finite and non-negative");
        }
        if (!Double.isFinite(distanceMeters) || distanceMeters < 0.0) {
            throw new IllegalArgumentException("Lead-change distance must be finite and non-negative");
        }
        if (newLeader == null || newLeader.isBlank()) {
            throw new IllegalArgumentException("New leader is required");
        }
    }
}

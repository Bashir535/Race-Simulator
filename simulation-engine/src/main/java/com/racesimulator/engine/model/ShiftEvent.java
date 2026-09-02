package com.racesimulator.engine.model;

public record ShiftEvent(
        double startTimeSeconds,
        double endTimeSeconds,
        int fromGear,
        int toGear,
        double rpmBefore,
        double rpmAfter) {

    public ShiftEvent {
        if (startTimeSeconds < 0.0 || endTimeSeconds <= startTimeSeconds) {
            throw new IllegalArgumentException("A shift must have a positive duration");
        }
        if (fromGear < 1 || toGear != fromGear + 1) {
            throw new IllegalArgumentException("Only sequential upshifts are currently supported");
        }
    }
}

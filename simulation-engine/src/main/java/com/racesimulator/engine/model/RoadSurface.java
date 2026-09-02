package com.racesimulator.engine.model;

/** Baseline grip multipliers. These values must be calibrated with test data. */
public enum RoadSurface {
    PREPARED_DRAG_STRIP(1.15),
    DRY_ASPHALT(1.00),
    WET_ASPHALT(0.65),
    GRAVEL(0.50);

    private final double gripMultiplier;

    RoadSurface(double gripMultiplier) {
        this.gripMultiplier = gripMultiplier;
    }

    public double gripMultiplier() {
        return gripMultiplier;
    }
}

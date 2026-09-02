package com.racesimulator.engine.model;

/** Dominant factor constraining forward acceleration in a telemetry frame. */
public enum AccelerationLimit {
    TRACTION,
    ENGINE_POWER,
    SHIFT,
    AERODYNAMIC_DRAG,
    FINISHED
}

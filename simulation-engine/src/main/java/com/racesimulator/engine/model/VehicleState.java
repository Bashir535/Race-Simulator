package com.racesimulator.engine.model;

public record VehicleState(
        double timeSeconds,
        double distanceMeters,
        double speedMetersPerSecond,
        double accelerationMetersPerSecondSquared,
        double engineRpm,
        int gear,
        double engineTorqueNewtonMeters,
        double requestedWheelForceNewtons,
        double wheelForceNewtons,
        double tractionLimitNewtons,
        boolean tractionLimited,
        double wheelSlipRatio,
        boolean launchActive,
        double torqueTransferFraction,
        double aerodynamicDragNewtons,
        double rollingResistanceNewtons,
        double enginePowerWatts,
        double wheelTorqueNewtonMeters,
        double effectiveDrivetrainEfficiency,
        double airDensityKgPerCubicMeter,
        double gradeResistanceNewtons,
        double netForceNewtons,
        AccelerationLimit accelerationLimit) {

    public VehicleState(
            double timeSeconds,
            double distanceMeters,
            double speedMetersPerSecond,
            double accelerationMetersPerSecondSquared,
            double engineRpm,
            int gear,
            double engineTorqueNewtonMeters,
            double requestedWheelForceNewtons,
            double wheelForceNewtons,
            double tractionLimitNewtons,
            boolean tractionLimited,
            double wheelSlipRatio,
            boolean launchActive,
            double torqueTransferFraction,
            double aerodynamicDragNewtons,
            double rollingResistanceNewtons) {
        this(timeSeconds, distanceMeters, speedMetersPerSecond,
                accelerationMetersPerSecondSquared, engineRpm, gear,
                engineTorqueNewtonMeters, requestedWheelForceNewtons,
                wheelForceNewtons, tractionLimitNewtons, tractionLimited,
                wheelSlipRatio, launchActive, torqueTransferFraction,
                aerodynamicDragNewtons, rollingResistanceNewtons, 0.0, 0.0,
                0.0, 1.225, 0.0,
                accelerationMetersPerSecondSquared, AccelerationLimit.ENGINE_POWER);
    }
}

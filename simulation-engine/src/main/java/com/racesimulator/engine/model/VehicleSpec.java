package com.racesimulator.engine.model;

import java.util.List;
import java.util.Objects;

public record VehicleSpec(
        String name,
        double massKg,
        Drivetrain drivetrain,
        double staticFrontWeightFraction,
        double wheelbaseMeters,
        double centerOfGravityHeightMeters,
        double drivetrainEfficiency,
        double wheelRadiusMeters,
        double dragCoefficient,
        double frontalAreaSquareMeters,
        double rollingResistanceCoefficient,
        double tireFrictionCoefficient,
        double finalDriveRatio,
        List<Double> gearRatios,
        TransmissionType transmissionType,
        double shiftDurationSeconds,
        LaunchProfile launchProfile,
        double idleRpm,
        double shiftRpm,
        double redlineRpm,
        TorqueCurve torqueCurve) {

    public VehicleSpec {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Vehicle name is required");
        }
        Objects.requireNonNull(drivetrain, "Drivetrain is required");
        Objects.requireNonNull(transmissionType, "Transmission type is required");
        Objects.requireNonNull(launchProfile, "Launch profile is required");
        Objects.requireNonNull(torqueCurve, "Torque curve is required");
        gearRatios = List.copyOf(Objects.requireNonNull(gearRatios, "Gear ratios are required"));
        if (gearRatios.isEmpty() || gearRatios.stream().anyMatch(ratio -> ratio == null || ratio <= 0.0)) {
            throw new IllegalArgumentException("At least one positive gear ratio is required");
        }
        requirePositive(massKg, "Mass");
        requireRange(staticFrontWeightFraction, 0.0, 1.0, "Static front weight fraction");
        requirePositive(wheelbaseMeters, "Wheelbase");
        requirePositive(centerOfGravityHeightMeters, "Center-of-gravity height");
        requireRange(drivetrainEfficiency, 0.0, 1.0, "Drivetrain efficiency");
        requirePositive(wheelRadiusMeters, "Wheel radius");
        requirePositive(dragCoefficient, "Drag coefficient");
        requirePositive(frontalAreaSquareMeters, "Frontal area");
        requirePositive(rollingResistanceCoefficient, "Rolling resistance coefficient");
        requirePositive(tireFrictionCoefficient, "Tire friction coefficient");
        requirePositive(finalDriveRatio, "Final drive ratio");
        requirePositive(shiftDurationSeconds, "Shift duration");
        if (shiftDurationSeconds > 1.0) {
            throw new IllegalArgumentException("Shift duration must not exceed one second");
        }
        requirePositive(idleRpm, "Idle RPM");
        requirePositive(shiftRpm, "Shift RPM");
        requirePositive(redlineRpm, "Redline RPM");
        if (idleRpm >= shiftRpm || shiftRpm > redlineRpm) {
            throw new IllegalArgumentException("RPM limits must satisfy idle < shift <= redline");
        }
        if (launchProfile.launchRpm() < idleRpm || launchProfile.launchRpm() > redlineRpm) {
            throw new IllegalArgumentException("Launch RPM must be between idle and redline RPM");
        }
    }

    private static void requirePositive(double value, String label) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(label + " must be finite and positive");
        }
    }

    private static void requireRange(double value, double minimum, double maximum, String label) {
        if (!Double.isFinite(value) || value <= minimum || value > maximum) {
            throw new IllegalArgumentException(label + " must be greater than " + minimum + " and at most " + maximum);
        }
    }
}

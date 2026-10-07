package com.racesimulator.engine.model;

/** Bounded what-if inputs, not a claim that a real aftermarket part achieves them. */
public record VehicleModifications(double torqueMultiplier, double weightReductionKg) {
    public static final VehicleModifications STOCK = new VehicleModifications(1, 0);

    public VehicleModifications {
        if (!Double.isFinite(torqueMultiplier) || torqueMultiplier < 0.8 || torqueMultiplier > 1.3) {
            throw new IllegalArgumentException("Torque multiplier must be between 0.8 and 1.3");
        }
        if (!Double.isFinite(weightReductionKg) || weightReductionKg < 0) {
            throw new IllegalArgumentException("Weight reduction must be finite and non-negative");
        }
    }

    public VehicleSpec apply(VehicleSpec stock) {
        if (weightReductionKg > stock.massKg() * 0.15) {
            throw new IllegalArgumentException("Weight reduction must not exceed 15% of stock mass");
        }
        return new VehicleSpec(stock.name(), stock.massKg() - weightReductionKg, stock.drivetrain(),
                stock.staticFrontWeightFraction(), stock.wheelbaseMeters(), stock.centerOfGravityHeightMeters(),
                stock.drivetrainEfficiency(), stock.wheelRadiusMeters(), stock.dragCoefficient(),
                stock.frontalAreaSquareMeters(), stock.rollingResistanceCoefficient(), stock.tireFrictionCoefficient(),
                stock.finalDriveRatio(), stock.gearRatios(), stock.transmissionType(), stock.shiftDurationSeconds(),
                stock.launchProfile(), stock.idleRpm(), stock.shiftRpm(), stock.redlineRpm(),
                new TorqueCurve(stock.torqueCurve().points().entrySet().stream()
                        .map(p -> new TorquePoint(p.getKey(), p.getValue() * torqueMultiplier)).toList()));
    }
}

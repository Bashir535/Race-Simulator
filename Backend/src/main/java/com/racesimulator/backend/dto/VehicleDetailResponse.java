package com.racesimulator.backend.dto;

import java.util.List;

public record VehicleDetailResponse(
        VehicleTrimResponse identity,
        String bodyStyle,
        Double originalMsrpUsd,
        String tireDescription,
        FuelEfficiency fuelEfficiency,
        Engine engine,
        Transmission transmission,
        SimulationSpecification simulationSpecification,
        List<PublishedPerformance> publishedPerformance,
        String dataStatus) {

    public record FuelEfficiency(Double cityMpg, Double highwayMpg, Double combinedMpg) {}
    public record Engine(String name, Double displacementLiters, String configuration,
                         String aspiration, String fuelType, Double horsepower,
                         Double peakHorsepowerRpm, Double torqueNm, Double peakTorqueRpm,
                         double idleRpm, double shiftRpm, double redlineRpm,
                         List<TorquePoint> torqueCurve) {}
    public record TorquePoint(double rpm, double torqueNm) {}
    public record Transmission(String name, String type, short numberOfGears,
                               double finalDriveRatio, double shiftDurationSeconds,
                               double drivetrainEfficiency, List<Double> gearRatios) {}
    public record SimulationSpecification(double massKg, double frontWeightFraction,
                                          double wheelbaseMeters, double centerOfGravityHeightMeters,
                                          double wheelRadiusMeters, double dragCoefficient,
                                          double frontalAreaSquareMeters,
                                          double rollingResistanceCoefficient,
                                          double tireFrictionCoefficient, double launchRpm,
                                          double launchEngagementDurationSeconds,
                                          double initialTorqueTransferFraction,
                                          boolean launchControlEnabled) {}
    public record PublishedPerformance(Double zeroToSixtySeconds, Double quarterMileSeconds,
                                       Double quarterMileTrapSpeedMph, Double rolloutSeconds,
                                       String source) {}
}

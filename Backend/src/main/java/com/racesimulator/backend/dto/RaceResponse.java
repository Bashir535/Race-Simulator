package com.racesimulator.backend.dto;

import java.util.List;

public record RaceResponse(
        String simulationVersion,
        Configuration config,
        VehicleResult vehicleA,
        VehicleResult vehicleB,
        String winner,
        double winningMarginSeconds,
        Summary summary,
        List<Frame> timeline) {

    public record Configuration(String goalType, Double distanceMeters,
                                Double targetSpeedMetersPerSecond,
                                double startingSpeedMetersPerSecond,
                                String roadSurface, Environment environment) {}
    public record Environment(double airTemperatureCelsius, double airPressurePascals,
                              double relativeHumidity, double roadTemperatureCelsius,
                              double roadGradePercent, double headwindMetersPerSecond) {}
    public record VehicleResult(Long vehicleId, String name, double finishTimeSeconds,
                                double finishSpeedMetersPerSecond, List<Milestone> milestones,
                                List<Shift> shifts) {}
    public record Milestone(String name, double elapsedSeconds, double speedMetersPerSecond) {}
    public record Shift(double startTimeSeconds, double endTimeSeconds, int fromGear,
                        int toGear, double rpmBefore, double rpmAfter) {}
    public record Summary(String winner, double timeMarginSeconds,
                          double distanceGapAtWinnerFinishMeters, String largestLeadVehicle,
                          double largestLeadMeters, List<LeadChange> leadChanges) {}
    public record LeadChange(double timeSeconds, double distanceMeters, String newLeader) {}
    public record Frame(double timeSeconds, VehicleTelemetry vehicleA, VehicleTelemetry vehicleB) {}
    public record VehicleTelemetry(double distanceMeters, double speedMetersPerSecond,
                                   double accelerationMetersPerSecondSquared, double engineRpm,
                                   int gear, boolean tractionLimited, boolean launchActive,
                                   String accelerationLimit) {}
}

package com.racesimulator.engine.validation;

public record PowerCurveValidation(
        double ratedHorsepower,
        double ratedPowerRpm,
        double curveHorsepowerAtRatedRpm,
        double curvePeakHorsepower,
        double percentErrorAtRatedRpm) {

    public boolean withinTolerance(double maximumPercentError) {
        return Math.abs(percentErrorAtRatedRpm) <= maximumPercentError;
    }
}

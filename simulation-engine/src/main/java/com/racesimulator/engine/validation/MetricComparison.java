package com.racesimulator.engine.validation;

public record MetricComparison(
        String metric,
        String unit,
        double simulated,
        double published,
        double percentError) {

    public boolean withinTolerance(double maximumPercentError) {
        return Math.abs(percentError) <= maximumPercentError;
    }
}

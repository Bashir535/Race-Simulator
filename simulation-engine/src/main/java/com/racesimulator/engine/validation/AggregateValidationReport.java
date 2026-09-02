package com.racesimulator.engine.validation;

import java.util.List;

public record AggregateValidationReport(
        List<ValidationReport> vehicleReports,
        double meanAbsolutePercentError,
        double zeroToSixtyMeanAbsolutePercentError,
        double quarterMileMeanAbsolutePercentError,
        double trapSpeedMeanAbsolutePercentError,
        double worstAbsolutePercentError,
        String worstVehicle,
        String worstMetric) {

    public AggregateValidationReport {
        vehicleReports = List.copyOf(vehicleReports);
    }

    public String asText() {
        return String.format(
                "Aggregate validation: %d vehicles, mean absolute error %.2f%% "
                        + "(0-60 %.2f%%, 1/4-mile %.2f%%, trap %.2f%%), "
                        + "worst error %.2f%% (%s, %s)%n",
                vehicleReports.size(), meanAbsolutePercentError,
                zeroToSixtyMeanAbsolutePercentError,
                quarterMileMeanAbsolutePercentError,
                trapSpeedMeanAbsolutePercentError,
                worstAbsolutePercentError, worstVehicle, worstMetric);
    }
}

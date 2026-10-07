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
                "Standing-start timing diagnostics: %d vehicles, timing mean absolute error %.2f%% "
                        + "(0-60 %.2f%%, 1/4-mile %.2f%%; finish-vs-trap proxy %.2f%% excluded from mean), "
                        + "worst error %.2f%% (%s, %s)%n",
                vehicleReports.size(), meanAbsolutePercentError,
                zeroToSixtyMeanAbsolutePercentError,
                quarterMileMeanAbsolutePercentError,
                trapSpeedMeanAbsolutePercentError,
                worstAbsolutePercentError, worstVehicle, worstMetric);
    }
}

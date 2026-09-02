package com.racesimulator.engine.validation;

import java.util.List;

public record ValidationReport(
        String vehicleName,
        String benchmarkSource,
        List<MetricComparison> comparisons) {

    public ValidationReport {
        comparisons = List.copyOf(comparisons);
    }

    public boolean allWithinTolerance(double maximumPercentError) {
        return comparisons.stream()
                .allMatch(comparison -> comparison.withinTolerance(maximumPercentError));
    }

    public String asText() {
        StringBuilder report = new StringBuilder()
                .append("Validation: ").append(vehicleName).append(System.lineSeparator())
                .append("Source: ").append(benchmarkSource).append(System.lineSeparator())
                .append(String.format("%-18s %12s %12s %12s%n", "Metric", "Simulated", "Published", "Error"));

        for (MetricComparison comparison : comparisons) {
            report.append(String.format(
                    "%-18s %8.2f %-3s %8.2f %-3s %+10.2f%%%n",
                    comparison.metric(),
                    comparison.simulated(),
                    comparison.unit(),
                    comparison.published(),
                    comparison.unit(),
                    comparison.percentError()));
        }
        return report.toString();
    }
}

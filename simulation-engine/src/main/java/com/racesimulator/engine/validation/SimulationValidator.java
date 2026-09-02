package com.racesimulator.engine.validation;

import com.racesimulator.engine.model.Milestone;
import com.racesimulator.engine.model.VehicleRaceResult;

import java.util.List;
import java.util.Comparator;

public final class SimulationValidator {
    private static final double METERS_PER_SECOND_TO_MPH = 2.2369362921;

    public ValidationReport compare(
            VehicleRaceResult simulatedResult,
            PerformanceBenchmark benchmark) {
        if (!simulatedResult.vehicle().name().equals(benchmark.vehicleName())) {
            throw new IllegalArgumentException("Simulation and benchmark vehicle names must match");
        }

        Milestone zeroToSixty = milestone(simulatedResult, "0-60 mph");
        double simulatedTrapSpeedMph = simulatedResult.finishSpeedMetersPerSecond()
                * METERS_PER_SECOND_TO_MPH;
        double publishedTrapSpeedMph = benchmark.quarterMileTrapSpeedMetersPerSecond()
                * METERS_PER_SECOND_TO_MPH;

        return new ValidationReport(
                benchmark.vehicleName(),
                benchmark.source(),
                List.of(
                        comparison("0-60 reported", "s", zeroToSixty.elapsedSeconds(), benchmark.zeroToSixtySeconds()),
                        comparison("0-60 no rollout", "s", zeroToSixty.elapsedSeconds(),
                                benchmark.zeroToSixtySeconds() + benchmark.rolloutSeconds()),
                        comparison("1/4 reported", "s", simulatedResult.finishTimeSeconds(), benchmark.quarterMileSeconds()),
                        comparison("1/4 no rollout", "s", simulatedResult.finishTimeSeconds(),
                                benchmark.quarterMileSeconds() + benchmark.rolloutSeconds()),
                        comparison("Trap speed", "mph", simulatedTrapSpeedMph, publishedTrapSpeedMph)));
    }

    public PowerCurveValidation validatePowerCurve(
            VehicleRaceResult simulatedResult,
            PerformanceBenchmark benchmark) {
        if (!simulatedResult.vehicle().name().equals(benchmark.vehicleName())) {
            throw new IllegalArgumentException("Simulation and benchmark vehicle names must match");
        }
        double atRatedRpm = simulatedResult.vehicle().torqueCurve()
                .horsepowerAt(benchmark.ratedPowerRpm());
        return new PowerCurveValidation(
                benchmark.ratedHorsepower(),
                benchmark.ratedPowerRpm(),
                atRatedRpm,
                simulatedResult.vehicle().torqueCurve().peakHorsepower(),
                ((atRatedRpm - benchmark.ratedHorsepower()) / benchmark.ratedHorsepower()) * 100.0);
    }

    public AggregateValidationReport aggregate(List<ValidationReport> reports) {
        if (reports == null || reports.isEmpty()) {
            throw new IllegalArgumentException("At least one validation report is required");
        }
        List<ValidationReport> copy = List.copyOf(reports);
        List<LocatedMetric> metrics = copy.stream()
                .flatMap(report -> report.comparisons().stream()
                        .filter(comparison -> comparison.metric().equals("0-60 no rollout")
                                || comparison.metric().equals("1/4 no rollout")
                                || comparison.metric().equals("Trap speed"))
                        .map(comparison -> new LocatedMetric(report.vehicleName(), comparison)))
                .toList();
        double mean = metrics.stream()
                .mapToDouble(metric -> Math.abs(metric.comparison().percentError()))
                .average()
                .orElseThrow();
        LocatedMetric worst = metrics.stream()
                .max(Comparator.comparingDouble(
                        metric -> Math.abs(metric.comparison().percentError())))
                .orElseThrow();
        double zeroToSixtyMean = metricMean(metrics, "0-60 no rollout");
        double quarterMileMean = metricMean(metrics, "1/4 no rollout");
        double trapSpeedMean = metricMean(metrics, "Trap speed");
        return new AggregateValidationReport(copy, mean,
                zeroToSixtyMean, quarterMileMean, trapSpeedMean,
                Math.abs(worst.comparison().percentError()), worst.vehicle(),
                worst.comparison().metric());
    }

    private double metricMean(List<LocatedMetric> metrics, String metricName) {
        return metrics.stream()
                .filter(metric -> metric.comparison().metric().equals(metricName))
                .mapToDouble(metric -> Math.abs(metric.comparison().percentError()))
                .average()
                .orElseThrow();
    }

    private record LocatedMetric(String vehicle, MetricComparison comparison) {
    }

    private Milestone milestone(VehicleRaceResult result, String name) {
        return result.milestones().stream()
                .filter(candidate -> candidate.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Simulation result is missing milestone: " + name));
    }

    private MetricComparison comparison(
            String metric,
            String unit,
            double simulated,
            double published) {
        double percentError = ((simulated - published) / published) * 100.0;
        return new MetricComparison(metric, unit, simulated, published, percentError);
    }
}

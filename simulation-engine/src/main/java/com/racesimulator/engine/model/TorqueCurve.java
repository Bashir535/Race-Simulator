package com.racesimulator.engine.model;

import java.util.Collection;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.TreeMap;

/** Immutable RPM-to-torque curve with linear interpolation between points. */
public final class TorqueCurve {
    private static final double WATTS_PER_HORSEPOWER = 745.6998715822702;
    private final NavigableMap<Double, Double> points;

    public TorqueCurve(Collection<TorquePoint> torquePoints) {
        if (torquePoints == null || torquePoints.size() < 2) {
            throw new IllegalArgumentException("A torque curve requires at least two points");
        }

        TreeMap<Double, Double> sorted = new TreeMap<>();
        for (TorquePoint point : torquePoints) {
            if (sorted.put(point.rpm(), point.torqueNewtonMeters()) != null) {
                throw new IllegalArgumentException("Torque curve RPM values must be unique");
            }
        }
        this.points = Collections.unmodifiableNavigableMap(sorted);
    }

    public double torqueAt(double rpm) {
        if (!Double.isFinite(rpm)) {
            throw new IllegalArgumentException("RPM must be finite");
        }

        var lower = points.floorEntry(rpm);
        var upper = points.ceilingEntry(rpm);

        if (lower == null) {
            return points.firstEntry().getValue();
        }
        if (upper == null) {
            return 0.0;
        }
        if (lower.getKey().equals(upper.getKey())) {
            return lower.getValue();
        }

        double position = (rpm - lower.getKey()) / (upper.getKey() - lower.getKey());
        return lower.getValue() + position * (upper.getValue() - lower.getValue());
    }

    public NavigableMap<Double, Double> points() {
        return points;
    }

    public double powerWattsAt(double rpm) {
        return torqueAt(rpm) * rpm * (2.0 * Math.PI / 60.0);
    }

    public double horsepowerAt(double rpm) {
        return powerWattsAt(rpm) / WATTS_PER_HORSEPOWER;
    }

    /** Highest power represented by the piecewise-linear torque curve. */
    public double peakHorsepower() {
        return points.keySet().stream()
                .mapToDouble(this::horsepowerAt)
                .max()
                .orElseThrow();
    }
}

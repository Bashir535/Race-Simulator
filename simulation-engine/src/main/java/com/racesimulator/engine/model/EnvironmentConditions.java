package com.racesimulator.engine.model;

/** Physical ambient and track inputs used by the deterministic model. */
public record EnvironmentConditions(
        double airTemperatureCelsius,
        double airPressurePascals,
        double relativeHumidity,
        double roadTemperatureCelsius,
        double roadGradePercent,
        double headwindMetersPerSecond) {

    public static final EnvironmentConditions STANDARD =
            new EnvironmentConditions(15.0, 101_325.0, 0.0, 25.0, 0.0, 0.0);

    public EnvironmentConditions {
        requireFinite(airTemperatureCelsius, "Air temperature");
        if (airTemperatureCelsius <= -273.15 || airTemperatureCelsius > 80.0) {
            throw new IllegalArgumentException("Air temperature is outside the supported range");
        }
        if (!Double.isFinite(airPressurePascals) || airPressurePascals < 50_000.0
                || airPressurePascals > 120_000.0) {
            throw new IllegalArgumentException("Air pressure must be between 50,000 and 120,000 Pa");
        }
        if (!Double.isFinite(relativeHumidity) || relativeHumidity < 0.0 || relativeHumidity > 1.0) {
            throw new IllegalArgumentException("Relative humidity must be between zero and one");
        }
        requireFinite(roadTemperatureCelsius, "Road temperature");
        if (roadTemperatureCelsius < -50.0 || roadTemperatureCelsius > 100.0) {
            throw new IllegalArgumentException("Road temperature is outside the supported range");
        }
        requireFinite(roadGradePercent, "Road grade");
        if (Math.abs(roadGradePercent) > 30.0) {
            throw new IllegalArgumentException("Road grade magnitude must not exceed 30 percent");
        }
        requireFinite(headwindMetersPerSecond, "Headwind");
        if (Math.abs(headwindMetersPerSecond) > 100.0) {
            throw new IllegalArgumentException("Headwind magnitude must not exceed 100 m/s");
        }
    }

    private static void requireFinite(double value, String label) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(label + " must be finite");
        }
    }
}

package com.racesimulator.backend.dto;

import com.racesimulator.engine.model.RoadSurface;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record RaceRequest(
        @NotNull @Positive Long vehicleAId,
        @NotNull @Positive Long vehicleBId,
        @NotNull @Valid RaceConfiguration race) {

    public enum GoalType { DISTANCE, SPEED }

    public record RaceConfiguration(
            @NotNull GoalType goalType,
            @Positive Double distanceMeters,
            @Positive Double targetSpeedMetersPerSecond,
            @PositiveOrZero double startingSpeedMetersPerSecond,
            @NotNull RoadSurface roadSurface,
            @Valid Environment environment) {
    }

    public record Environment(
            @DecimalMin("-273.14") @DecimalMax("80.0") double airTemperatureCelsius,
            @DecimalMin("50000") @DecimalMax("120000") double airPressurePascals,
            @DecimalMin("0.0") @DecimalMax("1.0") double relativeHumidity,
            @DecimalMin("-50.0") @DecimalMax("100.0") double roadTemperatureCelsius,
            @DecimalMin("-30.0") @DecimalMax("30.0") double roadGradePercent,
            @DecimalMin("-100.0") @DecimalMax("100.0") double headwindMetersPerSecond) {
    }
}

package com.racesimulator.backend.dto;

public record VehicleModelResponse(
        Long id,
        Long makeId,
        String name,
        String generation,
        String bodyStyle,
        Short productionStartYear,
        Short productionEndYear) {
}

package com.racesimulator.backend.dto;

public record VehicleTrimResponse(
        Long id,
        short year,
        String make,
        String model,
        String generation,
        String trim,
        String drivetrain,
        String transmission,
        Double horsepower,
        Double torqueNm,
        double massKg,
        String imageUrl,
        boolean popular) {
}

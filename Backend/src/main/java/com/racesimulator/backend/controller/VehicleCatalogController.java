package com.racesimulator.backend.controller;

import com.racesimulator.backend.dto.VehicleMakeResponse;
import com.racesimulator.backend.dto.VehicleModelResponse;
import com.racesimulator.backend.dto.VehicleTrimResponse;
import com.racesimulator.backend.dto.VehicleDetailResponse;
import com.racesimulator.backend.service.VehicleCatalogService;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles")
@Validated
public class VehicleCatalogController {

    private final VehicleCatalogService vehicleCatalogService;

    public VehicleCatalogController(VehicleCatalogService vehicleCatalogService) {
        this.vehicleCatalogService = vehicleCatalogService;
    }

    @GetMapping("/makes")
    public List<VehicleMakeResponse> getMakes(
            @RequestParam(required = false) @Min(1886) @Max(2200) Short year) {
        return year == null ? vehicleCatalogService.findMakes() : vehicleCatalogService.findMakes(year);
    }

    @GetMapping("/years")
    public List<Short> getYears() {
        return vehicleCatalogService.findYears();
    }

    @GetMapping("/models")
    public List<VehicleModelResponse> getModels(
            @RequestParam @Positive(message = "makeId must be positive") Long makeId,
            @RequestParam(required = false) @Min(1886) @Max(2200) Short year) {
        return year == null ? vehicleCatalogService.findModels(makeId)
                : vehicleCatalogService.findModels(makeId, year);
    }

    @GetMapping("/trims")
    public List<VehicleTrimResponse> getTrims(
            @RequestParam @Positive Long modelId,
            @RequestParam @Min(1886) @Max(2200) Short year) {
        return vehicleCatalogService.findTrims(modelId, year);
    }

    @GetMapping("/popular")
    public List<VehicleTrimResponse> getPopular() {
        return vehicleCatalogService.findPopular();
    }

    @GetMapping("/{id}")
    public VehicleDetailResponse getVehicle(@PathVariable @Positive Long id) {
        return vehicleCatalogService.findVehicle(id);
    }
}

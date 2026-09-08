package com.racesimulator.backend.controller;

import com.racesimulator.backend.dto.VehicleMakeResponse;
import com.racesimulator.backend.dto.VehicleModelResponse;
import com.racesimulator.backend.service.VehicleCatalogService;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public List<VehicleMakeResponse> getMakes() {
        return vehicleCatalogService.findMakes();
    }

    @GetMapping("/models")
    public List<VehicleModelResponse> getModels(
            @RequestParam @Positive(message = "makeId must be positive") Long makeId) {
        return vehicleCatalogService.findModels(makeId);
    }
}

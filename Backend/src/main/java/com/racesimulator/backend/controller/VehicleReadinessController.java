package com.racesimulator.backend.controller;

import com.racesimulator.backend.service.VehicleReadinessService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleReadinessController {
    private final VehicleReadinessService service;
    public VehicleReadinessController(VehicleReadinessService service) { this.service = service; }
    @GetMapping("/{id}/readiness")
    public VehicleReadinessService.Readiness readiness(@PathVariable Long id) { return service.inspect(id); }
}

package com.racesimulator.backend.controller;

import com.racesimulator.backend.dto.RaceRequest;
import com.racesimulator.backend.dto.RaceResponse;
import com.racesimulator.backend.service.RaceSimulationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/races")
public class RaceController {
    private final RaceSimulationService raceSimulationService;
    public RaceController(RaceSimulationService raceSimulationService) {
        this.raceSimulationService = raceSimulationService;
    }

    @PostMapping("/simulate")
    @ResponseStatus(HttpStatus.OK)
    public RaceResponse simulate(@Valid @RequestBody RaceRequest request) {
        return raceSimulationService.simulate(request);
    }
}

package com.racesimulator.backend.controller;

import com.racesimulator.backend.dto.RaceRequest;
import com.racesimulator.backend.service.GarageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/garage")
public class GarageController {
    private final GarageService garage;
    public GarageController(GarageService garage) { this.garage = garage; }
    public record SaveVehicle(@NotNull @Positive Long trimId, @Valid RaceRequest.Modifications modifications) {}
    @GetMapping
    public List<GarageService.Entry> list(@RequestHeader("X-Garage-Key") String key) { return garage.list(key); }
    @GetMapping("/{id}")
    public JsonNode get(@RequestHeader("X-Garage-Key") String key, @PathVariable UUID id) { return garage.get(key, id); }
    @PostMapping("/races")
    @ResponseStatus(HttpStatus.CREATED)
    public GarageService.Entry saveRace(@RequestHeader("X-Garage-Key") String key, @Valid @RequestBody RaceRequest request) {
        return garage.saveRace(key, request);
    }
    @PostMapping("/vehicles")
    @ResponseStatus(HttpStatus.CREATED)
    public GarageService.Entry saveVehicle(@RequestHeader("X-Garage-Key") String key, @Valid @RequestBody SaveVehicle request) {
        return garage.saveVehicle(key, request.trimId(), request.modifications());
    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader("X-Garage-Key") String key, @PathVariable UUID id) { garage.delete(key, id); }
}

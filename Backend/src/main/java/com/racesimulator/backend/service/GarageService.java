package com.racesimulator.backend.service;

import com.racesimulator.backend.dto.RaceRequest;
import com.racesimulator.backend.dto.RaceResponse;
import com.racesimulator.backend.dto.VehicleDetailResponse;
import com.racesimulator.backend.exception.ResourceNotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class GarageService {
    private final JdbcTemplate jdbc;
    private final JsonMapper json;
    private final VehicleCatalogService catalog;
    private final RaceSimulationService simulation;
    private final VehicleSpecMapper mapper;
    private final VehicleReadinessService readiness;
    public GarageService(JdbcTemplate jdbc, JsonMapper json, VehicleCatalogService catalog,
                         RaceSimulationService simulation, VehicleSpecMapper mapper, VehicleReadinessService readiness) {
        this.jdbc = jdbc; this.json = json; this.catalog = catalog; this.simulation = simulation; this.mapper = mapper;
        this.readiness = readiness;
    }
    public record Entry(UUID id, String kind, String label, String createdAt) {}
    public record RaceSnapshot(int schemaVersion, String simulationVersion, RaceRequest request,
                               VehicleDetailResponse stockVehicleA, VehicleDetailResponse stockVehicleB,
                               RaceResponse response, VehicleReadinessService.Readiness evidenceA,
                               VehicleReadinessService.Readiness evidenceB) {}
    public record VehicleSnapshot(int schemaVersion, String simulationVersion, VehicleDetailResponse stockVehicle,
                                  RaceRequest.Modifications modifications, VehicleReadinessService.Readiness evidence) {}

    public List<Entry> list(String token) {
        return jdbc.query("SELECT id, kind, label, created_at FROM garage_snapshots WHERE owner_hash = ? ORDER BY created_at DESC LIMIT 100",
                (rs, n) -> new Entry(rs.getObject("id", UUID.class), rs.getString("kind"), rs.getString("label"), rs.getString("created_at")), owner(token));
    }
    public JsonNode get(String token, UUID id) {
        var rows = jdbc.query("SELECT payload::text FROM garage_snapshots WHERE owner_hash = ? AND id = ?",
                (rs, n) -> rs.getString(1), owner(token), id);
        if (rows.isEmpty()) throw new ResourceNotFoundException("Saved item was not found");
        return json.readTree(rows.getFirst());
    }
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Entry saveRace(String token, RaceRequest request) {
        String owner = owner(token);
        // Server computes the outcome. Never accept user-supplied results as authoritative.
        var result = simulation.simulate(request);
        var snapshot = new RaceSnapshot(1, RaceSimulationService.SIMULATION_VERSION, request,
                catalog.findVehicle(request.vehicleAId()), catalog.findVehicle(request.vehicleBId()), result,
                readiness.inspect(request.vehicleAId()), readiness.inspect(request.vehicleBId()));
        return insert(owner, "RACE", result.vehicleA().name() + " vs " + result.vehicleB().name(), snapshot);
    }
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Entry saveVehicle(String token, Long trimId, RaceRequest.Modifications modifications) {
        String owner = owner(token);
        var mods = modifications == null ? new RaceRequest.Modifications(1, 0) : modifications;
        mods.toEngine().apply(mapper.toEngineSpec(catalog.findDetailedTrim(trimId)));
        var vehicle = catalog.findVehicle(trimId);
        return insert(owner, "VEHICLE", vehicle.identity().year() + " " + vehicle.identity().make() + " " + vehicle.identity().model(),
                new VehicleSnapshot(1, RaceSimulationService.SIMULATION_VERSION, vehicle, mods, readiness.inspect(trimId)));
    }
    public void delete(String token, UUID id) {
        if (jdbc.update("DELETE FROM garage_snapshots WHERE owner_hash = ? AND id = ?", owner(token), id) == 0) {
            throw new ResourceNotFoundException("Saved item was not found");
        }
    }
    private Entry insert(String owner, String kind, String label, Object payload) {
        UUID id = UUID.randomUUID();
        String shortLabel = label.substring(0, Math.min(200, label.length()));
        jdbc.update("INSERT INTO garage_snapshots(id, owner_hash, kind, label, payload) VALUES (?, ?, ?, ?, CAST(? AS jsonb))",
                id, owner, kind, shortLabel, json.writeValueAsString(payload));
        return new Entry(id, kind, shortLabel, java.time.Instant.now().toString());
    }
    private String owner(String token) {
        if (token == null || !token.matches("[a-fA-F0-9]{64}")) {
            throw new IllegalArgumentException("X-Garage-Key must be a 32-byte hexadecimal capability token");
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}

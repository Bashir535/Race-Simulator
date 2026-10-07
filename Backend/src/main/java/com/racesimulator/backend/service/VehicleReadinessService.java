package com.racesimulator.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/** Structural readiness is deliberately separate from measured real-world accuracy. */
@Service
@Transactional(readOnly = true)
public class VehicleReadinessService {
    private final VehicleCatalogService catalog;
    private final VehicleSpecMapper mapper;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    public VehicleReadinessService(VehicleCatalogService catalog, VehicleSpecMapper mapper, org.springframework.jdbc.core.JdbcTemplate jdbc) {
        this.catalog = catalog;
        this.mapper = mapper;
        this.jdbc = jdbc;
    }
    public record Evidence(String provider, String url, String status, String confidence, String notes) {}
    public record Readiness(boolean simulationReady, String accuracyStatus, List<String> blockers,
                            List<String> assumptions, List<String> benchmarkSources, List<Evidence> evidence) {}

    public Readiness inspect(Long id) {
        var trim = catalog.findDetailedTrim(id);
        try {
            var spec = mapper.toEngineSpec(trim);
            if (spec.torqueCurve().points().firstKey() > spec.idleRpm()
                    || spec.torqueCurve().points().lastKey() < spec.redlineRpm()) {
                return new Readiness(false, "UNVALIDATED", List.of("Torque curve must cover idle through redline"), List.of(), List.of(), evidence(id));
            }
        } catch (IllegalArgumentException | NullPointerException ex) {
            return new Readiness(false, "INCOMPLETE", List.of("Required physics inputs are missing or invalid"), List.of(), List.of(), evidence(id));
        }
        var sources = trim.getPublishedPerformance().stream().map(p -> p.getSourceDescription())
                .filter(s -> s != null && !s.isBlank()).distinct().toList();
        return new Readiness(true, "DEVELOPMENT_FIXTURE", List.of(), List.of(
                "Complete inputs do not mean independently validated accuracy",
                "Torque curve, tire grip, drivetrain loss, launch and shift parameters include modeling assumptions",
                "Standing-start benchmarks do not validate rolling acceleration",
                "Published trap speed and instantaneous finish speed are different measurements"), sources, evidence(id));
    }

    private List<Evidence> evidence(Long id) {
        return jdbc.query("""
                SELECT s.provider_name, s.source_url, ts.data_status, ts.confidence_level, ts.notes
                FROM vehicle_trim_sources ts JOIN vehicle_data_sources s ON s.id = ts.source_id
                WHERE ts.vehicle_trim_id = ? ORDER BY s.id
                """, (rs, n) -> new Evidence(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)), id);
    }
}

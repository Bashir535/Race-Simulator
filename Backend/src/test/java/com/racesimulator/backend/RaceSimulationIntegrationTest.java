package com.racesimulator.backend;

import com.racesimulator.backend.dto.RaceRequest;
import com.racesimulator.backend.repository.VehicleTrimRepository;
import com.racesimulator.backend.service.RaceSimulationService;
import com.racesimulator.engine.model.RoadSurface;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class RaceSimulationIntegrationTest {
    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired RaceSimulationService simulationService;
    @Autowired VehicleTrimRepository trimRepository;
    @Autowired com.racesimulator.backend.service.GarageService garage;
    @Autowired com.racesimulator.backend.service.VehicleCatalogService catalog;
    @Autowired com.racesimulator.backend.service.VehicleReadinessService readiness;
    @Autowired tools.jackson.databind.json.JsonMapper json;
    @org.springframework.beans.factory.annotation.Value("${local.server.port}") int port;

    private RaceRequest request() {
        var vehicles = trimRepository.findAllByPopularTrueOrderByModelYearDesc();
        return new RaceRequest(vehicles.get(0).getId(), vehicles.get(1).getId(),
                new RaceRequest.RaceConfiguration(RaceRequest.GoalType.DISTANCE, 201.168, null,
                        40 * 0.44704, RoadSurface.DRY_ASPHALT, null));
    }

    @Test
    void savedRaceRetainsInputsTimelineAndVersionAndIsIsolatedByOwner() {
        String owner = "a".repeat(64), other = "b".repeat(64);
        var request = request();
        var entry = garage.saveRace(owner, request);
        try {
            var saved = garage.get(owner, entry.id());
            assertThat(saved.path("simulationVersion").asText()).isEqualTo(RaceSimulationService.SIMULATION_VERSION);
            assertThat(saved.path("stockVehicleA").path("engine").path("torqueCurve").size()).isGreaterThan(1);
            assertThat(saved.path("stockVehicleA").path("transmission").path("gearRatios").size()).isGreaterThan(1);
            assertThat(saved.path("response").path("timeline").size()).isGreaterThan(1);
            assertThat(saved.path("evidenceA").path("evidence").size()).isGreaterThan(0);
            assertThat(saved.path("request").path("race").path("startingSpeedMetersPerSecond").asDouble()).isEqualTo(40 * 0.44704);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> garage.get(other, entry.id()))
                    .isInstanceOf(com.racesimulator.backend.exception.ResourceNotFoundException.class);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> garage.delete(other, entry.id()))
                    .isInstanceOf(com.racesimulator.backend.exception.ResourceNotFoundException.class);
            assertThat(garage.list(other)).noneMatch(e -> e.id().equals(entry.id()));
        } finally { garage.delete(owner, entry.id()); }
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> garage.get(owner, entry.id()))
                .isInstanceOf(com.racesimulator.backend.exception.ResourceNotFoundException.class);
    }

    @Test
    void modificationsChangeRaceButNeverStockCatalogAndBuildsPersist() {
        var request = request();
        var before = catalog.findVehicle(request.vehicleAId());
        var mods = new RaceRequest.Modifications(1.2, 100);
        var modified = new RaceRequest(request.vehicleAId(), request.vehicleBId(), request.race(), mods, null);
        var baseResult = simulationService.simulate(request);
        var modifiedResult = simulationService.simulate(modified);
        assertThat(modifiedResult.vehicleA().finishTimeSeconds()).isLessThan(baseResult.vehicleA().finishTimeSeconds());
        assertThat(catalog.findVehicle(request.vehicleAId())).isEqualTo(before);
        assertThat(readiness.inspect(request.vehicleAId()).simulationReady()).isTrue();
        var entry = garage.saveVehicle("c".repeat(64), request.vehicleAId(), mods);
        try {
            assertThat(garage.get("c".repeat(64), entry.id()).path("modifications").path("torqueMultiplier").asDouble()).isEqualTo(1.2);
        } finally { garage.delete("c".repeat(64), entry.id()); }
    }

    @Test
    void httpValidatesRequestsAndGarageHeadersAndSupportsCors() throws Exception {
        var client = java.net.http.HttpClient.newHttpClient();
        var base = "http://localhost:" + port + "/api/v1";
        var request = request();
        var bad = new RaceRequest(request.vehicleAId(), request.vehicleBId(), request.race(), new RaceRequest.Modifications(2, 0), null);
        var response = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/races/simulate"))
                .header("Content-Type", "application/json").POST(java.net.http.HttpRequest.BodyPublishers.ofString(json.writeValueAsString(bad))).build(), java.net.http.HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(400);
        var noKey = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/garage")).GET().build(), java.net.http.HttpResponse.BodyHandlers.ofString());
        assertThat(noKey.statusCode()).isEqualTo(400);
        var ready = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/vehicles/" + request.vehicleAId() + "/readiness")).GET().build(), java.net.http.HttpResponse.BodyHandlers.ofString());
        assertThat(ready.statusCode()).isEqualTo(200);
        assertThat(json.readTree(ready.body()).path("simulationReady").asBoolean()).isTrue();
        var cors = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/garage"))
                .header("Origin", "http://localhost:5173").header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type,x-garage-key").method("OPTIONS", java.net.http.HttpRequest.BodyPublishers.noBody()).build(), java.net.http.HttpResponse.BodyHandlers.ofString());
        assertThat(cors.statusCode()).isEqualTo(200);
        assertThat(cors.headers().firstValue("Access-Control-Allow-Origin")).contains("http://localhost:5173");
    }

    @Test
    void newCatalogCarsHaveCompletePhysicsAndFinishDigAndRollRaces() {
        var vehicles = trimRepository.findAllByPopularTrueOrderByModelYearDesc();
        var added = vehicles.stream().filter(v -> v.getTrimName().contains("Competition")
                || v.getTrimName().contains("MCT") || v.getTrimName().contains("Z06")).toList();
        assertThat(added).hasSize(4);
        for (var v : added) {
            assertThat(readiness.inspect(v.getId()).simulationReady()).isTrue();
            var detail = catalog.findVehicle(v.getId());
            var curve = new com.racesimulator.engine.model.TorqueCurve(detail.engine().torqueCurve().stream()
                    .map(p -> new com.racesimulator.engine.model.TorquePoint(p.rpm(), p.torqueNm())).toList());
            assertThat(curve.peakHorsepower()).isBetween(detail.engine().horsepower() * .99,
                    detail.engine().horsepower() * 1.02);
            for (double startMph : new double[]{0, 40, 70}) {
                for (double distance : new double[]{201.168, 402.336}) {
                    var req = new RaceRequest(v.getId(), vehicles.getFirst().getId(),
                            new RaceRequest.RaceConfiguration(RaceRequest.GoalType.DISTANCE, distance, null,
                                    startMph * .44704, RoadSurface.PREPARED_DRAG_STRIP, null));
                    var result = simulationService.simulate(req);
                    assertThat(result.vehicleA().finishTimeSeconds()).isBetween(.1, 25.0);
                    assertThat(result.vehicleA().finishSpeedMetersPerSecond()).isBetween(startMph * .44704, 100.0);
                    assertThat(result.timeline().getLast().vehicleA().distanceMeters()).isGreaterThanOrEqualTo(distance);
                    for (var frame : result.timeline()) {
                        assertThat(frame.vehicleA().engineRpm()).isFinite();
                        assertThat(frame.vehicleA().gear()).isBetween(1, (int) detail.transmission().numberOfGears());
                        assertThat(frame.vehicleA().speedMetersPerSecond()).isFinite().isNotNegative();
                    }
                    if (startMph == 70) assertThat(result.timeline().getFirst().vehicleA().gear()).isGreaterThan(1);
                    if (startMph == 0 && distance == 402.336) {
                        System.out.printf("CATALOG BASELINE %s: %.3f s, finish %.1f mph (not trap)%n",
                                result.vehicleA().name(), result.vehicleA().finishTimeSeconds(),
                                result.vehicleA().finishSpeedMetersPerSecond() / .44704);
                        assertThat(simulationService.simulate(req)).isEqualTo(result);
                    }
                }
            }
        }
    }

    @Test
    void migrationsSeedEightVehiclesAndEngineRunsQuarterMileMatchups() {
        var vehicles = trimRepository.findAllByPopularTrueOrderByModelYearDesc();
        assertThat(vehicles).hasSize(8);

        var mustang = vehicles.stream().filter(vehicle -> vehicle.getTrimName().contains("GT Performance"))
                .findFirst().orElseThrow();
        var corvette = vehicles.stream().filter(vehicle -> vehicle.getTrimName().contains("Stingray"))
                .findFirst().orElseThrow();
        var civic = vehicles.stream().filter(vehicle -> vehicle.getTrimName().contains("Type R"))
                .findFirst().orElseThrow();
        var golf = vehicles.stream().filter(vehicle -> vehicle.getTrimName().equals("R (7DSG)"))
                .findFirst().orElseThrow();

        var request = new RaceRequest(
                mustang.getId(),
                corvette.getId(),
                new RaceRequest.RaceConfiguration(
                        RaceRequest.GoalType.DISTANCE,
                        402.336,
                        null,
                        0.0,
                        RoadSurface.PREPARED_DRAG_STRIP,
                        null));

        var response = simulationService.simulate(request);

        assertThat(response.winner()).contains("Corvette");
        assertThat(response.vehicleA().milestones()).extracting("name").contains("0-60 mph", "1/8 mile");
        assertThat(response.vehicleB().milestones()).extracting("name").contains("0-60 mph", "1/8 mile");
        assertThat(response.timeline()).hasSizeGreaterThan(1_000);
        assertThat(response.timeline().getFirst().timeSeconds()).isZero();
        assertThat(response.timeline().getLast().vehicleA().distanceMeters()).isGreaterThanOrEqualTo(402.336);
        assertThat(response.timeline().getLast().vehicleB().distanceMeters()).isGreaterThanOrEqualTo(402.336);

        var hatchbackResponse = simulationService.simulate(new RaceRequest(
                civic.getId(),
                golf.getId(),
                new RaceRequest.RaceConfiguration(
                        RaceRequest.GoalType.DISTANCE,
                        402.336,
                        null,
                        0.0,
                        RoadSurface.PREPARED_DRAG_STRIP,
                        null)));

        assertThat(hatchbackResponse.winner()).contains("Golf R");
        assertThat(hatchbackResponse.vehicleA().milestones()).extracting("name")
                .contains("0-60 mph", "1/8 mile");
        assertThat(hatchbackResponse.vehicleB().milestones()).extracting("name")
                .contains("0-60 mph", "1/8 mile");
        assertThat(hatchbackResponse.vehicleA().shifts()).isNotEmpty();
        assertThat(hatchbackResponse.vehicleB().shifts()).isNotEmpty();
    }
}

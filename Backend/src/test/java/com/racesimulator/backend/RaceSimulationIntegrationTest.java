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

@SpringBootTest
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

    @Test
    void migrationsSeedTwoVehiclesAndEngineRunsQuarterMile() {
        var vehicles = trimRepository.findAllByPopularTrueOrderByModelYearDesc();
        assertThat(vehicles).hasSize(2);

        var mustang = vehicles.stream().filter(vehicle -> vehicle.getTrimName().contains("GT Performance"))
                .findFirst().orElseThrow();
        var corvette = vehicles.stream().filter(vehicle -> vehicle.getTrimName().contains("Stingray"))
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
    }
}

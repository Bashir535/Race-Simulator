package com.racesimulator.backend.service;

import com.racesimulator.backend.dto.RaceRequest;
import com.racesimulator.backend.repository.VehicleTrimRepository;
import com.racesimulator.engine.model.RoadSurface;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class RaceSimulationServiceTest {
    private final RaceSimulationService service = new RaceSimulationService(
            mock(VehicleTrimRepository.class), mock(VehicleSpecMapper.class));

    @Test
    void rejectsSameVehicleInBothLanesBeforeDatabaseLookup() {
        var request = new RaceRequest(1L, 1L,
                new RaceRequest.RaceConfiguration(RaceRequest.GoalType.DISTANCE,
                        402.336, null, 0.0, RoadSurface.DRY_ASPHALT, null));

        assertThatThrownBy(() -> service.simulate(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different vehicle");
    }
}

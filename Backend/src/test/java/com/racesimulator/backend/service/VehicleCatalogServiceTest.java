package com.racesimulator.backend.service;

import com.racesimulator.backend.entity.VehicleMake;
import com.racesimulator.backend.repository.VehicleMakeRepository;
import com.racesimulator.backend.repository.VehicleModelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleCatalogServiceTest {

    @Mock
    private VehicleMakeRepository makeRepository;

    @Mock
    private VehicleModelRepository modelRepository;

    @InjectMocks
    private VehicleCatalogService service;

    @Test
    void returnsMakesFromRepository() {
        VehicleMake ford = mock(VehicleMake.class);
        when(ford.getId()).thenReturn(1L);
        when(ford.getName()).thenReturn("Ford");
        when(ford.getCountry()).thenReturn("United States");
        when(makeRepository.findAllByOrderByNameAsc()).thenReturn(List.of(ford));

        var result = service.findMakes();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(1L);
        assertThat(result.getFirst().name()).isEqualTo("Ford");
        assertThat(result.getFirst().country()).isEqualTo("United States");
    }
}

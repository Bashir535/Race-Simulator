package com.racesimulator.backend.service;

import com.racesimulator.backend.dto.VehicleMakeResponse;
import com.racesimulator.backend.dto.VehicleModelResponse;
import com.racesimulator.backend.repository.VehicleMakeRepository;
import com.racesimulator.backend.repository.VehicleModelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class VehicleCatalogService {

    private final VehicleMakeRepository makeRepository;
    private final VehicleModelRepository modelRepository;

    public VehicleCatalogService(
            VehicleMakeRepository makeRepository,
            VehicleModelRepository modelRepository) {
        this.makeRepository = makeRepository;
        this.modelRepository = modelRepository;
    }

    public List<VehicleMakeResponse> findMakes() {
        return makeRepository.findAllByOrderByNameAsc().stream()
                .map(make -> new VehicleMakeResponse(make.getId(), make.getName(), make.getCountry()))
                .toList();
    }

    public List<VehicleModelResponse> findModels(Long makeId) {
        return modelRepository.findAllByMake_IdOrderByNameAsc(makeId).stream()
                .map(model -> new VehicleModelResponse(
                        model.getId(),
                        model.getMake().getId(),
                        model.getName(),
                        model.getGeneration(),
                        model.getBodyStyle(),
                        model.getProductionStartYear(),
                        model.getProductionEndYear()))
                .toList();
    }
}

package com.racesimulator.backend.service;

import com.racesimulator.backend.dto.VehicleMakeResponse;
import com.racesimulator.backend.dto.VehicleModelResponse;
import com.racesimulator.backend.dto.VehicleTrimResponse;
import com.racesimulator.backend.dto.VehicleDetailResponse;
import com.racesimulator.backend.entity.VehicleTrim;
import com.racesimulator.backend.exception.ResourceNotFoundException;
import com.racesimulator.backend.repository.VehicleMakeRepository;
import com.racesimulator.backend.repository.VehicleModelRepository;
import com.racesimulator.backend.repository.VehicleTrimRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class VehicleCatalogService {

    private final VehicleMakeRepository makeRepository;
    private final VehicleModelRepository modelRepository;
    private final VehicleTrimRepository trimRepository;

    public VehicleCatalogService(
            VehicleMakeRepository makeRepository,
            VehicleModelRepository modelRepository,
            VehicleTrimRepository trimRepository) {
        this.makeRepository = makeRepository;
        this.modelRepository = modelRepository;
        this.trimRepository = trimRepository;
    }

    public List<VehicleMakeResponse> findMakes() {
        return makeRepository.findAllByOrderByNameAsc().stream()
                .map(make -> new VehicleMakeResponse(make.getId(), make.getName(), make.getCountry()))
                .toList();
    }

    public List<Short> findYears() {
        return trimRepository.findDistinctModelYears();
    }

    public List<VehicleMakeResponse> findMakes(short year) {
        return makeRepository.findAllByIdInOrderByNameAsc(trimRepository.findMakeIdsForYear(year)).stream()
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

    public List<VehicleModelResponse> findModels(Long makeId, short year) {
        return modelRepository.findAvailableByMakeAndYear(makeId, year).stream()
                .map(model -> new VehicleModelResponse(model.getId(), model.getMake().getId(),
                        model.getName(), model.getGeneration(), model.getBodyStyle(),
                        model.getProductionStartYear(), model.getProductionEndYear()))
                .toList();
    }

    public List<VehicleTrimResponse> findTrims(Long modelId, short year) {
        return trimRepository.findAllByGeneration_Model_IdAndModelYearOrderByTrimNameAsc(modelId, year)
                .stream().map(this::summary).toList();
    }

    public List<VehicleTrimResponse> findPopular() {
        return trimRepository.findAllByPopularTrueOrderByModelYearDesc().stream()
                .map(this::summary).toList();
    }

    public VehicleDetailResponse findVehicle(Long id) {
        VehicleTrim trim = findDetailedTrim(id);
        var engine = trim.getEngine();
        var transmission = trim.getTransmission();
        var specification = trim.getSpecification();
        return new VehicleDetailResponse(
                summary(trim),
                trim.getGeneration().getBodyStyle(),
                trim.getOriginalMsrpUsd(),
                trim.getTireDescription(),
                new VehicleDetailResponse.FuelEfficiency(
                        trim.getCityMpg(), trim.getHighwayMpg(), trim.getCombinedMpg()),
                new VehicleDetailResponse.Engine(
                        engine.getName(), engine.getDisplacementLiters(), engine.getConfiguration(),
                        engine.getAspiration(), engine.getFuelType(), engine.getRatedHorsepower(),
                        engine.getPeakHorsepowerRpm(), engine.getRatedTorqueNm(), engine.getPeakTorqueRpm(),
                        engine.getIdleRpm(), engine.getShiftRpm(), engine.getRedlineRpm(),
                        engine.getTorqueCurvePoints().stream()
                                .map(point -> new VehicleDetailResponse.TorquePoint(
                                        point.getRpm(), point.getTorqueNm()))
                                .toList()),
                new VehicleDetailResponse.Transmission(
                        transmission.getName(), transmission.getTransmissionType(),
                        transmission.getNumberOfGears(), transmission.getFinalDriveRatio(),
                        transmission.getShiftDurationSeconds(), transmission.getDrivetrainEfficiency(),
                        transmission.getGearRatios().stream().map(ratio -> ratio.getRatio()).toList()),
                new VehicleDetailResponse.SimulationSpecification(
                        specification.getMassKg(), specification.getStaticFrontWeightFraction(),
                        specification.getWheelbaseMeters(), specification.getCenterOfGravityHeightMeters(),
                        specification.getWheelRadiusMeters(), specification.getDragCoefficient(),
                        specification.getFrontalAreaSquareMeters(),
                        specification.getRollingResistanceCoefficient(),
                        specification.getTireFrictionCoefficient(), specification.getLaunchRpm(),
                        specification.getLaunchEngagementDurationSeconds(),
                        specification.getInitialTorqueTransferFraction(),
                        specification.isLaunchControlEnabled()),
                trim.getPublishedPerformance().stream()
                        .map(performance -> new VehicleDetailResponse.PublishedPerformance(
                                performance.getZeroToSixtySeconds(), performance.getQuarterMileSeconds(),
                                performance.getQuarterMileTrapSpeedMph(), performance.getRolloutSeconds(),
                                performance.getSourceDescription()))
                        .toList(),
                "DEVELOPMENT_FIXTURE");
    }

    public VehicleTrim findDetailedTrim(Long id) {
        return trimRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle configuration " + id + " was not found"));
    }

    private VehicleTrimResponse summary(VehicleTrim trim) {
        var model = trim.getGeneration().getModel();
        return new VehicleTrimResponse(
                trim.getId(), trim.getModelYear(), model.getMake().getName(), model.getName(),
                trim.getGeneration().getCode(), trim.getTrimName(), trim.getDrivetrain(),
                trim.getTransmission().getName(), trim.getEngine().getRatedHorsepower(),
                trim.getEngine().getRatedTorqueNm(), trim.getSpecification().getMassKg(),
                trim.getImageUrl(), trim.isPopular());
    }
}

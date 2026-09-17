package com.racesimulator.backend.service;

import com.racesimulator.backend.dto.RaceRequest;
import com.racesimulator.backend.dto.RaceResponse;
import com.racesimulator.backend.entity.VehicleTrim;
import com.racesimulator.backend.repository.VehicleTrimRepository;
import com.racesimulator.backend.exception.ResourceNotFoundException;
import com.racesimulator.engine.RaceSimulator;
import com.racesimulator.engine.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RaceSimulationService {
    public static final String SIMULATION_VERSION = "0.1.0";
    private final VehicleTrimRepository trimRepository;
    private final VehicleSpecMapper vehicleSpecMapper;

    public RaceSimulationService(VehicleTrimRepository trimRepository, VehicleSpecMapper vehicleSpecMapper) {
        this.trimRepository = trimRepository;
        this.vehicleSpecMapper = vehicleSpecMapper;
    }

    public RaceResponse simulate(RaceRequest request) {
        if (request.vehicleAId().equals(request.vehicleBId())) {
            throw new IllegalArgumentException("Choose two different vehicle configurations");
        }
        VehicleTrim trimA = load(request.vehicleAId());
        VehicleTrim trimB = load(request.vehicleBId());
        RaceConfig config = toConfig(request.race());
        var result = new RaceSimulator().simulate(
                vehicleSpecMapper.toEngineSpec(trimA), vehicleSpecMapper.toEngineSpec(trimB), config);
        return toResponse(request.vehicleAId(), request.vehicleBId(), result);
    }

    private VehicleTrim load(Long id) {
        return trimRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle configuration " + id + " was not found"));
    }

    private RaceConfig toConfig(RaceRequest.RaceConfiguration request) {
        RaceGoal goal = switch (request.goalType()) {
            case DISTANCE -> new DistanceRaceGoal(required(request.distanceMeters(), "distanceMeters"));
            case SPEED -> new SpeedRaceGoal(required(
                    request.targetSpeedMetersPerSecond(), "targetSpeedMetersPerSecond"));
        };
        var environmentRequest = request.environment();
        EnvironmentConditions environment = environmentRequest == null
                ? EnvironmentConditions.STANDARD
                : new EnvironmentConditions(
                        environmentRequest.airTemperatureCelsius(), environmentRequest.airPressurePascals(),
                        environmentRequest.relativeHumidity(), environmentRequest.roadTemperatureCelsius(),
                        environmentRequest.roadGradePercent(), environmentRequest.headwindMetersPerSecond());
        return new RaceConfig(goal, request.startingSpeedMetersPerSecond(), request.roadSurface(),
                0.01, 60.0, environment);
    }

    private double required(Double value, String field) {
        if (value == null) throw new IllegalArgumentException(field + " is required for this goal type");
        return value;
    }

    private RaceResponse toResponse(Long vehicleAId, Long vehicleBId, RaceResult result) {
        RaceConfig config = result.config();
        Double distance = config.goal() instanceof DistanceRaceGoal goal ? goal.distanceMeters() : null;
        Double targetSpeed = config.goal() instanceof SpeedRaceGoal goal
                ? goal.targetSpeedMetersPerSecond() : null;
        var environment = config.environment();
        return new RaceResponse(
                SIMULATION_VERSION,
                new RaceResponse.Configuration(
                        config.goal() instanceof DistanceRaceGoal ? "DISTANCE" : "SPEED",
                        distance, targetSpeed, config.startingSpeedMetersPerSecond(),
                        config.roadSurface().name(), new RaceResponse.Environment(
                                environment.airTemperatureCelsius(), environment.airPressurePascals(),
                                environment.relativeHumidity(), environment.roadTemperatureCelsius(),
                                environment.roadGradePercent(), environment.headwindMetersPerSecond())),
                vehicleResult(vehicleAId, result.vehicleA()),
                vehicleResult(vehicleBId, result.vehicleB()),
                result.winner(), result.winningMarginSeconds(),
                new RaceResponse.Summary(
                        result.summary().winner(), result.summary().timeMarginSeconds(),
                        result.summary().distanceGapAtWinnerFinishMeters(),
                        result.summary().largestLeadVehicle(), result.summary().largestLeadMeters(),
                        result.summary().leadChanges().stream()
                                .map(change -> new RaceResponse.LeadChange(
                                        change.timeSeconds(), change.distanceMeters(), change.newLeader()))
                                .toList()),
                result.timeline().stream()
                        .map(frame -> new RaceResponse.Frame(frame.timeSeconds(),
                                telemetry(frame.vehicleA()), telemetry(frame.vehicleB())))
                        .toList());
    }

    private RaceResponse.VehicleResult vehicleResult(Long id, VehicleRaceResult result) {
        return new RaceResponse.VehicleResult(
                id, result.vehicle().name(), result.finishTimeSeconds(), result.finishSpeedMetersPerSecond(),
                result.milestones().stream().map(milestone -> new RaceResponse.Milestone(
                        milestone.name(), milestone.elapsedSeconds(), milestone.speedMetersPerSecond())).toList(),
                result.shiftEvents().stream().map(shift -> new RaceResponse.Shift(
                        shift.startTimeSeconds(), shift.endTimeSeconds(), shift.fromGear(), shift.toGear(),
                        shift.rpmBefore(), shift.rpmAfter())).toList());
    }

    private RaceResponse.VehicleTelemetry telemetry(VehicleState state) {
        return new RaceResponse.VehicleTelemetry(
                state.distanceMeters(), state.speedMetersPerSecond(),
                state.accelerationMetersPerSecondSquared(), state.engineRpm(), state.gear(),
                state.tractionLimited(), state.launchActive(), state.accelerationLimit().name());
    }
}

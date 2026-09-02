package com.racesimulator.engine;

import com.racesimulator.engine.model.AccelerationLimit;
import com.racesimulator.engine.model.EnvironmentConditions;
import com.racesimulator.engine.model.RaceConfig;
import com.racesimulator.engine.model.RaceResult;
import com.racesimulator.engine.model.RoadSurface;
import com.racesimulator.engine.validation.AggregateValidationReport;
import com.racesimulator.engine.validation.PerformanceBenchmark;
import com.racesimulator.engine.validation.PowerCurveValidation;
import com.racesimulator.engine.validation.SimulationValidator;
import com.racesimulator.engine.validation.ValidationReport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhysicsExpansionTest {
    private final RaceSimulator simulator = new RaceSimulator();
    private final SimulationValidator validator = new SimulationValidator();

    @Test
    void torqueCurvesReproduceRatedPeakPower() {
        assertPowerCurve(RealVehicleFixtures.mustangGtManual(), RealVehicleFixtures.mustangBenchmark());
        assertPowerCurve(RealVehicleFixtures.corvetteStingrayZ51(), RealVehicleFixtures.corvetteBenchmark());
        assertPowerCurve(RealVehicleFixtures.civicTypeRManual(), RealVehicleFixtures.civicTypeRBenchmark());
        assertPowerCurve(RealVehicleFixtures.golfRDualClutch(), RealVehicleFixtures.golfRBenchmark());
    }

    @Test
    void adverseEnvironmentChangesResultsThroughPhysicalInputs() {
        RaceConfig standard = RaceConfig.quarterMile(RoadSurface.DRY_ASPHALT);
        RaceConfig adverse = standard.withEnvironment(new EnvironmentConditions(
                38.0, 85_000.0, 0.70, 5.0, 2.0, 8.0));

        RaceResult baseline = simulator.simulate(
                RealVehicleFixtures.civicTypeRManual(),
                RealVehicleFixtures.golfRDualClutch(), standard);
        RaceResult difficult = simulator.simulate(
                RealVehicleFixtures.civicTypeRManual(),
                RealVehicleFixtures.golfRDualClutch(), adverse);

        assertTrue(difficult.vehicleA().finishTimeSeconds() > baseline.vehicleA().finishTimeSeconds());
        assertTrue(difficult.vehicleB().finishTimeSeconds() > baseline.vehicleB().finishTimeSeconds());
        assertTrue(difficult.timeline().get(1).vehicleA().airDensityKgPerCubicMeter()
                < baseline.timeline().get(1).vehicleA().airDensityKgPerCubicMeter());
        assertTrue(difficult.timeline().get(1).vehicleA().gradeResistanceNewtons() > 0.0);
    }

    @Test
    void telemetryAndAggregateAccuracyReportAreComplete() {
        RaceResult first = simulator.simulate(
                RealVehicleFixtures.mustangGtManual(),
                RealVehicleFixtures.corvetteStingrayZ51(),
                RaceConfig.quarterMile(RoadSurface.DRY_ASPHALT));
        RaceResult second = simulator.simulate(
                RealVehicleFixtures.civicTypeRManual(),
                RealVehicleFixtures.golfRDualClutch(),
                RaceConfig.quarterMile(RoadSurface.DRY_ASPHALT));
        List<ValidationReport> reports = List.of(
                validator.compare(first.vehicleA(), RealVehicleFixtures.mustangBenchmark()),
                validator.compare(first.vehicleB(), RealVehicleFixtures.corvetteBenchmark()),
                validator.compare(second.vehicleA(), RealVehicleFixtures.civicTypeRBenchmark()),
                validator.compare(second.vehicleB(), RealVehicleFixtures.golfRBenchmark()));

        AggregateValidationReport aggregate = validator.aggregate(reports);
        var frame = first.timeline().stream()
                .map(candidate -> candidate.vehicleA())
                .filter(state -> state.speedMetersPerSecond() > 1.0)
                .findFirst()
                .orElseThrow();

        assertEquals(4, aggregate.vehicleReports().size());
        assertTrue(aggregate.meanAbsolutePercentError() > 0.0);
        assertTrue(aggregate.worstAbsolutePercentError() >= aggregate.meanAbsolutePercentError());
        assertNotNull(aggregate.worstVehicle());
        assertTrue(frame.enginePowerWatts() > 0.0);
        assertTrue(frame.wheelTorqueNewtonMeters() > 0.0);
        assertTrue(frame.effectiveDrivetrainEfficiency() > 0.0);
        assertTrue(Double.isFinite(frame.netForceNewtons()));
        assertNotNull(frame.accelerationLimit());
        assertTrue(frame.accelerationLimit() == AccelerationLimit.TRACTION
                || frame.accelerationLimit() == AccelerationLimit.ENGINE_POWER);
        System.out.println(aggregate.asText());
    }

    private void assertPowerCurve(
            com.racesimulator.engine.model.VehicleSpec vehicle,
            PerformanceBenchmark benchmark) {
        RaceResult race = simulator.simulate(vehicle, vehicle,
                RaceConfig.quarterMile(RoadSurface.DRY_ASPHALT));
        PowerCurveValidation validation = validator.validatePowerCurve(race.vehicleA(), benchmark);
        assertTrue(validation.withinTolerance(2.0),
                () -> vehicle.name() + " power-curve error was " + validation.percentErrorAtRatedRpm());
        assertTrue(validation.curvePeakHorsepower() >= validation.curveHorsepowerAtRatedRpm() * 0.98);
    }
}

package com.racesimulator.engine;

import com.racesimulator.engine.model.Drivetrain;
import com.racesimulator.engine.model.LaunchProfile;
import com.racesimulator.engine.model.RaceConfig;
import com.racesimulator.engine.model.RaceResult;
import com.racesimulator.engine.model.RoadSurface;
import com.racesimulator.engine.model.TorqueCurve;
import com.racesimulator.engine.model.TorquePoint;
import com.racesimulator.engine.model.TransmissionType;
import com.racesimulator.engine.model.VehicleSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaceSimulatorTest {
    private final RaceSimulator simulator = new RaceSimulator();

    @Test
    void producesDeterministicResultsAndExpectedMilestones() {
        VehicleSpec vehicleA = vehicle("Vehicle A", 1_550.0, Drivetrain.AWD, 520.0);
        VehicleSpec vehicleB = vehicle("Vehicle B", 1_700.0, Drivetrain.RWD, 500.0);
        RaceConfig config = RaceConfig.quarterMile(RoadSurface.DRY_ASPHALT);

        RaceResult first = simulator.simulate(vehicleA, vehicleB, config);
        RaceResult second = simulator.simulate(vehicleA, vehicleB, config);

        assertEquals(first, second);
        assertEquals("Vehicle A", first.winner());
        assertEquals(first.winner(), first.summary().winner());
        assertEquals(first.winningMarginSeconds(), first.summary().timeMarginSeconds(), 1.0e-12);
        assertTrue(first.summary().distanceGapAtWinnerFinishMeters() >= 0.0);
        assertTrue(Double.isFinite(first.summary().largestLeadMeters()));
        assertFalse(first.timeline().isEmpty());
        assertFalse(first.vehicleA().shiftEvents().isEmpty());
        assertTrue(first.timeline().stream().anyMatch(frame -> frame.vehicleA().launchActive()));
        assertTrue(first.timeline().stream().anyMatch(frame -> frame.vehicleA().launchActive()
                && frame.vehicleA().torqueTransferFraction() < 1.0));
        assertTrue(first.timeline().stream()
                .allMatch(frame -> frame.vehicleA().requestedWheelForceNewtons()
                        >= frame.vehicleA().wheelForceNewtons()));
        assertTrue(first.vehicleA().shiftEvents().stream()
                .allMatch(shift -> shift.endTimeSeconds() > shift.startTimeSeconds()
                        && shift.toGear() == shift.fromGear() + 1
                        && shift.rpmAfter() < shift.rpmBefore()));
        assertEquals(List.of("0-60 mph", "1/8 mile", "Finish"),
                first.vehicleA().milestones().stream().map(milestone -> milestone.name()).toList());
        assertEquals(RaceConfig.QUARTER_MILE_METERS,
                first.config().distanceMeters(), 1.0e-9);
    }

    @Test
    void addedMassSlowsAnOtherwiseIdenticalVehicle() {
        VehicleSpec light = vehicle("Light", 1_450.0, Drivetrain.RWD, 500.0);
        VehicleSpec heavy = vehicle("Heavy", 1_950.0, Drivetrain.RWD, 500.0);

        RaceResult result = simulator.simulate(
                light,
                heavy,
                RaceConfig.quarterMile(RoadSurface.DRY_ASPHALT));

        assertEquals("Light", result.winner());
        assertTrue(result.vehicleA().finishTimeSeconds() < result.vehicleB().finishTimeSeconds());
    }

    @Test
    void reducedRoadGripSlowsATractionLimitedLaunch() {
        VehicleSpec vehicle = vehicle("AWD Test Vehicle", 1_650.0, Drivetrain.AWD, 700.0);
        RaceResult dry = simulator.simulate(
                vehicle,
                vehicle,
                RaceConfig.quarterMile(RoadSurface.DRY_ASPHALT));
        RaceResult wet = simulator.simulate(
                vehicle,
                vehicle,
                RaceConfig.quarterMile(RoadSurface.WET_ASPHALT));

        assertTrue(wet.vehicleA().finishTimeSeconds() > dry.vehicleA().finishTimeSeconds());
    }

    @Test
    void torqueCurveInterpolatesBetweenMeasuredPoints() {
        TorqueCurve curve = new TorqueCurve(List.of(
                new TorquePoint(1_000, 200),
                new TorquePoint(3_000, 400)));

        assertEquals(300.0, curve.torqueAt(2_000), 1.0e-9);
        assertEquals(200.0, curve.torqueAt(500), 1.0e-9);
        assertEquals(0.0, curve.torqueAt(3_500), 1.0e-9);
    }

    @Test
    void rollRaceSelectsAValidStartingGearAndFinishesAtTargetSpeed() {
        double startingSpeed = 60.0 * 0.44704;
        double targetSpeed = 100.0 * 0.44704;
        RaceResult result = simulator.simulate(
                vehicle("Vehicle A", 1_550.0, Drivetrain.AWD, 520.0),
                vehicle("Vehicle B", 1_700.0, Drivetrain.RWD, 500.0),
                RaceConfig.rollRace(startingSpeed, targetSpeed, RoadSurface.DRY_ASPHALT));

        assertEquals("Vehicle A", result.winner());
        assertTrue(result.timeline().getFirst().vehicleA().gear() > 1);
        assertTrue(result.timeline().stream().noneMatch(frame -> frame.vehicleA().launchActive()));
        assertEquals("Finish", result.vehicleA().milestones().getLast().name());
        assertEquals(targetSpeed,
                result.vehicleA().milestones().getLast().speedMetersPerSecond(), 1.0e-9);
    }

    @Test
    void rollRaceRequiresTargetSpeedAboveStartingSpeed() {
        assertThrows(IllegalArgumentException.class, () -> RaceConfig.rollRace(
                60.0 * 0.44704,
                40.0 * 0.44704,
                RoadSurface.DRY_ASPHALT));
    }

    private VehicleSpec vehicle(
            String name,
            double massKg,
            Drivetrain drivetrain,
            double peakTorqueNewtonMeters) {
        return new VehicleSpec(
                name,
                massKg,
                drivetrain,
                drivetrain == Drivetrain.FWD ? 0.60 : 0.54,
                2.72,
                0.55,
                drivetrain == Drivetrain.AWD ? 0.80 : 0.85,
                0.34,
                0.31,
                2.2,
                0.015,
                1.0,
                3.55,
                List.of(3.20, 2.10, 1.52, 1.17, 0.91, 0.74),
                TransmissionType.TORQUE_CONVERTER_AUTOMATIC,
                0.12,
                new LaunchProfile(2_500, 0.40, 0.55, true),
                900,
                6_500,
                7_000,
                new TorqueCurve(List.of(
                        new TorquePoint(900, peakTorqueNewtonMeters * 0.65),
                        new TorquePoint(2_500, peakTorqueNewtonMeters * 0.92),
                        new TorquePoint(4_000, peakTorqueNewtonMeters),
                        new TorquePoint(5_500, peakTorqueNewtonMeters * 0.95),
                        new TorquePoint(7_000, peakTorqueNewtonMeters * 0.75))));
    }
}

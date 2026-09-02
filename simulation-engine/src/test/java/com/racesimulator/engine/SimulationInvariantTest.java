package com.racesimulator.engine;

import com.racesimulator.engine.model.RaceConfig;
import com.racesimulator.engine.model.RaceFrame;
import com.racesimulator.engine.model.RaceResult;
import com.racesimulator.engine.model.RoadSurface;
import com.racesimulator.engine.model.VehicleSpec;
import com.racesimulator.engine.model.VehicleState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationInvariantTest {
    private static final double MPH_TO_METERS_PER_SECOND = 0.44704;
    private final RaceSimulator simulator = new RaceSimulator();

    @Test
    void quarterMileTimelineMaintainsPhysicalAndNumericalInvariants() {
        VehicleSpec vehicleA = RealVehicleFixtures.mustangGtManual();
        VehicleSpec vehicleB = RealVehicleFixtures.corvetteStingrayZ51();
        RaceResult result = simulator.simulate(
                vehicleA,
                vehicleB,
                RaceConfig.quarterMile(RoadSurface.DRY_ASPHALT));

        assertTimeline(result.timeline(), vehicleA, vehicleB);
    }

    @Test
    void rollRaceTimelineMaintainsPhysicalAndNumericalInvariants() {
        VehicleSpec vehicleA = RealVehicleFixtures.mustangGtManual();
        VehicleSpec vehicleB = RealVehicleFixtures.corvetteStingrayZ51();
        RaceResult result = simulator.simulate(
                vehicleA,
                vehicleB,
                RaceConfig.rollRace(
                        40.0 * MPH_TO_METERS_PER_SECOND,
                        120.0 * MPH_TO_METERS_PER_SECOND,
                        RoadSurface.DRY_ASPHALT));

        assertTimeline(result.timeline(), vehicleA, vehicleB);
    }

    private void assertTimeline(
            List<RaceFrame> timeline,
            VehicleSpec vehicleA,
            VehicleSpec vehicleB) {
        assertTrue(timeline.size() > 1);
        for (int index = 0; index < timeline.size(); index++) {
            RaceFrame frame = timeline.get(index);
            assertEquals(frame.timeSeconds(), frame.vehicleA().timeSeconds(), 1.0e-12);
            assertEquals(frame.timeSeconds(), frame.vehicleB().timeSeconds(), 1.0e-12);
            assertVehicleState(frame.vehicleA(), vehicleA);
            assertVehicleState(frame.vehicleB(), vehicleB);

            if (index > 0) {
                RaceFrame previous = timeline.get(index - 1);
                assertTrue(frame.timeSeconds() > previous.timeSeconds());
                assertTrue(frame.vehicleA().distanceMeters()
                        >= previous.vehicleA().distanceMeters());
                assertTrue(frame.vehicleB().distanceMeters()
                        >= previous.vehicleB().distanceMeters());
            }
        }
    }

    private void assertVehicleState(VehicleState state, VehicleSpec vehicle) {
        assertFinite(state.timeSeconds());
        assertFinite(state.distanceMeters());
        assertFinite(state.speedMetersPerSecond());
        assertFinite(state.accelerationMetersPerSecondSquared());
        assertFinite(state.engineRpm());
        assertFinite(state.engineTorqueNewtonMeters());
        assertFinite(state.requestedWheelForceNewtons());
        assertFinite(state.wheelForceNewtons());
        assertFinite(state.tractionLimitNewtons());
        assertFinite(state.wheelSlipRatio());
        assertFinite(state.torqueTransferFraction());

        assertTrue(state.timeSeconds() >= 0.0);
        assertTrue(state.distanceMeters() >= 0.0);
        assertTrue(state.speedMetersPerSecond() >= 0.0);
        assertTrue(state.engineRpm() >= vehicle.idleRpm());
        assertTrue(state.engineRpm() <= vehicle.redlineRpm());
        assertTrue(state.gear() >= 1 && state.gear() <= vehicle.gearRatios().size());
        assertTrue(state.requestedWheelForceNewtons() + 1.0e-9 >= state.wheelForceNewtons());
        assertTrue(state.tractionLimitNewtons() + 1.0e-9 >= state.wheelForceNewtons());
        assertTrue(state.wheelSlipRatio() >= 0.0 && state.wheelSlipRatio() <= 1.0);
        assertTrue(state.torqueTransferFraction() > 0.0 && state.torqueTransferFraction() <= 1.0);
    }

    private void assertFinite(double value) {
        assertTrue(Double.isFinite(value));
    }
}

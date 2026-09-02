package com.racesimulator.engine;

import com.racesimulator.engine.model.RaceConfig;
import com.racesimulator.engine.model.RaceResult;
import com.racesimulator.engine.model.RoadSurface;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NumericalConvergenceTest {
    private final RaceSimulator simulator = new RaceSimulator();

    @Test
    void standardTimeStepConvergesWithAFiveTimesFinerQuarterMileSimulation() {
        RaceConfig standard = RaceConfig.quarterMile(RoadSurface.DRY_ASPHALT);
        RaceConfig fine = new RaceConfig(
                standard.goal(),
                standard.startingSpeedMetersPerSecond(),
                standard.roadSurface(),
                0.002,
                standard.maximumDurationSeconds());

        RaceResult standardResult = simulator.simulate(
                RealVehicleFixtures.mustangGtManual(),
                RealVehicleFixtures.corvetteStingrayZ51(),
                standard);
        RaceResult fineResult = simulator.simulate(
                RealVehicleFixtures.mustangGtManual(),
                RealVehicleFixtures.corvetteStingrayZ51(),
                fine);

        assertClose(standardResult.vehicleA().finishTimeSeconds(),
                fineResult.vehicleA().finishTimeSeconds(), 0.02);
        assertClose(standardResult.vehicleB().finishTimeSeconds(),
                fineResult.vehicleB().finishTimeSeconds(), 0.02);
        assertClose(standardResult.vehicleA().finishSpeedMetersPerSecond(),
                fineResult.vehicleA().finishSpeedMetersPerSecond(), 0.15);
        assertClose(standardResult.vehicleB().finishSpeedMetersPerSecond(),
                fineResult.vehicleB().finishSpeedMetersPerSecond(), 0.15);
    }

    @Test
    void standardTimeStepConvergesWithAFiveTimesFinerRollRaceSimulation() {
        RaceConfig standard = RaceConfig.rollRace(
                40.0 * 0.44704,
                120.0 * 0.44704,
                RoadSurface.DRY_ASPHALT);
        RaceConfig fine = new RaceConfig(
                standard.goal(),
                standard.startingSpeedMetersPerSecond(),
                standard.roadSurface(),
                0.002,
                standard.maximumDurationSeconds());

        RaceResult standardResult = simulator.simulate(
                RealVehicleFixtures.mustangGtManual(),
                RealVehicleFixtures.corvetteStingrayZ51(),
                standard);
        RaceResult fineResult = simulator.simulate(
                RealVehicleFixtures.mustangGtManual(),
                RealVehicleFixtures.corvetteStingrayZ51(),
                fine);

        assertClose(standardResult.vehicleA().finishTimeSeconds(),
                fineResult.vehicleA().finishTimeSeconds(), 0.02);
        assertClose(standardResult.vehicleB().finishTimeSeconds(),
                fineResult.vehicleB().finishTimeSeconds(), 0.02);
    }

    private void assertClose(double standard, double fine, double tolerance) {
        assertEquals(fine, standard, tolerance);
    }
}

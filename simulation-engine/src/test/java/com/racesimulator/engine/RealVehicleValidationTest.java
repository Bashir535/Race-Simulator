package com.racesimulator.engine;

import com.racesimulator.engine.model.RaceConfig;
import com.racesimulator.engine.model.RaceResult;
import com.racesimulator.engine.model.RoadSurface;
import com.racesimulator.engine.validation.SimulationValidator;
import com.racesimulator.engine.validation.ValidationReport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealVehicleValidationTest {
    private final RaceSimulator simulator = new RaceSimulator();
    private final SimulationValidator validator = new SimulationValidator();

    @Test
    void reportsCurrentErrorAgainstPublishedInstrumentedTests() {
        RaceResult race = simulator.simulate(
                RealVehicleFixtures.mustangGtManual(),
                RealVehicleFixtures.corvetteStingrayZ51(),
                RaceConfig.quarterMile(RoadSurface.DRY_ASPHALT));

        ValidationReport mustangReport = validator.compare(
                race.vehicleA(),
                RealVehicleFixtures.mustangBenchmark());
        ValidationReport corvetteReport = validator.compare(
                race.vehicleB(),
                RealVehicleFixtures.corvetteBenchmark());

        System.out.println(mustangReport.asText());
        System.out.println(corvetteReport.asText());

        assertEquals(5, mustangReport.comparisons().size());
        assertEquals(5, corvetteReport.comparisons().size());
        assertTrue(mustangReport.comparisons().stream()
                .allMatch(comparison -> Double.isFinite(comparison.percentError())));
        assertTrue(corvetteReport.comparisons().stream()
                .allMatch(comparison -> Double.isFinite(comparison.percentError())));
        assertTrue(race.timeline().stream()
                .anyMatch(frame -> frame.vehicleA().wheelSlipRatio() > 0.0));
        assertTrue(race.timeline().stream()
                .filter(frame -> frame.vehicleB().launchActive())
                .allMatch(frame -> frame.vehicleB().wheelSlipRatio() <= 0.10));

        // Accuracy is deliberately not a passing condition yet. This test exposes
        // baseline error so model improvements can be measured instead of guessed.
        assertFalse(mustangReport.allWithinTolerance(1.0));
        assertFalse(corvetteReport.allWithinTolerance(1.0));
    }

    @Test
    void reportsCurrentErrorForFwdManualAndAwdDualClutchVehicles() {
        RaceResult race = simulator.simulate(
                RealVehicleFixtures.civicTypeRManual(),
                RealVehicleFixtures.golfRDualClutch(),
                RaceConfig.quarterMile(RoadSurface.DRY_ASPHALT));

        ValidationReport civicReport = validator.compare(
                race.vehicleA(),
                RealVehicleFixtures.civicTypeRBenchmark());
        ValidationReport golfReport = validator.compare(
                race.vehicleB(),
                RealVehicleFixtures.golfRBenchmark());

        System.out.println(civicReport.asText());
        System.out.println(golfReport.asText());

        assertValidReport(civicReport);
        assertValidReport(golfReport);
        assertTrue(race.timeline().stream()
                .anyMatch(frame -> frame.vehicleA().wheelSlipRatio() > 0.0));
        assertTrue(race.timeline().stream()
                .filter(frame -> frame.vehicleB().launchActive())
                .allMatch(frame -> frame.vehicleB().wheelSlipRatio() <= 0.10));
    }

    @Test
    void golfRRollRaceUsesSequentialEffectiveRatiosAndRealisticPerformance() {
        var golf = RealVehicleFixtures.golfRDualClutch();
        for (int index = 1; index < golf.gearRatios().size(); index++) {
            assertTrue(golf.gearRatios().get(index) < golf.gearRatios().get(index - 1),
                    "Effective DSG ratios must decrease with each higher gear");
        }

        RaceResult race = simulator.simulate(
                golf,
                golf,
                RaceConfig.rollRace(40.0 * 0.44704, 120.0 * 0.44704,
                        RoadSurface.DRY_ASPHALT));

        System.out.printf("Golf R 40-120 mph validation: %.2f s%n",
                race.vehicleA().finishTimeSeconds());
        assertTrue(race.vehicleA().finishTimeSeconds() >= 9.0);
        assertTrue(race.vehicleA().finishTimeSeconds() <= 15.0);
        assertTrue(race.timeline().getFirst().vehicleA().engineRpm() < golf.shiftRpm());
    }

    private static void assertValidReport(ValidationReport report) {
        assertEquals(5, report.comparisons().size());
        assertTrue(report.comparisons().stream()
                .allMatch(comparison -> Double.isFinite(comparison.percentError())));
    }
}

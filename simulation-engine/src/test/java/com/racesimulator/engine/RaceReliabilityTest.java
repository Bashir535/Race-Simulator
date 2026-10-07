package com.racesimulator.engine;

import com.racesimulator.engine.model.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RaceReliabilityTest {
    private final RaceSimulator simulator = new RaceSimulator();
    private final List<VehicleSpec> vehicles = List.of(RealVehicleFixtures.mustangGtManual(),
            RealVehicleFixtures.corvetteStingrayZ51(), RealVehicleFixtures.civicTypeRManual(), RealVehicleFixtures.golfRDualClutch());

    @Test
    void everySupportedStartDistanceAndSurfaceProducesFiniteMonotonicTelemetry() {
        for (int pair = 0; pair < vehicles.size(); pair += 2) {
            for (int mph = 0; mph <= 70; mph += 10) {
                for (double distance : new double[]{201.168, 402.336}) {
                    for (RoadSurface surface : RoadSurface.values()) {
                        var race = simulator.simulate(vehicles.get(pair), vehicles.get(pair + 1),
                                RaceConfig.distanceRace(distance, mph * 0.44704, surface));
                        assertTrue(Double.isFinite(race.vehicleA().finishTimeSeconds()));
                        assertTrue(Double.isFinite(race.vehicleB().finishTimeSeconds()));
                        double lastA = 0, lastB = 0, lastTime = -1;
                        for (var frame : race.timeline()) {
                            assertTrue(frame.timeSeconds() > lastTime);
                            for (var state : List.of(frame.vehicleA(), frame.vehicleB())) {
                                assertTrue(Double.isFinite(state.engineRpm()));
                                assertTrue(Double.isFinite(state.speedMetersPerSecond()));
                                assertTrue(Double.isFinite(state.accelerationMetersPerSecondSquared()));
                                assertTrue(state.speedMetersPerSecond() >= 0);
                                assertTrue(state.gear() >= 1);
                            }
                            assertTrue(frame.vehicleA().distanceMeters() >= lastA);
                            assertTrue(frame.vehicleB().distanceMeters() >= lastB);
                            lastA = frame.vehicleA().distanceMeters(); lastB = frame.vehicleB().distanceMeters();
                            lastTime = frame.timeSeconds();
                        }
                        assertTrue(lastA >= distance); assertTrue(lastB >= distance);
                        if (mph > 0) {
                            assertTrue(race.vehicleA().milestones().stream().noneMatch(m -> m.name().equals("0-60 mph")));
                            assertTrue(race.vehicleB().milestones().stream().noneMatch(m -> m.name().equals("0-60 mph")));
                        }
                    }
                }
            }
        }
    }

    @Test
    void halvingTimestepKeepsFinishTimesWithinFiftyMilliseconds() {
        for (var vehicle : vehicles) {
            for (double start : new double[]{0, 60 * 0.44704}) {
                var config = RaceConfig.distanceRace(402.336, start, RoadSurface.DRY_ASPHALT);
                var finer = new RaceConfig(config.goal(), start, config.roadSurface(), 0.005, 60, config.environment());
                var baseline = simulator.simulate(vehicle, vehicle, config);
                var fine = simulator.simulate(vehicle, vehicle, finer);
                assertEquals(baseline.vehicleA().finishTimeSeconds(), fine.vehicleA().finishTimeSeconds(), 0.05, vehicle.name());
                assertEquals(baseline.timeline(), simulator.simulate(vehicle, vehicle, config).timeline());
            }
        }
    }

    @Test
    void modificationsAreBoundedImmutableAndKeepPowerConsistent() {
        var stock = vehicles.getFirst();
        var modified = new VehicleModifications(1.2, 100).apply(stock);
        assertEquals(stock.massKg() - 100, modified.massKg());
        assertEquals(stock.torqueCurve().powerWattsAt(4000) * 1.2, modified.torqueCurve().powerWattsAt(4000), 0.00001);
        assertEquals(vehicles.getFirst().massKg(), stock.massKg());
        assertThrows(IllegalArgumentException.class, () -> new VehicleModifications(Double.NaN, 0));
        assertThrows(IllegalArgumentException.class, () -> new VehicleModifications(1, stock.massKg()).apply(stock));
        assertThrows(IllegalArgumentException.class, () -> new VehicleModifications(1.31, 0));
    }

    @Test
    void peakPowerIncludesInteriorOfDescendingTorqueSegment() {
        var curve = new TorqueCurve(List.of(new TorquePoint(1000, 300), new TorquePoint(5000, 100)));
        assertEquals(curve.horsepowerAt(3500), curve.peakHorsepower(), 0.000001);
        assertTrue(curve.peakHorsepower() > curve.horsepowerAt(5000));
    }
}

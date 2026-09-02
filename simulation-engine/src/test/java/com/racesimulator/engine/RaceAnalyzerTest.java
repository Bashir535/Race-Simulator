package com.racesimulator.engine;

import com.racesimulator.engine.model.LeadChange;
import com.racesimulator.engine.model.RaceFrame;
import com.racesimulator.engine.model.RaceSummary;
import com.racesimulator.engine.model.VehicleRaceResult;
import com.racesimulator.engine.model.VehicleSpec;
import com.racesimulator.engine.model.VehicleState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RaceAnalyzerTest {
    @Test
    void interpolatesAnOvertakeBetweenTimelineFrames() {
        VehicleSpec vehicleA = RealVehicleFixtures.mustangGtManual();
        VehicleSpec vehicleB = RealVehicleFixtures.corvetteStingrayZ51();
        List<RaceFrame> timeline = List.of(
                new RaceFrame(0.0, state(0.0, 0.0), state(0.0, 0.0)),
                new RaceFrame(1.0, state(1.0, 10.0), state(1.0, 8.0)),
                new RaceFrame(2.0, state(2.0, 20.0), state(2.0, 21.0)),
                new RaceFrame(3.0, state(3.0, 30.0), state(3.0, 32.0)));
        VehicleRaceResult resultA = new VehicleRaceResult(
                vehicleA, 3.0, 50.0, List.of(), List.of());
        VehicleRaceResult resultB = new VehicleRaceResult(
                vehicleB, 2.0, 50.0, List.of(), List.of());

        RaceSummary summary = new RaceAnalyzer().analyze(resultA, resultB, timeline);

        assertEquals(vehicleB.name(), summary.winner());
        assertEquals(1.0, summary.distanceGapAtWinnerFinishMeters(), 1.0e-9);
        assertEquals(vehicleA.name(), summary.largestLeadVehicle());
        assertEquals(2.0, summary.largestLeadMeters(), 1.0e-9);
        assertEquals(1, summary.leadChanges().size());
        LeadChange overtake = summary.leadChanges().getFirst();
        assertEquals(vehicleB.name(), overtake.newLeader());
        assertEquals(5.0 / 3.0, overtake.timeSeconds(), 1.0e-9);
        assertEquals(50.0 / 3.0, overtake.distanceMeters(), 1.0e-9);
    }

    private VehicleState state(double time, double distance) {
        return new VehicleState(
                time,
                distance,
                10.0,
                0.0,
                2_000.0,
                1,
                300.0,
                1_000.0,
                1_000.0,
                2_000.0,
                false,
                0.0,
                false,
                1.0,
                50.0,
                100.0);
    }
}

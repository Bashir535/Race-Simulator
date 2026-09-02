package com.racesimulator.engine;

import com.racesimulator.engine.model.LeadChange;
import com.racesimulator.engine.model.RaceFrame;
import com.racesimulator.engine.model.RaceSummary;
import com.racesimulator.engine.model.VehicleRaceResult;

import java.util.ArrayList;
import java.util.List;

/** Derives race-level facts from authoritative simulation telemetry. */
public final class RaceAnalyzer {
    private static final double LEAD_EPSILON_METERS = 1.0e-6;

    public RaceSummary analyze(
            VehicleRaceResult vehicleA,
            VehicleRaceResult vehicleB,
            List<RaceFrame> timeline) {
        if (timeline == null || timeline.isEmpty()) {
            throw new IllegalArgumentException("Timeline is required");
        }

        double difference = vehicleA.finishTimeSeconds() - vehicleB.finishTimeSeconds();
        boolean tie = Math.abs(difference) < 1.0e-9;
        boolean aWon = difference < 0.0;
        String winner = tie ? "Tie" : aWon ? vehicleA.vehicle().name() : vehicleB.vehicle().name();
        double winnerFinishTime = Math.min(
                vehicleA.finishTimeSeconds(),
                vehicleB.finishTimeSeconds());

        double aDistanceAtFinish = distanceAt(timeline, winnerFinishTime, true);
        double bDistanceAtFinish = distanceAt(timeline, winnerFinishTime, false);
        double winnerDistanceGap = tie
                ? 0.0
                : aWon
                        ? aDistanceAtFinish - bDistanceAtFinish
                        : bDistanceAtFinish - aDistanceAtFinish;

        List<LeadChange> leadChanges = new ArrayList<>();
        String establishedLeader = null;
        String largestLeadVehicle = "Tie";
        double largestLead = 0.0;
        RaceFrame previous = timeline.getFirst();

        for (RaceFrame current : timeline) {
            if (current.timeSeconds() > winnerFinishTime + 1.0e-12) {
                break;
            }

            double gap = current.vehicleA().distanceMeters() - current.vehicleB().distanceMeters();
            String leader = leaderForGap(
                    gap,
                    vehicleA.vehicle().name(),
                    vehicleB.vehicle().name());
            if (Math.abs(gap) > largestLead) {
                largestLead = Math.abs(gap);
                largestLeadVehicle = leader;
            }

            if (leader != null && establishedLeader == null) {
                establishedLeader = leader;
            } else if (leader != null && !leader.equals(establishedLeader)) {
                double previousGap = previous.vehicleA().distanceMeters()
                        - previous.vehicleB().distanceMeters();
                double fraction = crossingFraction(previousGap, gap);
                double crossingTime = interpolate(
                        previous.timeSeconds(), current.timeSeconds(), fraction);
                double aDistance = interpolate(
                        previous.vehicleA().distanceMeters(),
                        current.vehicleA().distanceMeters(),
                        fraction);
                double bDistance = interpolate(
                        previous.vehicleB().distanceMeters(),
                        current.vehicleB().distanceMeters(),
                        fraction);
                leadChanges.add(new LeadChange(
                        crossingTime,
                        (aDistance + bDistance) / 2.0,
                        leader));
                establishedLeader = leader;
            }
            previous = current;
        }

        return new RaceSummary(
                winner,
                Math.abs(difference),
                winnerDistanceGap,
                largestLeadVehicle == null ? "Tie" : largestLeadVehicle,
                largestLead,
                leadChanges);
    }

    private double distanceAt(List<RaceFrame> timeline, double time, boolean vehicleA) {
        RaceFrame previous = timeline.getFirst();
        for (RaceFrame current : timeline) {
            if (current.timeSeconds() >= time) {
                double fraction = current.timeSeconds() == previous.timeSeconds()
                        ? 0.0
                        : (time - previous.timeSeconds())
                                / (current.timeSeconds() - previous.timeSeconds());
                double previousDistance = vehicleA
                        ? previous.vehicleA().distanceMeters()
                        : previous.vehicleB().distanceMeters();
                double currentDistance = vehicleA
                        ? current.vehicleA().distanceMeters()
                        : current.vehicleB().distanceMeters();
                return interpolate(previousDistance, currentDistance, fraction);
            }
            previous = current;
        }
        return vehicleA
                ? timeline.getLast().vehicleA().distanceMeters()
                : timeline.getLast().vehicleB().distanceMeters();
    }

    private String leaderForGap(double gap, String vehicleA, String vehicleB) {
        if (Math.abs(gap) <= LEAD_EPSILON_METERS) {
            return null;
        }
        return gap > 0.0 ? vehicleA : vehicleB;
    }

    private double crossingFraction(double previousGap, double currentGap) {
        double change = currentGap - previousGap;
        if (Math.abs(change) < 1.0e-12) {
            return 0.0;
        }
        return Math.clamp(-previousGap / change, 0.0, 1.0);
    }

    private double interpolate(double start, double end, double fraction) {
        return start + (end - start) * fraction;
    }
}

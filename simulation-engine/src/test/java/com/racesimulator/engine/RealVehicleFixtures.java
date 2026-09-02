package com.racesimulator.engine;

import com.racesimulator.engine.model.Drivetrain;
import com.racesimulator.engine.model.LaunchProfile;
import com.racesimulator.engine.model.TorqueCurve;
import com.racesimulator.engine.model.TorquePoint;
import com.racesimulator.engine.model.TransmissionType;
import com.racesimulator.engine.model.VehicleSpec;
import com.racesimulator.engine.validation.PerformanceBenchmark;

import java.util.List;

/**
 * Initial validation fixtures. See REAL_VEHICLE_DATA.md for field-level
 * provenance and all assumptions. These are calibration inputs, not canonical
 * production vehicle records.
 */
final class RealVehicleFixtures {
    private static final double POUNDS_TO_KILOGRAMS = 0.45359237;
    private static final double MPH_TO_METERS_PER_SECOND = 0.44704;
    private static final double POUND_FEET_TO_NEWTON_METERS = 1.3558179483;

    private RealVehicleFixtures() {
    }

    static VehicleSpec mustangGtManual() {
        return new VehicleSpec(
                "2024 Ford Mustang GT Performance Package (6MT)",
                3_947 * POUNDS_TO_KILOGRAMS,
                Drivetrain.RWD,
                0.55,
                107.0 * 0.0254,
                0.55,
                0.88,
                tireRadiusMeters(275, 40, 19),
                0.377,
                2.20,
                0.015,
                1.15,
                3.73,
                List.of(3.237, 2.104, 1.422, 1.000, 0.814, 0.622),
                TransmissionType.MANUAL,
                0.20,
                new LaunchProfile(3_500, 0.45, 0.55, false),
                750,
                7_250,
                7_500,
                torqueCurvePoundFeet(List.of(
                        new TorquePoint(750, 220),
                        new TorquePoint(2_000, 325),
                        new TorquePoint(3_500, 390),
                        new TorquePoint(4_900, 418),
                        new TorquePoint(6_000, 405),
                        new TorquePoint(7_250, 352),
                        new TorquePoint(7_500, 330))));
    }

    static PerformanceBenchmark mustangBenchmark() {
        return new PerformanceBenchmark(
                "2024 Ford Mustang GT Performance Package (6MT)",
                4.2,
                12.5,
                114 * MPH_TO_METERS_PER_SECOND,
                0.3,
                "Car and Driver instrumented test; results omit 0.3-second rollout",
                486,
                7_250);
    }

    static VehicleSpec corvetteStingrayZ51() {
        return new VehicleSpec(
                "2020 Chevrolet Corvette Stingray Z51 (8DCT)",
                3_647 * POUNDS_TO_KILOGRAMS,
                Drivetrain.RWD,
                0.40,
                107.2 * 0.0254,
                0.48,
                0.90,
                tireRadiusMeters(305, 30, 20),
                0.322,
                2.075,
                0.015,
                1.20,
                5.17,
                List.of(2.905, 1.759, 1.220, 0.878, 0.653, 0.508, 0.397, 0.329),
                TransmissionType.DUAL_CLUTCH,
                0.08,
                new LaunchProfile(3_500, 0.35, 0.55, true),
                650,
                6_450,
                6_500,
                torqueCurvePoundFeet(List.of(
                        new TorquePoint(650, 300),
                        new TorquePoint(2_000, 410),
                        new TorquePoint(3_500, 455),
                        new TorquePoint(5_150, 470),
                        new TorquePoint(6_000, 440),
                        new TorquePoint(6_450, 403),
                        new TorquePoint(6_500, 395))));
    }

    static PerformanceBenchmark corvetteBenchmark() {
        return new PerformanceBenchmark(
                "2020 Chevrolet Corvette Stingray Z51 (8DCT)",
                2.8,
                11.2,
                122 * MPH_TO_METERS_PER_SECOND,
                0.2,
                "Car and Driver instrumented test; results omit 0.2-second rollout",
                495,
                6_450);
    }

    static VehicleSpec civicTypeRManual() {
        return new VehicleSpec(
                "2023 Honda Civic Type R (6MT)",
                3_183 * POUNDS_TO_KILOGRAMS,
                Drivetrain.FWD,
                0.62,
                107.7 * 0.0254,
                0.55,
                0.88,
                tireRadiusMeters(265, 30, 19),
                0.34,
                2.20,
                0.015,
                1.15,
                3.842,
                List.of(3.625, 2.115, 1.529, 1.125, 0.911, 0.735),
                TransmissionType.MANUAL,
                0.20,
                new LaunchProfile(3_000, 0.50, 0.45, false),
                800,
                6_900,
                7_000,
                torqueCurvePoundFeet(List.of(
                        new TorquePoint(800, 125),
                        new TorquePoint(1_500, 210),
                        new TorquePoint(2_600, 310),
                        new TorquePoint(4_000, 310),
                        new TorquePoint(5_000, 290),
                        new TorquePoint(6_500, 255),
                        new TorquePoint(7_000, 220))));
    }

    static PerformanceBenchmark civicTypeRBenchmark() {
        return new PerformanceBenchmark(
                "2023 Honda Civic Type R (6MT)",
                4.9,
                13.5,
                106 * MPH_TO_METERS_PER_SECOND,
                0.3,
                "Car and Driver instrumented test; results omit 0.3-second rollout",
                315,
                6_500);
    }

    static VehicleSpec golfRDualClutch() {
        // This DSG uses two final drives. Folding each final-drive value into
        // its corresponding gear ratio preserves the effective wheel ratio.
        return new VehicleSpec(
                "2022 Volkswagen Golf R Euro-spec (7DSG)",
                3_360 * POUNDS_TO_KILOGRAMS,
                Drivetrain.AWD,
                0.60,
                103.5 * 0.0254,
                0.55,
                0.82,
                tireRadiusMeters(235, 35, 19),
                0.34,
                2.22,
                0.015,
                1.15,
                1.0,
                List.of(
                        3.19 * 4.47,
                        2.75 * 4.47,
                        1.90 * 4.47,
                        1.04 * 4.47,
                        0.79 * 3.30,
                        0.86 * 3.30,
                        0.66 * 3.30),
                TransmissionType.DUAL_CLUTCH,
                0.08,
                new LaunchProfile(3_000, 0.35, 0.55, true),
                800,
                6_500,
                6_800,
                torqueCurvePoundFeet(List.of(
                        new TorquePoint(800, 145),
                        new TorquePoint(1_500, 260),
                        new TorquePoint(1_900, 310),
                        new TorquePoint(4_500, 310),
                        new TorquePoint(5_900, 280),
                        new TorquePoint(6_500, 245),
                        new TorquePoint(6_800, 220))));
    }

    static PerformanceBenchmark golfRBenchmark() {
        return new PerformanceBenchmark(
                "2022 Volkswagen Golf R Euro-spec (7DSG)",
                3.9,
                12.5,
                111 * MPH_TO_METERS_PER_SECOND,
                0.2,
                "Car and Driver instrumented test; results omit 0.2-second rollout",
                315,
                5_900);
    }

    private static TorqueCurve torqueCurvePoundFeet(List<TorquePoint> poundFeetPoints) {
        return new TorqueCurve(poundFeetPoints.stream()
                .map(point -> new TorquePoint(
                        point.rpm(),
                        point.torqueNewtonMeters() * POUND_FEET_TO_NEWTON_METERS))
                .toList());
    }

    private static double tireRadiusMeters(
            double widthMillimeters,
            double aspectRatioPercent,
            double wheelDiameterInches) {
        double sidewallMeters = (widthMillimeters / 1_000.0) * (aspectRatioPercent / 100.0);
        return ((wheelDiameterInches * 0.0254) + (2.0 * sidewallMeters)) / 2.0;
    }
}

package com.racesimulator.engine;

import com.racesimulator.engine.model.DistanceRaceGoal;
import com.racesimulator.engine.model.AccelerationLimit;
import com.racesimulator.engine.model.EnvironmentConditions;
import com.racesimulator.engine.model.Milestone;
import com.racesimulator.engine.model.RaceConfig;
import com.racesimulator.engine.model.RaceFrame;
import com.racesimulator.engine.model.RaceResult;
import com.racesimulator.engine.model.ShiftEvent;
import com.racesimulator.engine.model.SpeedRaceGoal;
import com.racesimulator.engine.model.VehicleRaceResult;
import com.racesimulator.engine.model.VehicleSpec;
import com.racesimulator.engine.model.VehicleState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Deterministic, fixed-time-step straight-line vehicle race simulator. */
public final class RaceSimulator {
    private static final double STANDARD_AIR_DENSITY_KG_PER_CUBIC_METER = 1.225;
    private static final double GRAVITY_METERS_PER_SECOND_SQUARED = 9.80665;
    private static final double SIXTY_MPH_METERS_PER_SECOND = 26.8224;

    public RaceResult simulate(VehicleSpec vehicleA, VehicleSpec vehicleB, RaceConfig config) {
        Objects.requireNonNull(vehicleA, "Vehicle A is required");
        Objects.requireNonNull(vehicleB, "Vehicle B is required");
        Objects.requireNonNull(config, "Race configuration is required");

        MutableRun runA = new MutableRun(vehicleA, config.startingSpeedMetersPerSecond());
        MutableRun runB = new MutableRun(vehicleB, config.startingSpeedMetersPerSecond());
        runA.gearIndex = selectStartingGear(vehicleA, runA.speed);
        runB.gearIndex = selectStartingGear(vehicleB, runB.speed);
        List<RaceFrame> timeline = new ArrayList<>();

        VehicleState initialA = snapshot(runA, 0.0, config);
        VehicleState initialB = snapshot(runB, 0.0, config);
        timeline.add(new RaceFrame(0.0, initialA, initialB));

        double time = 0.0;
        while ((!runA.finished || !runB.finished) && time < config.maximumDurationSeconds()) {
            double nextTime = time + config.timeStepSeconds();
            VehicleState previousA = timeline.getLast().vehicleA();
            VehicleState previousB = timeline.getLast().vehicleB();

            VehicleState stateA = advance(runA, nextTime, config);
            VehicleState stateB = advance(runB, nextTime, config);
            captureMilestones(runA, previousA, stateA, config);
            captureMilestones(runB, previousB, stateB, config);
            timeline.add(new RaceFrame(nextTime, stateA, stateB));
            time = nextTime;
        }

        if (!runA.finished || !runB.finished) {
            throw new IllegalStateException("Race did not finish within the configured maximum duration");
        }

        VehicleRaceResult resultA = resultFor(runA);
        VehicleRaceResult resultB = resultFor(runB);
        double difference = resultA.finishTimeSeconds() - resultB.finishTimeSeconds();
        String winner = Math.abs(difference) < 1.0e-9
                ? "Tie"
                : difference < 0.0 ? vehicleA.name() : vehicleB.name();
        var summary = new RaceAnalyzer().analyze(resultA, resultB, timeline);

        return new RaceResult(
                config,
                resultA,
                resultB,
                winner,
                Math.abs(difference),
                summary,
                timeline);
    }

    private VehicleState advance(MutableRun run, double time, RaceConfig config) {
        if (run.finished) {
            return new VehicleState(
                    time,
                    run.distance,
                    run.speed,
                    0.0,
                    run.rpm,
                    run.gearIndex + 1,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    false,
                    0.0,
                    false,
                    1.0,
                    aerodynamicDrag(run.vehicle, run.speed, config),
                    rollingResistance(run.vehicle, config),
                    0.0, 0.0, 0.0, airDensity(config.environment()),
                    gradeResistance(run.vehicle, config), 0.0,
                    AccelerationLimit.FINISHED);
        }

        updateTransmission(run, time);
        VehicleSpec vehicle = run.vehicle;
        LaunchState launch = launchState(run, time);
        run.rpm = launch.engineRpm();
        double ratio = vehicle.gearRatios().get(run.gearIndex);
        double torque = run.shifting ? 0.0 : vehicle.torqueCurve().torqueAt(run.rpm);
        double density = airDensity(config.environment());
        double powerCorrection = airPowerCorrection(density);
        torque *= powerCorrection;
        double effectiveEfficiency = effectiveDrivetrainEfficiency(vehicle, run.rpm, run.speed);
        double requestedWheelForce = torque * ratio * vehicle.finalDriveRatio()
                * effectiveEfficiency / vehicle.wheelRadiusMeters()
                * launch.torqueTransferFraction();
        double drag = aerodynamicDrag(vehicle, run.speed, config);
        double rollingResistance = rollingResistance(vehicle, config);
        double gradeResistance = gradeResistance(vehicle, config);
        ForceResolution forces = resolveTractionLimitedForce(
                vehicle,
                config,
                requestedWheelForce,
                drag,
                rollingResistance + gradeResistance,
                run.acceleration,
                launch.controlledByLaunchControl());
        double usableWheelForce = forces.usableWheelForce();
        double tractionLimit = forces.tractionLimit();
        double acceleration = forces.acceleration();
        boolean tractionLimited = requestedWheelForce > tractionLimit + 1.0e-9;
        double demandRatio = tractionLimit <= 0.0 ? 0.0 : requestedWheelForce / tractionLimit;
        double wheelSlipRatio = tractionLimited
                ? launch.controlledByLaunchControl()
                    ? Math.clamp(0.04 + (demandRatio - 1.0) * 0.02, 0.04, 0.10)
                    : Math.clamp(0.08 + (demandRatio - 1.0) * 0.18, 0.08, 0.35)
                : Math.clamp(demandRatio * 0.03, 0.0, 0.03);
        double nextSpeed = Math.max(0.0, run.speed + acceleration * config.timeStepSeconds());
        double nextDistance = run.distance
                + ((run.speed + nextSpeed) / 2.0) * config.timeStepSeconds();

        run.speed = nextSpeed;
        run.distance = nextDistance;
        run.rpm = engineRpm(vehicle, run.speed, run.gearIndex);
        run.acceleration = acceleration;
        double enginePower = torque * run.rpm * (2.0 * Math.PI / 60.0);
        double wheelTorque = usableWheelForce * vehicle.wheelRadiusMeters();
        double netForce = usableWheelForce - drag - rollingResistance - gradeResistance;
        AccelerationLimit limit = run.shifting
                ? AccelerationLimit.SHIFT
                : tractionLimited ? AccelerationLimit.TRACTION
                : drag > usableWheelForce * 0.5
                    ? AccelerationLimit.AERODYNAMIC_DRAG
                    : AccelerationLimit.ENGINE_POWER;

        return new VehicleState(
                time,
                run.distance,
                run.speed,
                acceleration,
                run.rpm,
                run.gearIndex + 1,
                torque,
                requestedWheelForce,
                usableWheelForce,
                tractionLimit,
                tractionLimited,
                wheelSlipRatio,
                launch.active(),
                launch.torqueTransferFraction(),
                drag,
                rollingResistance,
                enginePower,
                wheelTorque,
                effectiveEfficiency,
                density,
                gradeResistance,
                netForce,
                limit);
    }

    private VehicleState snapshot(MutableRun run, double time, RaceConfig config) {
        updateTransmission(run, time);
        LaunchState launch = launchState(run, time);
        run.rpm = launch.engineRpm();
        double drag = aerodynamicDrag(run.vehicle, run.speed, config);
        double rollingResistance = rollingResistance(run.vehicle, config);
        double density = airDensity(config.environment());
        double gradeResistance = gradeResistance(run.vehicle, config);
        double efficiency = effectiveDrivetrainEfficiency(run.vehicle, run.rpm, run.speed);
        double torque = run.vehicle.torqueCurve().torqueAt(run.rpm) * airPowerCorrection(density);
        return new VehicleState(
                time,
                run.distance,
                run.speed,
                0.0,
                run.rpm,
                run.gearIndex + 1,
                torque,
                0.0,
                0.0,
                tractionLimit(run.vehicle, config, 0.0),
                false,
                0.0,
                launch.active(),
                launch.torqueTransferFraction(),
                drag,
                rollingResistance,
                torque * run.rpm * (2.0 * Math.PI / 60.0),
                0.0,
                efficiency,
                density,
                gradeResistance,
                -drag - rollingResistance - gradeResistance,
                AccelerationLimit.ENGINE_POWER);
    }

    private LaunchState launchState(MutableRun run, double time) {
        var profile = run.vehicle.launchProfile();
        boolean active = run.launchEnabled
                && !run.shifting
                && time <= profile.engagementDurationSeconds() + 1.0e-12;
        if (!active) {
            return new LaunchState(false, run.rpm, 1.0, false);
        }

        double normalizedTime = Math.clamp(
                time / profile.engagementDurationSeconds(),
                0.0,
                1.0);
        double smoothProgress = normalizedTime * normalizedTime * (3.0 - 2.0 * normalizedTime);
        double transfer = interpolate(
                profile.initialTorqueTransferFraction(),
                1.0,
                smoothProgress);
        return new LaunchState(
                true,
                Math.max(run.rpm, profile.launchRpm()),
                transfer,
                profile.launchControlEnabled());
    }

    private void updateTransmission(MutableRun run, double time) {
        VehicleSpec vehicle = run.vehicle;

        if (run.shifting) {
            if (time + 1.0e-12 >= run.shiftEndTime) {
                run.gearIndex = run.targetGearIndex;
                run.shifting = false;
                run.rpm = engineRpm(vehicle, run.speed, run.gearIndex);
            } else {
                double progress = Math.clamp(
                        (time - run.shiftStartTime) / vehicle.shiftDurationSeconds(),
                        0.0,
                        1.0);
                run.rpm = interpolate(run.shiftRpmBefore, run.shiftRpmAfter, progress);
                return;
            }
        }

        run.rpm = engineRpm(vehicle, run.speed, run.gearIndex);
        if (run.rpm >= vehicle.shiftRpm() && run.gearIndex < vehicle.gearRatios().size() - 1) {
            run.shifting = true;
            run.targetGearIndex = run.gearIndex + 1;
            run.shiftStartTime = time;
            run.shiftEndTime = time + vehicle.shiftDurationSeconds();
            run.shiftRpmBefore = run.rpm;
            run.shiftRpmAfter = engineRpm(vehicle, run.speed, run.targetGearIndex);
            run.shiftEvents.add(new ShiftEvent(
                    run.shiftStartTime,
                    run.shiftEndTime,
                    run.gearIndex + 1,
                    run.targetGearIndex + 1,
                    run.shiftRpmBefore,
                    run.shiftRpmAfter));
        }
    }

    private double engineRpm(VehicleSpec vehicle, double speed, int gearIndex) {
        double wheelRevolutionsPerSecond = speed / (2.0 * Math.PI * vehicle.wheelRadiusMeters());
        double rpm = wheelRevolutionsPerSecond * 60.0
                * vehicle.gearRatios().get(gearIndex)
                * vehicle.finalDriveRatio();
        return Math.min(vehicle.redlineRpm(), Math.max(vehicle.idleRpm(), rpm));
    }

    private int selectStartingGear(VehicleSpec vehicle, double speed) {
        if (speed < 0.5) {
            return 0;
        }

        int bestGear = vehicle.gearRatios().size() - 1;
        double bestWheelForce = Double.NEGATIVE_INFINITY;
        for (int gearIndex = 0; gearIndex < vehicle.gearRatios().size(); gearIndex++) {
            double rpm = unboundedEngineRpm(vehicle, speed, gearIndex);
            if (rpm > vehicle.shiftRpm()) {
                continue;
            }
            double operatingRpm = Math.max(vehicle.idleRpm(), rpm);
            double wheelForce = vehicle.torqueCurve().torqueAt(operatingRpm)
                    * vehicle.gearRatios().get(gearIndex)
                    * vehicle.finalDriveRatio()
                    * vehicle.drivetrainEfficiency()
                    / vehicle.wheelRadiusMeters();
            if (wheelForce > bestWheelForce) {
                bestWheelForce = wheelForce;
                bestGear = gearIndex;
            }
        }
        return bestGear;
    }

    private double unboundedEngineRpm(VehicleSpec vehicle, double speed, int gearIndex) {
        double wheelRevolutionsPerSecond = speed / (2.0 * Math.PI * vehicle.wheelRadiusMeters());
        return wheelRevolutionsPerSecond * 60.0
                * vehicle.gearRatios().get(gearIndex)
                * vehicle.finalDriveRatio();
    }

    private double aerodynamicDrag(VehicleSpec vehicle, double speed, RaceConfig config) {
        double airSpeed = Math.max(0.0, speed + config.environment().headwindMetersPerSecond());
        return 0.5 * airDensity(config.environment())
                * vehicle.dragCoefficient()
                * vehicle.frontalAreaSquareMeters()
                * airSpeed * airSpeed;
    }

    private double rollingResistance(VehicleSpec vehicle, RaceConfig config) {
        double temperatureFactor = Math.clamp(
                1.0 + (20.0 - config.environment().roadTemperatureCelsius()) * 0.003,
                0.85,
                1.20);
        return vehicle.rollingResistanceCoefficient()
                * vehicle.massKg()
                * GRAVITY_METERS_PER_SECOND_SQUARED
                * temperatureFactor;
    }

    private double gradeResistance(VehicleSpec vehicle, RaceConfig config) {
        double grade = config.environment().roadGradePercent() / 100.0;
        return vehicle.massKg() * GRAVITY_METERS_PER_SECOND_SQUARED
                * Math.sin(Math.atan(grade));
    }

    private double airDensity(EnvironmentConditions environment) {
        double temperatureKelvin = environment.airTemperatureCelsius() + 273.15;
        double saturationPressure = 610.94 * Math.exp(
                17.625 * environment.airTemperatureCelsius()
                        / (environment.airTemperatureCelsius() + 243.04));
        double vaporPressure = environment.relativeHumidity() * saturationPressure;
        double dryPressure = environment.airPressurePascals() - vaporPressure;
        return dryPressure / (287.058 * temperatureKelvin)
                + vaporPressure / (461.495 * temperatureKelvin);
    }

    private double airPowerCorrection(double density) {
        return Math.clamp(Math.sqrt(density / STANDARD_AIR_DENSITY_KG_PER_CUBIC_METER), 0.75, 1.10);
    }

    private double effectiveDrivetrainEfficiency(VehicleSpec vehicle, double rpm, double speed) {
        double normalizedRpm = Math.clamp(rpm / vehicle.redlineRpm(), 0.0, 1.0);
        double speedFactor = Math.clamp(speed / 70.0, 0.0, 1.0);
        double drivetrainFactor = switch (vehicle.drivetrain()) {
            case FWD -> 1.005;
            case RWD -> 1.000;
            case AWD -> 0.985;
        };
        double transmissionFactor = switch (vehicle.transmissionType()) {
            case MANUAL -> 1.000;
            case TORQUE_CONVERTER_AUTOMATIC -> 0.990;
            case DUAL_CLUTCH -> 1.005;
        };
        double operatingFactor = 1.01 - 0.025 * normalizedRpm - 0.01 * speedFactor;
        return Math.clamp(vehicle.drivetrainEfficiency()
                * drivetrainFactor * transmissionFactor * operatingFactor, 0.65, 0.96);
    }

    /**
     * Solves the small feedback loop between longitudinal acceleration, weight
     * transfer, driven-axle load, and the resulting traction limit.
     */
    private ForceResolution resolveTractionLimitedForce(
            VehicleSpec vehicle,
            RaceConfig config,
            double requestedWheelForce,
            double drag,
            double rollingResistance,
            double previousAcceleration,
            boolean launchControlled) {
        double acceleration = previousAcceleration;
        double limit = tractionLimit(vehicle, config, acceleration);
        double usableForce = tireForce(requestedWheelForce, limit, launchControlled);

        for (int iteration = 0; iteration < 8; iteration++) {
            double nextAcceleration = (usableForce - drag - rollingResistance) / vehicle.massKg();
            limit = tractionLimit(vehicle, config, nextAcceleration);
            usableForce = tireForce(requestedWheelForce, limit, launchControlled);
            acceleration = nextAcceleration;
        }

        acceleration = (usableForce - drag - rollingResistance) / vehicle.massKg();
        return new ForceResolution(usableForce, limit, acceleration);
    }

    private double tireForce(double requestedForce, double tractionLimit, boolean launchControlled) {
        if (requestedForce <= tractionLimit) {
            return requestedForce;
        }
        if (launchControlled) {
            return tractionLimit;
        }
        double overload = (requestedForce - tractionLimit) / Math.max(requestedForce, 1.0);
        return tractionLimit * Math.clamp(1.0 - 0.18 * overload, 0.82, 1.0);
    }

    private double tractionLimit(VehicleSpec vehicle, RaceConfig config, double acceleration) {
        double totalNormalForce = vehicle.massKg() * GRAVITY_METERS_PER_SECOND_SQUARED;
        double transfer = vehicle.massKg()
                * acceleration
                * vehicle.centerOfGravityHeightMeters()
                / vehicle.wheelbaseMeters();
        double frontNormalForce = Math.clamp(
                totalNormalForce * vehicle.staticFrontWeightFraction() - transfer,
                0.0,
                totalNormalForce);
        double rearNormalForce = totalNormalForce - frontNormalForce;
        double drivenNormalForce = switch (vehicle.drivetrain()) {
            case FWD -> frontNormalForce;
            case RWD -> rearNormalForce;
            case AWD -> totalNormalForce;
        };
        return vehicle.tireFrictionCoefficient()
                * config.roadSurface().gripMultiplier()
                * roadTemperatureGripFactor(config.environment().roadTemperatureCelsius())
                * drivenNormalForce;
    }

    private double roadTemperatureGripFactor(double roadTemperatureCelsius) {
        double distanceFromOptimum = Math.abs(roadTemperatureCelsius - 30.0);
        return Math.clamp(1.03 - distanceFromOptimum * 0.004, 0.75, 1.03);
    }

    private void captureMilestones(
            MutableRun run,
            VehicleState previous,
            VehicleState current,
            RaceConfig config) {
        captureSpeedMilestone(run, "0-60 mph", SIXTY_MPH_METERS_PER_SECOND, previous, current);

        if (config.goal() instanceof DistanceRaceGoal distanceGoal) {
            if (distanceGoal.distanceMeters() >= RaceConfig.EIGHTH_MILE_METERS) {
                captureDistanceMilestone(run, "1/8 mile", RaceConfig.EIGHTH_MILE_METERS, previous, current);
            }
            captureDistanceMilestone(run, "Finish", distanceGoal.distanceMeters(), previous, current);
        } else if (config.goal() instanceof SpeedRaceGoal speedGoal) {
            captureSpeedMilestone(run, "Finish", speedGoal.targetSpeedMetersPerSecond(), previous, current);
        }

        if (!run.finished && hasMilestone(run, "Finish")) {
            Milestone finish = run.milestones.stream()
                    .filter(milestone -> milestone.name().equals("Finish"))
                    .findFirst()
                    .orElseThrow();
            run.finished = true;
            run.finishTime = finish.elapsedSeconds();
            run.finishSpeed = finish.speedMetersPerSecond();
        }
    }

    private void captureSpeedMilestone(
            MutableRun run,
            String name,
            double targetSpeed,
            VehicleState previous,
            VehicleState current) {
        if (hasMilestone(run, name)
                || previous.speedMetersPerSecond() >= targetSpeed
                || current.speedMetersPerSecond() < targetSpeed) {
            return;
        }
        double fraction = interpolationFraction(
                previous.speedMetersPerSecond(),
                current.speedMetersPerSecond(),
                targetSpeed);
        double time = interpolate(previous.timeSeconds(), current.timeSeconds(), fraction);
        run.milestones.add(new Milestone(name, time, targetSpeed));
    }

    private void captureDistanceMilestone(
            MutableRun run,
            String name,
            double targetDistance,
            VehicleState previous,
            VehicleState current) {
        if (hasMilestone(run, name)
                || previous.distanceMeters() >= targetDistance
                || current.distanceMeters() < targetDistance) {
            return;
        }
        double fraction = interpolationFraction(
                previous.distanceMeters(),
                current.distanceMeters(),
                targetDistance);
        double time = interpolate(previous.timeSeconds(), current.timeSeconds(), fraction);
        double speed = interpolate(
                previous.speedMetersPerSecond(),
                current.speedMetersPerSecond(),
                fraction);
        run.milestones.add(new Milestone(name, time, speed));
    }

    private boolean hasMilestone(MutableRun run, String name) {
        return run.milestones.stream().anyMatch(milestone -> milestone.name().equals(name));
    }

    private double interpolationFraction(double start, double end, double target) {
        if (Math.abs(end - start) < 1.0e-12) {
            return 0.0;
        }
        return Math.clamp((target - start) / (end - start), 0.0, 1.0);
    }

    private double interpolate(double start, double end, double fraction) {
        return start + (end - start) * fraction;
    }

    private VehicleRaceResult resultFor(MutableRun run) {
        return new VehicleRaceResult(
                run.vehicle,
                run.finishTime,
                run.finishSpeed,
                run.milestones,
                run.shiftEvents);
    }

    private static final class MutableRun {
        private final VehicleSpec vehicle;
        private final List<Milestone> milestones = new ArrayList<>();
        private final List<ShiftEvent> shiftEvents = new ArrayList<>();
        private final boolean launchEnabled;
        private double distance;
        private double speed;
        private double rpm;
        private double acceleration;
        private int gearIndex;
        private int targetGearIndex;
        private boolean shifting;
        private double shiftStartTime;
        private double shiftEndTime;
        private double shiftRpmBefore;
        private double shiftRpmAfter;
        private boolean finished;
        private double finishTime;
        private double finishSpeed;

        private MutableRun(VehicleSpec vehicle, double startingSpeed) {
            this.vehicle = vehicle;
            this.speed = startingSpeed;
            this.launchEnabled = startingSpeed < 0.5;
        }
    }

    private record ForceResolution(
            double usableWheelForce,
            double tractionLimit,
            double acceleration) {
    }

    private record LaunchState(
            boolean active,
            double engineRpm,
            double torqueTransferFraction,
            boolean controlledByLaunchControl) {
    }
}

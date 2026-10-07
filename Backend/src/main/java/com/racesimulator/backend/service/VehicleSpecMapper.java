package com.racesimulator.backend.service;

import com.racesimulator.backend.entity.VehicleTrim;
import com.racesimulator.engine.model.*;
import org.springframework.stereotype.Component;

@Component
public class VehicleSpecMapper {
    public VehicleSpec toEngineSpec(VehicleTrim trim) {
        var model = trim.getGeneration().getModel();
        var engine = trim.getEngine();
        var transmission = trim.getTransmission();
        var spec = trim.getSpecification();
        String name = "%d %s %s %s".formatted(
                trim.getModelYear(), model.getMake().getName(), model.getName(), trim.getTrimName());

        if (transmission.getGearRatios().size() != transmission.getNumberOfGears()) {
            throw new IllegalArgumentException("Vehicle is not simulation-ready: incomplete gear ratios");
        }
        var curve = new TorqueCurve(engine.getTorqueCurvePoints().stream()
                .map(point -> new TorquePoint(point.getRpm(), point.getTorqueNm())).toList());
        if (curve.points().firstKey() > engine.getIdleRpm() || curve.points().lastKey() < engine.getRedlineRpm()) {
            throw new IllegalArgumentException("Vehicle is not simulation-ready: torque curve must cover idle through redline");
        }
        return new VehicleSpec(
                name,
                spec.getMassKg(),
                Drivetrain.valueOf(trim.getDrivetrain()),
                spec.getStaticFrontWeightFraction(),
                spec.getWheelbaseMeters(),
                spec.getCenterOfGravityHeightMeters(),
                transmission.getDrivetrainEfficiency(),
                spec.getWheelRadiusMeters(),
                spec.getDragCoefficient(),
                spec.getFrontalAreaSquareMeters(),
                spec.getRollingResistanceCoefficient(),
                spec.getTireFrictionCoefficient(),
                transmission.getFinalDriveRatio(),
                transmission.getGearRatios().stream().map(ratio -> ratio.getRatio()).toList(),
                TransmissionType.valueOf(transmission.getTransmissionType()),
                transmission.getShiftDurationSeconds(),
                new LaunchProfile(spec.getLaunchRpm(), spec.getLaunchEngagementDurationSeconds(),
                        spec.getInitialTorqueTransferFraction(), spec.isLaunchControlEnabled()),
                engine.getIdleRpm(),
                engine.getShiftRpm(),
                engine.getRedlineRpm(),
                curve);
    }
}

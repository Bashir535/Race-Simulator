package com.racesimulator.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "vehicle_specifications")
public class VehicleSpecification {
    @Id @Column(name = "vehicle_trim_id") private Long vehicleTrimId;
    @MapsId @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "vehicle_trim_id")
    private VehicleTrim trim;
    @Column(name = "mass_kg", nullable = false) private double massKg;
    @Column(name = "static_front_weight_fraction", nullable = false) private double staticFrontWeightFraction;
    @Column(name = "wheelbase_meters", nullable = false) private double wheelbaseMeters;
    @Column(name = "center_of_gravity_height_meters", nullable = false) private double centerOfGravityHeightMeters;
    @Column(name = "wheel_radius_meters", nullable = false) private double wheelRadiusMeters;
    @Column(name = "drag_coefficient", nullable = false) private double dragCoefficient;
    @Column(name = "frontal_area_square_meters", nullable = false) private double frontalAreaSquareMeters;
    @Column(name = "rolling_resistance_coefficient", nullable = false) private double rollingResistanceCoefficient;
    @Column(name = "tire_friction_coefficient", nullable = false) private double tireFrictionCoefficient;
    @Column(name = "launch_rpm", nullable = false) private double launchRpm;
    @Column(name = "launch_engagement_duration_seconds", nullable = false) private double launchEngagementDurationSeconds;
    @Column(name = "initial_torque_transfer_fraction", nullable = false) private double initialTorqueTransferFraction;
    @Column(name = "launch_control_enabled", nullable = false) private boolean launchControlEnabled;
    protected VehicleSpecification() {}
    public double getMassKg() { return massKg; }
    public double getStaticFrontWeightFraction() { return staticFrontWeightFraction; }
    public double getWheelbaseMeters() { return wheelbaseMeters; }
    public double getCenterOfGravityHeightMeters() { return centerOfGravityHeightMeters; }
    public double getWheelRadiusMeters() { return wheelRadiusMeters; }
    public double getDragCoefficient() { return dragCoefficient; }
    public double getFrontalAreaSquareMeters() { return frontalAreaSquareMeters; }
    public double getRollingResistanceCoefficient() { return rollingResistanceCoefficient; }
    public double getTireFrictionCoefficient() { return tireFrictionCoefficient; }
    public double getLaunchRpm() { return launchRpm; }
    public double getLaunchEngagementDurationSeconds() { return launchEngagementDurationSeconds; }
    public double getInitialTorqueTransferFraction() { return initialTorqueTransferFraction; }
    public boolean isLaunchControlEnabled() { return launchControlEnabled; }
}

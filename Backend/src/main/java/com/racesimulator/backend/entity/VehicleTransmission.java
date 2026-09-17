package com.racesimulator.backend.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transmissions")
public class VehicleTransmission {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 150) private String name;
    @Column(name = "transmission_type", nullable = false, length = 40) private String transmissionType;
    @Column(name = "number_of_gears", nullable = false) private short numberOfGears;
    @Column(name = "final_drive_ratio", nullable = false) private double finalDriveRatio;
    @Column(name = "shift_duration_seconds", nullable = false) private double shiftDurationSeconds;
    @Column(name = "drivetrain_efficiency", nullable = false) private double drivetrainEfficiency;
    @OneToMany(mappedBy = "transmission", fetch = FetchType.LAZY)
    @OrderBy("gearNumber ASC")
    private List<GearRatioEntity> gearRatios = new ArrayList<>();
    protected VehicleTransmission() {}
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getTransmissionType() { return transmissionType; }
    public short getNumberOfGears() { return numberOfGears; }
    public double getFinalDriveRatio() { return finalDriveRatio; }
    public double getShiftDurationSeconds() { return shiftDurationSeconds; }
    public double getDrivetrainEfficiency() { return drivetrainEfficiency; }
    public List<GearRatioEntity> getGearRatios() { return gearRatios; }
}

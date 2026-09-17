package com.racesimulator.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "gear_ratios")
public class GearRatioEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transmission_id", nullable = false)
    private VehicleTransmission transmission;
    @Column(name = "gear_number", nullable = false) private short gearNumber;
    @Column(nullable = false) private double ratio;
    protected GearRatioEntity() {}
    public Long getId() { return id; }
    public short getGearNumber() { return gearNumber; }
    public double getRatio() { return ratio; }
}

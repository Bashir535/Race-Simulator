package com.racesimulator.backend.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "engines")
public class PowertrainEngine {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 150) private String name;
    @Column(name = "displacement_liters") private Double displacementLiters;
    @Column(length = 50) private String configuration;
    @Column(length = 50) private String aspiration;
    @Column(name = "fuel_type", length = 50) private String fuelType;
    @Column(name = "rated_horsepower") private Double ratedHorsepower;
    @Column(name = "peak_horsepower_rpm") private Double peakHorsepowerRpm;
    @Column(name = "rated_torque_nm") private Double ratedTorqueNm;
    @Column(name = "peak_torque_rpm") private Double peakTorqueRpm;
    @Column(name = "idle_rpm", nullable = false) private double idleRpm;
    @Column(name = "shift_rpm", nullable = false) private double shiftRpm;
    @Column(name = "redline_rpm", nullable = false) private double redlineRpm;
    @OneToMany(mappedBy = "engine", fetch = FetchType.LAZY)
    @OrderBy("rpm ASC")
    private List<TorqueCurvePointEntity> torqueCurvePoints = new ArrayList<>();
    protected PowertrainEngine() {}
    public Long getId() { return id; }
    public String getName() { return name; }
    public Double getDisplacementLiters() { return displacementLiters; }
    public String getConfiguration() { return configuration; }
    public String getAspiration() { return aspiration; }
    public String getFuelType() { return fuelType; }
    public Double getRatedHorsepower() { return ratedHorsepower; }
    public Double getPeakHorsepowerRpm() { return peakHorsepowerRpm; }
    public Double getRatedTorqueNm() { return ratedTorqueNm; }
    public Double getPeakTorqueRpm() { return peakTorqueRpm; }
    public double getIdleRpm() { return idleRpm; }
    public double getShiftRpm() { return shiftRpm; }
    public double getRedlineRpm() { return redlineRpm; }
    public List<TorqueCurvePointEntity> getTorqueCurvePoints() { return torqueCurvePoints; }
}

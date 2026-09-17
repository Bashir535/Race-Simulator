package com.racesimulator.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "torque_curve_points")
public class TorqueCurvePointEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "engine_id", nullable = false)
    private PowertrainEngine engine;
    @Column(nullable = false) private double rpm;
    @Column(name = "torque_nm", nullable = false) private double torqueNm;
    protected TorqueCurvePointEntity() {}
    public Long getId() { return id; }
    public double getRpm() { return rpm; }
    public double getTorqueNm() { return torqueNm; }
}

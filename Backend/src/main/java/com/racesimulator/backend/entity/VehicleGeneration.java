package com.racesimulator.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "vehicle_generations")
public class VehicleGeneration {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_model_id", nullable = false)
    private VehicleModel model;
    @Column(length = 100) private String code;
    @Column(name = "body_style", length = 50) private String bodyStyle;
    @Column(name = "production_start_year") private Short productionStartYear;
    @Column(name = "production_end_year") private Short productionEndYear;
    protected VehicleGeneration() {}
    public Long getId() { return id; }
    public VehicleModel getModel() { return model; }
    public String getCode() { return code; }
    public String getBodyStyle() { return bodyStyle; }
    public Short getProductionStartYear() { return productionStartYear; }
    public Short getProductionEndYear() { return productionEndYear; }
}

package com.racesimulator.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "vehicle_models")
public class VehicleModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "make_id", nullable = false)
    private VehicleMake make;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String generation;

    @Column(name = "body_style", length = 50)
    private String bodyStyle;

    @Column(name = "production_start_year")
    private Short productionStartYear;

    @Column(name = "production_end_year")
    private Short productionEndYear;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    protected VehicleModel() {
    }

    public Long getId() {
        return id;
    }

    public VehicleMake getMake() {
        return make;
    }

    public String getName() {
        return name;
    }

    public String getGeneration() {
        return generation;
    }

    public String getBodyStyle() {
        return bodyStyle;
    }

    public Short getProductionStartYear() {
        return productionStartYear;
    }

    public Short getProductionEndYear() {
        return productionEndYear;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

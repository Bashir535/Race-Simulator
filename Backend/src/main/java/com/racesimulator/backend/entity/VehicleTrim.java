package com.racesimulator.backend.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vehicle_trims")
public class VehicleTrim {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generation_id", nullable = false)
    private VehicleGeneration generation;
    @Column(name = "model_year", nullable = false) private short modelYear;
    @Column(name = "trim_name", nullable = false, length = 150) private String trimName;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "engine_id", nullable = false)
    private PowertrainEngine engine;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transmission_id", nullable = false)
    private VehicleTransmission transmission;
    @Column(nullable = false, length = 10) private String drivetrain;
    @Column(name = "original_msrp_usd") private Double originalMsrpUsd;
    @Column(name = "tire_description", length = 150) private String tireDescription;
    @Column(name = "city_mpg") private Double cityMpg;
    @Column(name = "highway_mpg") private Double highwayMpg;
    @Column(name = "combined_mpg") private Double combinedMpg;
    @Column(name = "image_url") private String imageUrl;
    @Column(name = "is_popular", nullable = false) private boolean popular;
    @OneToOne(mappedBy = "trim", fetch = FetchType.LAZY, optional = false)
    private VehicleSpecification specification;
    @OneToMany(mappedBy = "trim", fetch = FetchType.LAZY)
    private List<PublishedPerformance> publishedPerformance = new ArrayList<>();
    protected VehicleTrim() {}
    public Long getId() { return id; }
    public VehicleGeneration getGeneration() { return generation; }
    public short getModelYear() { return modelYear; }
    public String getTrimName() { return trimName; }
    public PowertrainEngine getEngine() { return engine; }
    public VehicleTransmission getTransmission() { return transmission; }
    public String getDrivetrain() { return drivetrain; }
    public Double getOriginalMsrpUsd() { return originalMsrpUsd; }
    public String getTireDescription() { return tireDescription; }
    public Double getCityMpg() { return cityMpg; }
    public Double getHighwayMpg() { return highwayMpg; }
    public Double getCombinedMpg() { return combinedMpg; }
    public String getImageUrl() { return imageUrl; }
    public boolean isPopular() { return popular; }
    public VehicleSpecification getSpecification() { return specification; }
    public List<PublishedPerformance> getPublishedPerformance() { return publishedPerformance; }
}

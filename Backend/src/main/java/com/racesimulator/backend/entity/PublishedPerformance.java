package com.racesimulator.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "published_performance")
public class PublishedPerformance {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_trim_id", nullable = false)
    private VehicleTrim trim;
    @Column(name = "zero_to_sixty_seconds") private Double zeroToSixtySeconds;
    @Column(name = "quarter_mile_seconds") private Double quarterMileSeconds;
    @Column(name = "quarter_mile_trap_speed_mph") private Double quarterMileTrapSpeedMph;
    @Column(name = "rollout_seconds") private Double rolloutSeconds;
    @Column(name = "source_description", nullable = false) private String sourceDescription;
    protected PublishedPerformance() {}
    public Double getZeroToSixtySeconds() { return zeroToSixtySeconds; }
    public Double getQuarterMileSeconds() { return quarterMileSeconds; }
    public Double getQuarterMileTrapSpeedMph() { return quarterMileTrapSpeedMph; }
    public Double getRolloutSeconds() { return rolloutSeconds; }
    public String getSourceDescription() { return sourceDescription; }
}

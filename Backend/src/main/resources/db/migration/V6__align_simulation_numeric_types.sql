-- Hibernate maps Java double/Double fields to PostgreSQL DOUBLE PRECISION.
-- Keep the persistence schema aligned with those entity types so schema
-- validation remains enabled and catches real mapping mistakes at startup.
ALTER TABLE engines
    ALTER COLUMN displacement_liters TYPE DOUBLE PRECISION USING displacement_liters::DOUBLE PRECISION,
    ALTER COLUMN rated_horsepower TYPE DOUBLE PRECISION USING rated_horsepower::DOUBLE PRECISION,
    ALTER COLUMN peak_horsepower_rpm TYPE DOUBLE PRECISION USING peak_horsepower_rpm::DOUBLE PRECISION,
    ALTER COLUMN rated_torque_nm TYPE DOUBLE PRECISION USING rated_torque_nm::DOUBLE PRECISION,
    ALTER COLUMN peak_torque_rpm TYPE DOUBLE PRECISION USING peak_torque_rpm::DOUBLE PRECISION,
    ALTER COLUMN idle_rpm TYPE DOUBLE PRECISION USING idle_rpm::DOUBLE PRECISION,
    ALTER COLUMN shift_rpm TYPE DOUBLE PRECISION USING shift_rpm::DOUBLE PRECISION,
    ALTER COLUMN redline_rpm TYPE DOUBLE PRECISION USING redline_rpm::DOUBLE PRECISION;

ALTER TABLE torque_curve_points
    ALTER COLUMN rpm TYPE DOUBLE PRECISION USING rpm::DOUBLE PRECISION,
    ALTER COLUMN torque_nm TYPE DOUBLE PRECISION USING torque_nm::DOUBLE PRECISION;

ALTER TABLE transmissions
    ALTER COLUMN final_drive_ratio TYPE DOUBLE PRECISION USING final_drive_ratio::DOUBLE PRECISION,
    ALTER COLUMN shift_duration_seconds TYPE DOUBLE PRECISION USING shift_duration_seconds::DOUBLE PRECISION,
    ALTER COLUMN drivetrain_efficiency TYPE DOUBLE PRECISION USING drivetrain_efficiency::DOUBLE PRECISION;

ALTER TABLE gear_ratios
    ALTER COLUMN ratio TYPE DOUBLE PRECISION USING ratio::DOUBLE PRECISION;

ALTER TABLE vehicle_trims
    ALTER COLUMN original_msrp_usd TYPE DOUBLE PRECISION USING original_msrp_usd::DOUBLE PRECISION,
    ALTER COLUMN city_mpg TYPE DOUBLE PRECISION USING city_mpg::DOUBLE PRECISION,
    ALTER COLUMN highway_mpg TYPE DOUBLE PRECISION USING highway_mpg::DOUBLE PRECISION,
    ALTER COLUMN combined_mpg TYPE DOUBLE PRECISION USING combined_mpg::DOUBLE PRECISION;

ALTER TABLE vehicle_specifications
    ALTER COLUMN mass_kg TYPE DOUBLE PRECISION USING mass_kg::DOUBLE PRECISION,
    ALTER COLUMN static_front_weight_fraction TYPE DOUBLE PRECISION USING static_front_weight_fraction::DOUBLE PRECISION,
    ALTER COLUMN wheelbase_meters TYPE DOUBLE PRECISION USING wheelbase_meters::DOUBLE PRECISION,
    ALTER COLUMN center_of_gravity_height_meters TYPE DOUBLE PRECISION USING center_of_gravity_height_meters::DOUBLE PRECISION,
    ALTER COLUMN wheel_radius_meters TYPE DOUBLE PRECISION USING wheel_radius_meters::DOUBLE PRECISION,
    ALTER COLUMN drag_coefficient TYPE DOUBLE PRECISION USING drag_coefficient::DOUBLE PRECISION,
    ALTER COLUMN frontal_area_square_meters TYPE DOUBLE PRECISION USING frontal_area_square_meters::DOUBLE PRECISION,
    ALTER COLUMN rolling_resistance_coefficient TYPE DOUBLE PRECISION USING rolling_resistance_coefficient::DOUBLE PRECISION,
    ALTER COLUMN tire_friction_coefficient TYPE DOUBLE PRECISION USING tire_friction_coefficient::DOUBLE PRECISION,
    ALTER COLUMN launch_rpm TYPE DOUBLE PRECISION USING launch_rpm::DOUBLE PRECISION,
    ALTER COLUMN launch_engagement_duration_seconds TYPE DOUBLE PRECISION USING launch_engagement_duration_seconds::DOUBLE PRECISION,
    ALTER COLUMN initial_torque_transfer_fraction TYPE DOUBLE PRECISION USING initial_torque_transfer_fraction::DOUBLE PRECISION;

ALTER TABLE published_performance
    ALTER COLUMN zero_to_sixty_seconds TYPE DOUBLE PRECISION USING zero_to_sixty_seconds::DOUBLE PRECISION,
    ALTER COLUMN quarter_mile_seconds TYPE DOUBLE PRECISION USING quarter_mile_seconds::DOUBLE PRECISION,
    ALTER COLUMN quarter_mile_trap_speed_mph TYPE DOUBLE PRECISION USING quarter_mile_trap_speed_mph::DOUBLE PRECISION,
    ALTER COLUMN rollout_seconds TYPE DOUBLE PRECISION USING rollout_seconds::DOUBLE PRECISION;

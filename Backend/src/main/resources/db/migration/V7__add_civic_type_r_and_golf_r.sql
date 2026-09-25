-- Promote the Civic Type R and Golf R validation fixtures into the catalog.
-- Published values and engineering estimates are documented field-by-field in
-- simulation-engine/REAL_VEHICLE_DATA.md. Keep this migration aligned with
-- RealVehicleFixtures so database-driven races use the validated inputs.

INSERT INTO vehicle_generations (
    vehicle_model_id, code, body_style, production_start_year, production_end_year
)
SELECT id, generation, body_style, production_start_year, production_end_year
FROM vehicle_models
WHERE (name = 'Civic' AND generation = 'FL5')
   OR (name = 'Golf' AND generation = 'Mk8');

INSERT INTO engines (
    name, displacement_liters, configuration, aspiration, fuel_type,
    rated_horsepower, peak_horsepower_rpm, rated_torque_nm, peak_torque_rpm,
    idle_rpm, shift_rpm, redline_rpm
) VALUES
    ('Honda K20C1 2.0L Turbo I4', 2.00, 'Inline-4', 'Turbocharged', 'Gasoline',
     315, 6500, 420.304, 3100, 800, 6900, 7000),
    ('Volkswagen EA888 2.0L Turbo I4', 2.00, 'Inline-4', 'Turbocharged', 'Gasoline',
     315, 5900, 420.304, 1900, 800, 6500, 6800);

INSERT INTO torque_curve_points (engine_id, rpm, torque_nm)
SELECT id, point.rpm, point.torque_nm
FROM engines
CROSS JOIN LATERAL (VALUES
    (800.0, 169.477), (1500.0, 284.722), (2600.0, 420.304),
    (4000.0, 420.304), (5000.0, 393.187), (6500.0, 345.734),
    (7000.0, 298.280)
) AS point(rpm, torque_nm)
WHERE name = 'Honda K20C1 2.0L Turbo I4';

INSERT INTO torque_curve_points (engine_id, rpm, torque_nm)
SELECT id, point.rpm, point.torque_nm
FROM engines
CROSS JOIN LATERAL (VALUES
    (800.0, 196.594), (1500.0, 352.513), (1900.0, 420.304),
    (4500.0, 420.304), (5900.0, 379.629), (6500.0, 332.175),
    (6800.0, 298.280)
) AS point(rpm, torque_nm)
WHERE name = 'Volkswagen EA888 2.0L Turbo I4';

INSERT INTO transmissions (
    name, transmission_type, number_of_gears, final_drive_ratio,
    shift_duration_seconds, drivetrain_efficiency
) VALUES
    ('Honda 6-Speed Manual', 'MANUAL', 6, 3.842, 0.20, 0.88),
    ('Volkswagen 7-Speed DSG', 'DUAL_CLUTCH', 7, 1.0, 0.08, 0.82);

INSERT INTO gear_ratios (transmission_id, gear_number, ratio)
SELECT id, gear.gear_number, gear.ratio
FROM transmissions
CROSS JOIN LATERAL (VALUES
    (1, 3.625), (2, 2.115), (3, 1.529),
    (4, 1.125), (5, 0.911), (6, 0.735)
) AS gear(gear_number, ratio)
WHERE name = 'Honda 6-Speed Manual';

-- The DSG uses two final drives. These effective ratios fold the appropriate
-- final-drive multiplication into each ratio; the stored final drive is 1.0.
INSERT INTO gear_ratios (transmission_id, gear_number, ratio)
SELECT id, gear.gear_number, gear.ratio
FROM transmissions
CROSS JOIN LATERAL (VALUES
    (1, 14.2593), (2, 12.2925), (3, 8.4930), (4, 4.6488),
    (5, 2.6070), (6, 2.8380), (7, 2.1780)
) AS gear(gear_number, ratio)
WHERE name = 'Volkswagen 7-Speed DSG';

INSERT INTO vehicle_trims (
    generation_id, model_year, trim_name, engine_id, transmission_id, drivetrain,
    original_msrp_usd, tire_description, city_mpg, highway_mpg, combined_mpg,
    is_popular
)
SELECT generation.id, 2023, 'Type R (6MT)', engine.id, transmission.id, 'FWD',
       43295, '265/30ZR19', 22, 28, 24, TRUE
FROM vehicle_generations generation
JOIN vehicle_models model ON model.id = generation.vehicle_model_id
JOIN engines engine ON engine.name = 'Honda K20C1 2.0L Turbo I4'
JOIN transmissions transmission ON transmission.name = 'Honda 6-Speed Manual'
WHERE model.name = 'Civic' AND generation.code = 'FL5';

INSERT INTO vehicle_trims (
    generation_id, model_year, trim_name, engine_id, transmission_id, drivetrain,
    original_msrp_usd, tire_description, city_mpg, highway_mpg, combined_mpg,
    is_popular
)
SELECT generation.id, 2022, 'Golf R (7DSG)', engine.id, transmission.id, 'AWD',
       44835, '235/35R19', 23, 30, 26, TRUE
FROM vehicle_generations generation
JOIN vehicle_models model ON model.id = generation.vehicle_model_id
JOIN engines engine ON engine.name = 'Volkswagen EA888 2.0L Turbo I4'
JOIN transmissions transmission ON transmission.name = 'Volkswagen 7-Speed DSG'
WHERE model.name = 'Golf' AND generation.code = 'Mk8';

INSERT INTO vehicle_specifications (
    vehicle_trim_id, mass_kg, static_front_weight_fraction, wheelbase_meters,
    center_of_gravity_height_meters, wheel_radius_meters, drag_coefficient,
    frontal_area_square_meters, rolling_resistance_coefficient,
    tire_friction_coefficient, launch_rpm, launch_engagement_duration_seconds,
    initial_torque_transfer_fraction, launch_control_enabled
)
SELECT id, 1443.784, 0.62, 2.73558, 0.55, 0.32080, 0.34, 2.20,
       0.015, 1.15, 3000, 0.50, 0.45, FALSE
FROM vehicle_trims WHERE model_year = 2023 AND trim_name = 'Type R (6MT)'
UNION ALL
SELECT id, 1524.072, 0.60, 2.62890, 0.55, 0.32365, 0.34, 2.22,
       0.015, 1.15, 3000, 0.35, 0.55, TRUE
FROM vehicle_trims WHERE model_year = 2022 AND trim_name = 'Golf R (7DSG)';

INSERT INTO published_performance (
    vehicle_trim_id, zero_to_sixty_seconds, quarter_mile_seconds,
    quarter_mile_trap_speed_mph, rollout_seconds, source_description
)
SELECT id, 4.9, 13.5, 106, 0.3,
       'Car and Driver instrumented test; result omits rollout'
FROM vehicle_trims WHERE model_year = 2023 AND trim_name = 'Type R (6MT)'
UNION ALL
SELECT id, 3.9, 12.5, 111, 0.2,
       'Car and Driver Euro-spec instrumented test; result omits rollout'
FROM vehicle_trims WHERE model_year = 2022 AND trim_name = 'Golf R (7DSG)';

INSERT INTO vehicle_data_sources (provider_name, source_url, license_notes) VALUES
    ('Honda Civic Type R specifications brochure',
     'https://d31sro4iz4ob5n.cloudfront.net/upload/car/civic-type-r-2023/brochure/civic-type-r-2023-lhd-brochure_en.pdf',
     'Published manufacturer specifications; retained as provenance only'),
    ('Volkswagen Golf R technical specifications',
     'https://downloads.regulations.gov/NHTSA-2023-0022-0074/attachment_2.pdf',
     'Published manufacturer technical data; retained as provenance only');

INSERT INTO vehicle_trim_sources (
    vehicle_trim_id, source_id, data_status, confidence_level, notes
)
SELECT trim.id, source.id, 'PUBLISHED', 'HIGH',
       'Published identity, powertrain, dimensions, tires, and gearing; simulation assumptions are separately documented'
FROM vehicle_trims trim
JOIN vehicle_generations generation ON generation.id = trim.generation_id
JOIN vehicle_models model ON model.id = generation.vehicle_model_id
JOIN vehicle_data_sources source
  ON (model.name = 'Civic' AND source.provider_name = 'Honda Civic Type R specifications brochure')
  OR (model.name = 'Golf' AND source.provider_name = 'Volkswagen Golf R technical specifications')
WHERE (trim.model_year = 2023 AND trim.trim_name = 'Type R (6MT)')
   OR (trim.model_year = 2022 AND trim.trim_name = 'Golf R (7DSG)');

INSERT INTO vehicle_trim_sources (
    vehicle_trim_id, source_id, data_status, confidence_level, notes
)
SELECT trim.id, source.id, 'MANUAL', 'MEDIUM',
       'Simulation-ready validation fixture; estimated fields are listed in simulation-engine/REAL_VEHICLE_DATA.md'
FROM vehicle_trims trim
CROSS JOIN vehicle_data_sources source
WHERE source.provider_name = 'Project validation dataset'
  AND ((trim.model_year = 2023 AND trim.trim_name = 'Type R (6MT)')
    OR (trim.model_year = 2022 AND trim.trim_name = 'Golf R (7DSG)'));

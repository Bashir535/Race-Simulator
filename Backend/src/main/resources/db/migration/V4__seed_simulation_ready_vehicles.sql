-- V2 used performance variants as model names. Normalize the catalog so the
-- trim/generation tables introduced in V3 carry that detail instead.
UPDATE vehicle_models SET name = 'Corvette' WHERE name = 'Corvette Stingray';
UPDATE vehicle_models SET name = 'Civic' WHERE name = 'Civic Type R';
UPDATE vehicle_models SET name = 'Golf' WHERE name = 'Golf R';

INSERT INTO vehicle_generations (
    vehicle_model_id, code, body_style, production_start_year, production_end_year
)
SELECT id, generation, body_style, production_start_year, production_end_year
FROM vehicle_models
WHERE (name = 'Mustang' AND generation = 'S650')
   OR (name = 'Corvette' AND generation = 'C8');

INSERT INTO engines (
    name, displacement_liters, configuration, aspiration, fuel_type,
    rated_horsepower, peak_horsepower_rpm, rated_torque_nm, peak_torque_rpm,
    idle_rpm, shift_rpm, redline_rpm
) VALUES
    ('Ford Coyote 5.0L V8', 5.00, 'V8', 'Naturally Aspirated', 'Gasoline',
     486, 7250, 566.740, 4900, 750, 7250, 7500),
    ('Chevrolet LT2 6.2L V8', 6.20, 'V8', 'Naturally Aspirated', 'Gasoline',
     495, 6450, 637.234, 5150, 650, 6450, 6500);

INSERT INTO torque_curve_points (engine_id, rpm, torque_nm)
SELECT id, point.rpm, point.torque_nm
FROM engines
CROSS JOIN LATERAL (VALUES
    (750.0, 298.280), (2000.0, 440.641), (3500.0, 528.769),
    (4900.0, 566.740), (6000.0, 549.106), (7250.0, 477.250),
    (7500.0, 447.420)
) AS point(rpm, torque_nm)
WHERE name = 'Ford Coyote 5.0L V8';

INSERT INTO torque_curve_points (engine_id, rpm, torque_nm)
SELECT id, point.rpm, point.torque_nm
FROM engines
CROSS JOIN LATERAL (VALUES
    (650.0, 406.745), (2000.0, 555.885), (3500.0, 616.897),
    (5150.0, 637.234), (6000.0, 596.560), (6450.0, 546.395),
    (6500.0, 535.548)
) AS point(rpm, torque_nm)
WHERE name = 'Chevrolet LT2 6.2L V8';

INSERT INTO transmissions (
    name, transmission_type, number_of_gears, final_drive_ratio,
    shift_duration_seconds, drivetrain_efficiency
) VALUES
    ('Ford 6-Speed Manual', 'MANUAL', 6, 3.73, 0.20, 0.88),
    ('Tremec 8-Speed Dual-Clutch', 'DUAL_CLUTCH', 8, 5.17, 0.08, 0.90);

INSERT INTO gear_ratios (transmission_id, gear_number, ratio)
SELECT id, gear.gear_number, gear.ratio
FROM transmissions
CROSS JOIN LATERAL (VALUES
    (1, 3.237), (2, 2.104), (3, 1.422), (4, 1.000), (5, 0.814), (6, 0.622)
) AS gear(gear_number, ratio)
WHERE name = 'Ford 6-Speed Manual';

INSERT INTO gear_ratios (transmission_id, gear_number, ratio)
SELECT id, gear.gear_number, gear.ratio
FROM transmissions
CROSS JOIN LATERAL (VALUES
    (1, 2.905), (2, 1.759), (3, 1.220), (4, 0.878),
    (5, 0.653), (6, 0.508), (7, 0.397), (8, 0.329)
) AS gear(gear_number, ratio)
WHERE name = 'Tremec 8-Speed Dual-Clutch';

INSERT INTO vehicle_trims (
    generation_id, model_year, trim_name, engine_id, transmission_id, drivetrain,
    original_msrp_usd, tire_description, city_mpg, highway_mpg, combined_mpg,
    is_popular
)
SELECT generation.id, 2024, 'GT Performance Package (6MT)', engine.id,
       transmission.id, 'RWD', 46555, '275/40R19 rear', 15, 24, 18, TRUE
FROM vehicle_generations generation
JOIN vehicle_models model ON model.id = generation.vehicle_model_id
JOIN engines engine ON engine.name = 'Ford Coyote 5.0L V8'
JOIN transmissions transmission ON transmission.name = 'Ford 6-Speed Manual'
WHERE model.name = 'Mustang' AND generation.code = 'S650';

INSERT INTO vehicle_trims (
    generation_id, model_year, trim_name, engine_id, transmission_id, drivetrain,
    original_msrp_usd, tire_description, city_mpg, highway_mpg, combined_mpg,
    is_popular
)
SELECT generation.id, 2020, 'Stingray Z51 (8DCT)', engine.id,
       transmission.id, 'RWD', 64495, '305/30R20 rear', 15, 27, 19, TRUE
FROM vehicle_generations generation
JOIN vehicle_models model ON model.id = generation.vehicle_model_id
JOIN engines engine ON engine.name = 'Chevrolet LT2 6.2L V8'
JOIN transmissions transmission ON transmission.name = 'Tremec 8-Speed Dual-Clutch'
WHERE model.name = 'Corvette' AND generation.code = 'C8';

INSERT INTO vehicle_specifications (
    vehicle_trim_id, mass_kg, static_front_weight_fraction, wheelbase_meters,
    center_of_gravity_height_meters, wheel_radius_meters, drag_coefficient,
    frontal_area_square_meters, rolling_resistance_coefficient,
    tire_friction_coefficient, launch_rpm, launch_engagement_duration_seconds,
    initial_torque_transfer_fraction, launch_control_enabled
)
SELECT id, 1790.330, 0.55, 2.7178, 0.55, 0.3523, 0.377, 2.20,
       0.015, 1.15, 3500, 0.45, 0.55, FALSE
FROM vehicle_trims WHERE model_year = 2024 AND trim_name = 'GT Performance Package (6MT)';

INSERT INTO vehicle_specifications (
    vehicle_trim_id, mass_kg, static_front_weight_fraction, wheelbase_meters,
    center_of_gravity_height_meters, wheel_radius_meters, drag_coefficient,
    frontal_area_square_meters, rolling_resistance_coefficient,
    tire_friction_coefficient, launch_rpm, launch_engagement_duration_seconds,
    initial_torque_transfer_fraction, launch_control_enabled
)
SELECT id, 1654.252, 0.40, 2.7229, 0.48, 0.3455, 0.322, 2.075,
       0.015, 1.20, 3500, 0.35, 0.55, TRUE
FROM vehicle_trims WHERE model_year = 2020 AND trim_name = 'Stingray Z51 (8DCT)';

INSERT INTO published_performance (
    vehicle_trim_id, zero_to_sixty_seconds, quarter_mile_seconds,
    quarter_mile_trap_speed_mph, rollout_seconds, source_description
)
SELECT id, 4.2, 12.5, 114, 0.3,
       'Car and Driver instrumented test; result omits rollout'
FROM vehicle_trims WHERE model_year = 2024 AND trim_name = 'GT Performance Package (6MT)'
UNION ALL
SELECT id, 2.8, 11.2, 122, 0.2,
       'Car and Driver instrumented test; result omits rollout'
FROM vehicle_trims WHERE model_year = 2020 AND trim_name = 'Stingray Z51 (8DCT)';

INSERT INTO vehicle_data_sources (provider_name, source_url, license_notes)
VALUES ('Project validation dataset', NULL,
        'Development fixture derived from documented published specifications; verify before production use');

INSERT INTO vehicle_trim_sources (
    vehicle_trim_id, source_id, data_status, confidence_level, notes
)
SELECT trim.id, source.id, 'MANUAL', 'MEDIUM',
       'Simulation-ready development fixture; see simulation-engine/REAL_VEHICLE_DATA.md'
FROM vehicle_trims trim
CROSS JOIN vehicle_data_sources source
WHERE source.provider_name = 'Project validation dataset';

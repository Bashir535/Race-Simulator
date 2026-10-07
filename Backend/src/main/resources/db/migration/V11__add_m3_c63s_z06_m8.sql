-- Four initial simulation-ready stock configurations. Not accuracy-certified.
-- Sources, market differences and estimated physics inputs: simulation-engine/REAL_VEHICLE_DATA.md.
-- Do not alter older applied migrations. No existing vehicle data is changed here.

-- 2021 BMW M3 Competition RWD (8AT)
INSERT INTO makes (name, country) VALUES ('BMW', 'Germany') ON CONFLICT (name) DO NOTHING;
INSERT INTO vehicle_models (make_id, name, generation, body_style, production_start_year)
SELECT id, 'M3', 'G80', 'Sedan', 2021 FROM makes WHERE name='BMW';
INSERT INTO vehicle_generations (vehicle_model_id, code, body_style, production_start_year)
SELECT model.id, 'G80', 'Sedan', 2021 FROM vehicle_models model JOIN makes m ON m.id=model.make_id
WHERE m.name='BMW' AND model.name='M3' AND model.generation='G80';
INSERT INTO engines (name, displacement_liters, configuration, aspiration, fuel_type, rated_horsepower, peak_horsepower_rpm, rated_torque_nm, peak_torque_rpm, idle_rpm, shift_rpm, redline_rpm)
VALUES ('BMW S58 3.0L Twin-Turbo I6',3,'Inline-6','Twin-turbocharged','Gasoline',503,6250,649.439,2750,800,7000,7200);
-- Intermediate curve points are estimates anchored to published peak ratings.
INSERT INTO torque_curve_points (engine_id,rpm,torque_nm)
SELECT e.id,p.rpm,p.torque FROM engines e CROSS JOIN LATERAL (VALUES (800,230),(1500,440),(2750,649.439),(5000,649.439),(5500,649.439),(6250,573.095),(7200,450)) p(rpm,torque) WHERE e.name='BMW S58 3.0L Twin-Turbo I6';
INSERT INTO transmissions (name,transmission_type,number_of_gears,final_drive_ratio,shift_duration_seconds,drivetrain_efficiency)
VALUES ('BMW M3 M Steptronic 8-Speed','TORQUE_CONVERTER_AUTOMATIC',8,3.15,0.1,0.89);
INSERT INTO gear_ratios (transmission_id,gear_number,ratio)
SELECT t.id,g.n,g.ratio FROM transmissions t CROSS JOIN LATERAL (VALUES (1,5),(2,3.2),(3,2.14),(4,1.72),(5,1.31),(6,1),(7,0.82),(8,0.64)) g(n,ratio) WHERE t.name='BMW M3 M Steptronic 8-Speed';
INSERT INTO vehicle_trims (generation_id,model_year,trim_name,engine_id,transmission_id,drivetrain,original_msrp_usd,tire_description,city_mpg,highway_mpg,combined_mpg,is_popular)
SELECT g.id,2021,'Competition RWD (8AT)',e.id,t.id,'RWD',72800,'Front 275/40ZR18; rear 285/35ZR19 (standard)',16,23,19,TRUE
FROM vehicle_generations g JOIN vehicle_models model ON model.id=g.vehicle_model_id JOIN makes m ON m.id=model.make_id
CROSS JOIN engines e CROSS JOIN transmissions t
WHERE m.name='BMW' AND model.name='M3' AND g.code='G80' AND e.name='BMW S58 3.0L Twin-Turbo I6' AND t.name='BMW M3 M Steptronic 8-Speed';
INSERT INTO vehicle_specifications (vehicle_trim_id,mass_kg,static_front_weight_fraction,wheelbase_meters,center_of_gravity_height_meters,wheel_radius_meters,drag_coefficient,frontal_area_square_meters,rolling_resistance_coefficient,tire_friction_coefficient,launch_rpm,launch_engagement_duration_seconds,initial_torque_transfer_fraction,launch_control_enabled)
SELECT id,1764.474,0.531,2.8575,0.52,0.34105,0.34,2.25,0.015,1.15,3000,0.35,0.55,TRUE FROM vehicle_trims WHERE model_year=2021 AND trim_name='Competition RWD (8AT)';
INSERT INTO vehicle_data_sources (provider_name,source_url,license_notes)
VALUES ('2021 BMW M3 factory specifications','https://www.press.bmwgroup.com/usa/article/detail/T0317577EN_US/the-new-2021-bmw-m3-sedan-and-m4-coupe?language=en_US','Public specifications; reference link only');
INSERT INTO vehicle_trim_sources (vehicle_trim_id,source_id,data_status,confidence_level,notes)
SELECT v.id,s.id,'PUBLISHED','HIGH','Factory identity and powertrain; estimated inputs and market/test differences documented separately'
FROM vehicle_trims v CROSS JOIN vehicle_data_sources s WHERE v.model_year=2021 AND v.trim_name='Competition RWD (8AT)' AND s.source_url='https://www.press.bmwgroup.com/usa/article/detail/T0317577EN_US/the-new-2021-bmw-m3-sedan-and-m4-coupe?language=en_US';
INSERT INTO vehicle_trim_sources (vehicle_trim_id,source_id,data_status,confidence_level,notes)
SELECT v.id,s.id,'ESTIMATED','LOW','Initial physics fixture: torque shape, launch, shift duration, losses, grip, CG and some aerodynamic inputs estimated. Not independently calibrated; see REAL_VEHICLE_DATA.md.'
FROM vehicle_trims v CROSS JOIN vehicle_data_sources s WHERE v.model_year=2021 AND v.trim_name='Competition RWD (8AT)' AND s.provider_name='Project validation dataset';
INSERT INTO published_performance (vehicle_trim_id,zero_to_sixty_seconds,quarter_mile_seconds,quarter_mile_trap_speed_mph,rollout_seconds,source_description)
SELECT id,3.5,11.6,124,0.2,'Car and Driver 2021 M3 Competition test; omits 0.2 s rollout; tested 3820 lb versus catalog factory 3890 lb' FROM vehicle_trims WHERE model_year=2021 AND trim_name='Competition RWD (8AT)';
INSERT INTO vehicle_data_sources (provider_name,source_url,license_notes) VALUES ('2021 BMW M3 instrumented reference','https://www.caranddriver.com/reviews/a37286079/2021-bmw-m3-competition-by-the-numbers/','Published test reference; not an engine output');
INSERT INTO vehicle_trim_sources (vehicle_trim_id,source_id,data_status,confidence_level,notes)
SELECT v.id,s.id,'MEASURED','MEDIUM','Car and Driver 2021 M3 Competition test; omits 0.2 s rollout; tested 3820 lb versus catalog factory 3890 lb' FROM vehicle_trims v CROSS JOIN vehicle_data_sources s WHERE v.model_year=2021 AND v.trim_name='Competition RWD (8AT)' AND s.source_url='https://www.caranddriver.com/reviews/a37286079/2021-bmw-m3-competition-by-the-numbers/';

-- 2019 Mercedes-AMG C63 S Sedan (9-speed MCT)
INSERT INTO makes (name, country) VALUES ('Mercedes-AMG', 'Germany') ON CONFLICT (name) DO NOTHING;
INSERT INTO vehicle_models (make_id, name, generation, body_style, production_start_year)
SELECT id, 'C63', 'W205', 'Sedan', NULL FROM makes WHERE name='Mercedes-AMG';
INSERT INTO vehicle_generations (vehicle_model_id, code, body_style, production_start_year)
SELECT model.id, 'W205', 'Sedan', NULL FROM vehicle_models model JOIN makes m ON m.id=model.make_id
WHERE m.name='Mercedes-AMG' AND model.name='C63' AND model.generation='W205';
INSERT INTO engines (name, displacement_liters, configuration, aspiration, fuel_type, rated_horsepower, peak_horsepower_rpm, rated_torque_nm, peak_torque_rpm, idle_rpm, shift_rpm, redline_rpm)
VALUES ('Mercedes-AMG M177 4.0L Twin-Turbo V8',4,'V8','Twin-turbocharged','Gasoline',503,6250,699.592,2000,800,6800,7000);
-- Intermediate curve points are estimates anchored to published peak ratings.
INSERT INTO torque_curve_points (engine_id,rpm,torque_nm)
SELECT e.id,p.rpm,p.torque FROM engines e CROSS JOIN LATERAL (VALUES (800,260),(1500,500),(2000,699.592),(4500,699.592),(5500,651.244),(6250,573.095),(7000,475)) p(rpm,torque) WHERE e.name='Mercedes-AMG M177 4.0L Twin-Turbo V8';
INSERT INTO transmissions (name,transmission_type,number_of_gears,final_drive_ratio,shift_duration_seconds,drivetrain_efficiency)
VALUES ('AMG SPEEDSHIFT MCT 9-Speed','WET_CLUTCH_AUTOMATIC',9,2.82,0.1,0.89);
INSERT INTO gear_ratios (transmission_id,gear_number,ratio)
SELECT t.id,g.n,g.ratio FROM transmissions t CROSS JOIN LATERAL (VALUES (1,5.35),(2,3.24),(3,2.25),(4,1.64),(5,1.21),(6,1),(7,0.86),(8,0.72),(9,0.6)) g(n,ratio) WHERE t.name='AMG SPEEDSHIFT MCT 9-Speed';
INSERT INTO vehicle_trims (generation_id,model_year,trim_name,engine_id,transmission_id,drivetrain,original_msrp_usd,tire_description,city_mpg,highway_mpg,combined_mpg,is_popular)
SELECT g.id,2019,'S Sedan (9-speed MCT)',e.id,t.id,'RWD',74600,'Front 245/35ZR19; rear 265/35ZR19; Michelin Pilot Super Sport (tested)',18,27,21,TRUE
FROM vehicle_generations g JOIN vehicle_models model ON model.id=g.vehicle_model_id JOIN makes m ON m.id=model.make_id
CROSS JOIN engines e CROSS JOIN transmissions t
WHERE m.name='Mercedes-AMG' AND model.name='C63' AND g.code='W205' AND e.name='Mercedes-AMG M177 4.0L Twin-Turbo V8' AND t.name='AMG SPEEDSHIFT MCT 9-Speed';
INSERT INTO vehicle_specifications (vehicle_trim_id,mass_kg,static_front_weight_fraction,wheelbase_meters,center_of_gravity_height_meters,wheel_radius_meters,drag_coefficient,frontal_area_square_meters,rolling_resistance_coefficient,tire_friction_coefficient,launch_rpm,launch_engagement_duration_seconds,initial_torque_transfer_fraction,launch_control_enabled)
SELECT id,1808.472,0.54,2.83972,0.53,0.33405,0.32,2.2,0.015,1.15,3000,0.35,0.55,TRUE FROM vehicle_trims WHERE model_year=2019 AND trim_name='S Sedan (9-speed MCT)';
INSERT INTO vehicle_data_sources (provider_name,source_url,license_notes)
VALUES ('2019 Mercedes-AMG C63 factory specifications','https://media.mbusa.com/news/2019-mercedes-amg-c-63-and-c-63-s-sedan-specifications','Public specifications; reference link only');
INSERT INTO vehicle_trim_sources (vehicle_trim_id,source_id,data_status,confidence_level,notes)
SELECT v.id,s.id,'PUBLISHED','HIGH','Factory identity and powertrain; estimated inputs and market/test differences documented separately'
FROM vehicle_trims v CROSS JOIN vehicle_data_sources s WHERE v.model_year=2019 AND v.trim_name='S Sedan (9-speed MCT)' AND s.source_url='https://media.mbusa.com/news/2019-mercedes-amg-c-63-and-c-63-s-sedan-specifications';
INSERT INTO vehicle_trim_sources (vehicle_trim_id,source_id,data_status,confidence_level,notes)
SELECT v.id,s.id,'ESTIMATED','LOW','Initial physics fixture: torque shape, launch, shift duration, losses, grip, CG and some aerodynamic inputs estimated. Not independently calibrated; see REAL_VEHICLE_DATA.md.'
FROM vehicle_trims v CROSS JOIN vehicle_data_sources s WHERE v.model_year=2019 AND v.trim_name='S Sedan (9-speed MCT)' AND s.provider_name='Project validation dataset';
INSERT INTO published_performance (vehicle_trim_id,zero_to_sixty_seconds,quarter_mile_seconds,quarter_mile_trap_speed_mph,rollout_seconds,source_description)
SELECT id,3.7,11.9,122,NULL,'Car and Driver 2019 C63 S sedan instrumented test; rollout not specified' FROM vehicle_trims WHERE model_year=2019 AND trim_name='S Sedan (9-speed MCT)';
INSERT INTO vehicle_data_sources (provider_name,source_url,license_notes) VALUES ('2019 Mercedes-AMG C63 instrumented reference','https://www.caranddriver.com/reviews/a22174935/2019-mercedes-amg-c63-first-drive-review/','Published test reference; not an engine output');
INSERT INTO vehicle_trim_sources (vehicle_trim_id,source_id,data_status,confidence_level,notes)
SELECT v.id,s.id,'MEASURED','MEDIUM','Car and Driver 2019 C63 S sedan instrumented test; rollout not specified' FROM vehicle_trims v CROSS JOIN vehicle_data_sources s WHERE v.model_year=2019 AND v.trim_name='S Sedan (9-speed MCT)' AND s.source_url='https://www.caranddriver.com/reviews/a22174935/2019-mercedes-amg-c63-first-drive-review/';

-- 2019 Chevrolet Corvette Z06 Coupe (8AT, standard aero)
INSERT INTO makes (name, country) VALUES ('Chevrolet', 'United States') ON CONFLICT (name) DO NOTHING;
INSERT INTO vehicle_models (make_id, name, generation, body_style, production_start_year)
SELECT id, 'Corvette', 'C7', 'Coupe', NULL FROM makes WHERE name='Chevrolet';
INSERT INTO vehicle_generations (vehicle_model_id, code, body_style, production_start_year)
SELECT model.id, 'C7', 'Coupe', NULL FROM vehicle_models model JOIN makes m ON m.id=model.make_id
WHERE m.name='Chevrolet' AND model.name='Corvette' AND model.generation='C7';
INSERT INTO engines (name, displacement_liters, configuration, aspiration, fuel_type, rated_horsepower, peak_horsepower_rpm, rated_torque_nm, peak_torque_rpm, idle_rpm, shift_rpm, redline_rpm)
VALUES ('Chevrolet LT4 6.2L Supercharged V8',6.2,'V8','Supercharged','Gasoline',650,6400,881.282,3600,800,6500,6600);
-- Intermediate curve points are estimates anchored to published peak ratings.
INSERT INTO torque_curve_points (engine_id,rpm,torque_nm)
SELECT e.id,p.rpm,p.torque FROM engines e CROSS JOIN LATERAL (VALUES (800,330),(1500,600),(2500,790),(3600,881.282),(4500,870),(5500,810),(6400,723.26),(6600,690)) p(rpm,torque) WHERE e.name='Chevrolet LT4 6.2L Supercharged V8';
INSERT INTO transmissions (name,transmission_type,number_of_gears,final_drive_ratio,shift_duration_seconds,drivetrain_efficiency)
VALUES ('Chevrolet 8L90 8-Speed Automatic','TORQUE_CONVERTER_AUTOMATIC',8,2.41,0.1,0.89);
INSERT INTO gear_ratios (transmission_id,gear_number,ratio)
SELECT t.id,g.n,g.ratio FROM transmissions t CROSS JOIN LATERAL (VALUES (1,4.56),(2,2.97),(3,2.08),(4,1.69),(5,1.27),(6,1),(7,0.85),(8,0.65)) g(n,ratio) WHERE t.name='Chevrolet 8L90 8-Speed Automatic';
INSERT INTO vehicle_trims (generation_id,model_year,trim_name,engine_id,transmission_id,drivetrain,original_msrp_usd,tire_description,city_mpg,highway_mpg,combined_mpg,is_popular)
SELECT g.id,2019,'Z06 Coupe (8AT, standard aero)',e.id,t.id,'RWD',NULL,'Front 285/30ZR19; rear 335/25ZR20; Michelin Pilot Super Sport',NULL,NULL,NULL,TRUE
FROM vehicle_generations g JOIN vehicle_models model ON model.id=g.vehicle_model_id JOIN makes m ON m.id=model.make_id
CROSS JOIN engines e CROSS JOIN transmissions t
WHERE m.name='Chevrolet' AND model.name='Corvette' AND g.code='C7' AND e.name='Chevrolet LT4 6.2L Supercharged V8' AND t.name='Chevrolet 8L90 8-Speed Automatic';
INSERT INTO vehicle_specifications (vehicle_trim_id,mass_kg,static_front_weight_fraction,wheelbase_meters,center_of_gravity_height_meters,wheel_radius_meters,drag_coefficient,frontal_area_square_meters,rolling_resistance_coefficient,tire_friction_coefficient,launch_rpm,launch_engagement_duration_seconds,initial_torque_transfer_fraction,launch_control_enabled)
SELECT id,1659,0.5,2.71,0.46,0.33775,0.36,2.05,0.015,1.15,2500,0.35,0.55,TRUE FROM vehicle_trims WHERE model_year=2019 AND trim_name='Z06 Coupe (8AT, standard aero)';
INSERT INTO vehicle_data_sources (provider_name,source_url,license_notes)
VALUES ('2019 Chevrolet Corvette factory specifications','https://media.cadillac.com/content/dam/Media/documents/INTL/chevrolet/2019/vehicles/corvette-z06/Tech-Data-Chevrolet-Corvette-Z06.pdf','Public specifications; reference link only');
INSERT INTO vehicle_trim_sources (vehicle_trim_id,source_id,data_status,confidence_level,notes)
SELECT v.id,s.id,'PUBLISHED','HIGH','Factory identity and powertrain; estimated inputs and market/test differences documented separately'
FROM vehicle_trims v CROSS JOIN vehicle_data_sources s WHERE v.model_year=2019 AND v.trim_name='Z06 Coupe (8AT, standard aero)' AND s.source_url='https://media.cadillac.com/content/dam/Media/documents/INTL/chevrolet/2019/vehicles/corvette-z06/Tech-Data-Chevrolet-Corvette-Z06.pdf';
INSERT INTO vehicle_trim_sources (vehicle_trim_id,source_id,data_status,confidence_level,notes)
SELECT v.id,s.id,'ESTIMATED','LOW','Initial physics fixture: torque shape, launch, shift duration, losses, grip, CG and some aerodynamic inputs estimated. Not independently calibrated; see REAL_VEHICLE_DATA.md.'
FROM vehicle_trims v CROSS JOIN vehicle_data_sources s WHERE v.model_year=2019 AND v.trim_name='Z06 Coupe (8AT, standard aero)' AND s.provider_name='Project validation dataset';

-- 2022 BMW M8 Competition Coupe (8AT)
INSERT INTO makes (name, country) VALUES ('BMW', 'Germany') ON CONFLICT (name) DO NOTHING;
INSERT INTO vehicle_models (make_id, name, generation, body_style, production_start_year)
SELECT id, 'M8', 'F92', 'Coupe', NULL FROM makes WHERE name='BMW';
INSERT INTO vehicle_generations (vehicle_model_id, code, body_style, production_start_year)
SELECT model.id, 'F92', 'Coupe', NULL FROM vehicle_models model JOIN makes m ON m.id=model.make_id
WHERE m.name='BMW' AND model.name='M8' AND model.generation='F92';
INSERT INTO engines (name, displacement_liters, configuration, aspiration, fuel_type, rated_horsepower, peak_horsepower_rpm, rated_torque_nm, peak_torque_rpm, idle_rpm, shift_rpm, redline_rpm)
VALUES ('BMW S63 4.4L Twin-Turbo V8',4.4,'V8','Twin-turbocharged','Gasoline',617,6000,749.771,1800,800,7000,7200);
-- Intermediate curve points are estimates anchored to published peak ratings.
INSERT INTO torque_curve_points (engine_id,rpm,torque_nm)
SELECT e.id,p.rpm,p.torque FROM engines e CROSS JOIN LATERAL (VALUES (800,280),(1500,580),(1800,749.771),(5000,749.771),(5860,749.771),(6000,732.255),(6500,665),(7200,560)) p(rpm,torque) WHERE e.name='BMW S63 4.4L Twin-Turbo V8';
INSERT INTO transmissions (name,transmission_type,number_of_gears,final_drive_ratio,shift_duration_seconds,drivetrain_efficiency)
VALUES ('BMW M8 M Steptronic 8-Speed','TORQUE_CONVERTER_AUTOMATIC',8,3.154,0.1,0.86);
INSERT INTO gear_ratios (transmission_id,gear_number,ratio)
SELECT t.id,g.n,g.ratio FROM transmissions t CROSS JOIN LATERAL (VALUES (1,5),(2,3.2),(3,2.143),(4,1.72),(5,1.313),(6,1),(7,0.823),(8,0.64)) g(n,ratio) WHERE t.name='BMW M8 M Steptronic 8-Speed';
INSERT INTO vehicle_trims (generation_id,model_year,trim_name,engine_id,transmission_id,drivetrain,original_msrp_usd,tire_description,city_mpg,highway_mpg,combined_mpg,is_popular)
SELECT g.id,2022,'Competition Coupe (8AT)',e.id,t.id,'AWD',130000,'Front 275/35ZR20; rear 285/35ZR20',NULL,NULL,NULL,TRUE
FROM vehicle_generations g JOIN vehicle_models model ON model.id=g.vehicle_model_id JOIN makes m ON m.id=model.make_id
CROSS JOIN engines e CROSS JOIN transmissions t
WHERE m.name='BMW' AND model.name='M8' AND g.code='F92' AND e.name='BMW S63 4.4L Twin-Turbo V8' AND t.name='BMW M8 M Steptronic 8-Speed';
INSERT INTO vehicle_specifications (vehicle_trim_id,mass_kg,static_front_weight_fraction,wheelbase_meters,center_of_gravity_height_meters,wheel_radius_meters,drag_coefficient,frontal_area_square_meters,rolling_resistance_coefficient,tire_friction_coefficient,launch_rpm,launch_engagement_duration_seconds,initial_torque_transfer_fraction,launch_control_enabled)
SELECT id,1948.179,0.54,2.827,0.52,0.35375,0.33,2.25,0.015,1.15,3000,0.25,0.6,TRUE FROM vehicle_trims WHERE model_year=2022 AND trim_name='Competition Coupe (8AT)';
INSERT INTO vehicle_data_sources (provider_name,source_url,license_notes)
VALUES ('2022 BMW M8 factory specifications','https://www.press.bmwgroup.com/global/article/attachment/T0364657EN/519030','Public specifications; reference link only');
INSERT INTO vehicle_trim_sources (vehicle_trim_id,source_id,data_status,confidence_level,notes)
SELECT v.id,s.id,'PUBLISHED','HIGH','Factory identity and powertrain; estimated inputs and market/test differences documented separately'
FROM vehicle_trims v CROSS JOIN vehicle_data_sources s WHERE v.model_year=2022 AND v.trim_name='Competition Coupe (8AT)' AND s.source_url='https://www.press.bmwgroup.com/global/article/attachment/T0364657EN/519030';
INSERT INTO vehicle_trim_sources (vehicle_trim_id,source_id,data_status,confidence_level,notes)
SELECT v.id,s.id,'ESTIMATED','LOW','Initial physics fixture: torque shape, launch, shift duration, losses, grip, CG and some aerodynamic inputs estimated. Not independently calibrated; see REAL_VEHICLE_DATA.md.'
FROM vehicle_trims v CROSS JOIN vehicle_data_sources s WHERE v.model_year=2022 AND v.trim_name='Competition Coupe (8AT)' AND s.provider_name='Project validation dataset';


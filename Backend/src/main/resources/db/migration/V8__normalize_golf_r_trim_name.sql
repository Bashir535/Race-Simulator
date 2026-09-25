-- The model already supplies "Golf"; keep the trim suffix from duplicating it
-- in display names such as "Volkswagen Golf Golf R".
UPDATE vehicle_trims
SET trim_name = 'R (7DSG)'
WHERE model_year = 2022
  AND trim_name = 'Golf R (7DSG)';

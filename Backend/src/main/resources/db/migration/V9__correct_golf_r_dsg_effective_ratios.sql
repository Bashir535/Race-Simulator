-- The DQ381 uses two final-drive paths. V7 grouped the final drives by gear
-- range, which made the effective ratios non-sequential and produced
-- unrealistic roll-race shift speeds. Store the correct overall wheel ratios
-- because the simulation model accepts one final-drive value per vehicle.
UPDATE gear_ratios AS ratio
SET ratio = CASE ratio.gear_number
    WHEN 1 THEN 14.2593 -- 3.19 * 4.47
    WHEN 2 THEN  9.0750 -- 2.75 * 3.30
    WHEN 3 THEN  6.2700 -- 1.90 * 3.30
    WHEN 4 THEN  4.6488 -- 1.04 * 4.47
    WHEN 5 THEN  3.5313 -- 0.79 * 4.47
    WHEN 6 THEN  2.8380 -- 0.86 * 3.30
    WHEN 7 THEN  2.1780 -- 0.66 * 3.30
END
FROM transmissions AS transmission
WHERE ratio.transmission_id = transmission.id
  AND transmission.name = 'Volkswagen 7-Speed DSG'
  AND ratio.gear_number BETWEEN 1 AND 7;

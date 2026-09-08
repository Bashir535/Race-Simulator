INSERT INTO makes (name, country) VALUES
    ('Ford', 'United States'),
    ('Honda', 'Japan'),
    ('Volkswagen', 'Germany'),
    ('Chevrolet', 'United States');

INSERT INTO vehicle_models (
    make_id,
    name,
    generation,
    body_style,
    production_start_year,
    production_end_year
)
SELECT id, 'Mustang', 'S650', 'Coupe', 2024, CAST(NULL AS SMALLINT) FROM makes WHERE name = 'Ford'

UNION ALL

SELECT id, 'Civic Type R', 'FL5', 'Hatchback', 2023, CAST(NULL AS SMALLINT) FROM makes WHERE name = 'Honda'

UNION ALL

SELECT id, 'Golf R', 'Mk8', 'Hatchback', 2022, CAST(NULL AS SMALLINT) FROM makes WHERE name = 'Volkswagen'

UNION ALL

SELECT id, 'Corvette Stingray', 'C8', 'Coupe', 2020, CAST(NULL AS SMALLINT) FROM makes WHERE name = 'Chevrolet';
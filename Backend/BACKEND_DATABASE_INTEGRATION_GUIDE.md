# Race Simulator Backend and Database Integration Guide

## 1. Purpose of this increment

This increment connects the deterministic Java simulation engine to a Spring
Boot API backed by PostgreSQL. It establishes the first complete application
path that the frontend can call:

```text
Frontend or API client
        |
        v
Spring Boot REST controller
        |
        v
Catalog/race service layer
        |
        +----> PostgreSQL vehicle catalog
        |
        +----> Database-to-engine mapper
                    |
                    v
          Java simulation engine
                    |
                    v
         Race results and telemetry
```

The simulation engine remains responsible for race outcomes. The backend does
not substitute database values or AI-generated estimates for the engine's
deterministic physics calculations.

## 2. What is working now

The system can now:

- start PostgreSQL locally with Docker Compose;
- create and migrate the database using Flyway;
- store normalized vehicle, engine, transmission, torque-curve, and simulation data;
- retrieve vehicles through year, make, model, and trim catalog endpoints;
- retrieve popular vehicles and complete vehicle details;
- load two selected vehicle configurations from PostgreSQL;
- translate database entities into the simulation engine's `VehicleSpec` model;
- execute a standing-start or rolling race through an HTTP endpoint;
- return the winner, finish times, milestones, lead changes, and synchronized telemetry;
- validate requests and return consistent API error responses; and
- allow the frontend development server to call the API through configured CORS rules.

Authentication, saved garages, saved races, external vehicle-data ingestion,
vehicle modifications, and AI explanations are not part of this increment.

## 3. Maven module integration

The repository-level `pom.xml` is a Maven reactor containing:

1. `simulation-engine`
2. `Backend`

The backend declares the engine as a Maven dependency. This lets backend code
use engine models and invoke `RaceSimulator` directly while keeping the physics
code in its own module.

The reactor builds the engine before the backend:

```bash
cd "/path/to/Race Simulator"
mvn test
```

## 4. Database design

### Catalog identity

- `makes`: manufacturer identity and country.
- `vehicle_models`: model identity belonging to a make.
- `vehicle_generations`: platform/generation and production-year information.
- `vehicle_trims`: a specific model year, trim, engine, transmission, and drivetrain combination.

Model names were normalized so variants belong at the proper level. For
example, `Corvette` is the model, `C8` is the generation, and `Stingray Z51
(8DCT)` is the trim/configuration.

### Powertrain data

- `engines`: displacement, layout, aspiration, fuel, rated output, and RPM limits.
- `torque_curve_points`: RPM-to-torque samples used by the physics engine.
- `transmissions`: type, gear count, final drive, shift duration, and efficiency.
- `gear_ratios`: ordered ratio for every forward gear.

### Physics inputs

`vehicle_specifications` stores values required by the current simulation:

- mass;
- static front weight distribution;
- wheelbase;
- center-of-gravity height;
- driven-wheel radius;
- drag coefficient and frontal area;
- rolling-resistance coefficient;
- tire friction coefficient; and
- launch RPM, engagement time, initial torque transfer, and launch-control status.

Values use explicit SI-oriented column names such as `mass_kg` and
`wheelbase_meters` to reduce unit-conversion mistakes.

### Benchmarks and provenance

- `published_performance`: published 0-60, quarter-mile, trap-speed, and rollout values.
- `vehicle_data_sources`: provider, URL, retrieval time, and licensing notes.
- `vehicle_trim_sources`: source relationship, data status, confidence, and notes.

This separates simulation inputs from validation targets and provides a place
to identify measured, published, estimated, or manually entered values.

## 5. Flyway migrations

- `V1`: creates the initial makes and models foundation.
- `V2`: seeds the initial makes and models.
- `V3`: creates the complete simulation-ready normalized catalog.
- `V4`: normalizes model names and seeds two complete vehicle fixtures.
- `V5`: adds triggers that maintain `updated_at` values automatically.
- `V6`: aligns PostgreSQL measurement columns with Java `double`/`Double` mappings.

Flyway records applied migrations in `flyway_schema_history`. Existing migration
files should not be edited after they have been shared or applied by teammates;
future schema changes should be added as `V7`, `V8`, and so on.

## 6. Seeded vehicle configurations

The local development database currently contains two complete fixtures:

- 2024 Ford Mustang GT Performance Package with six-speed manual transmission.
- 2020 Chevrolet Corvette Stingray Z51 with eight-speed dual-clutch transmission.

They include torque curves, gear ratios, launch profiles, aerodynamic values,
traction inputs, published benchmarks, and source metadata. These are development
fixtures for integration and validation, not yet a production vehicle catalog.

## 7. Backend layers

### Controllers

`VehicleCatalogController` exposes read-only dealership/catalog operations.
`RaceController` exposes deterministic race execution.

Controllers validate HTTP input and delegate work. They do not contain physics
or database mapping logic.

### Services

`VehicleCatalogService` performs catalog queries and maps entities to API DTOs.

`RaceSimulationService`:

1. validates that two different vehicle configurations were selected;
2. retrieves both complete configurations;
3. creates a validated engine `RaceConfig`;
4. invokes the deterministic `RaceSimulator`; and
5. maps results into the stable frontend response shape.

Both services use read-only transactions so required child collections can be
loaded safely while the persistence context is open.

### Database-to-engine mapper

`VehicleSpecMapper` is the boundary between persistence and physics. It maps:

- database drivetrain strings to engine enums;
- transmission data and ordered gear ratios;
- launch settings;
- the ordered torque curve;
- physical dimensions and mass; and
- resistance, aerodynamic, and traction inputs.

The mapper prevents the simulation engine from depending on JPA or database
entities.

### Repositories

Spring Data repositories provide catalog filters and detailed trim retrieval.
Detailed retrieval eager-loads to-one relationships. Ordered child lists are
loaded separately inside the service transaction. This avoids Hibernate's
`MultipleBagFetchException` and prevents a large Cartesian-product query.

### Error and web configuration

`ApiExceptionHandler` provides consistent JSON errors with:

- timestamp;
- HTTP status;
- application error code;
- message;
- field errors;
- correlation ID; and
- retryable status.

`WebConfiguration` enables API calls from configured frontend origins. Local
development defaults to `http://localhost:5173`.

## 8. Current API contract

### Vehicle discovery

```text
GET /api/v1/vehicles/years
GET /api/v1/vehicles/makes
GET /api/v1/vehicles/makes?year=2024
GET /api/v1/vehicles/models?makeId={id}
GET /api/v1/vehicles/models?makeId={id}&year=2024
GET /api/v1/vehicles/trims?modelId={id}&year=2024
GET /api/v1/vehicles/popular
GET /api/v1/vehicles/{trimId}
```

The frontend should discover generated IDs through these endpoints rather than
assuming permanent numeric IDs.

### Race simulation

```text
POST /api/v1/races/simulate
```

Example quarter-mile request:

```json
{
  "vehicleAId": 1,
  "vehicleBId": 2,
  "race": {
    "goalType": "DISTANCE",
    "distanceMeters": 402.336,
    "startingSpeedMetersPerSecond": 0,
    "roadSurface": "PREPARED_DRAG_STRIP"
  }
}
```

The endpoint also supports a `SPEED` goal with
`targetSpeedMetersPerSecond`. Optional environment inputs include air
temperature, pressure, humidity, road temperature, grade, and headwind. When
the environment is omitted, the engine uses its standard conditions.

The response includes:

- simulation version and configuration snapshot;
- both vehicle results;
- winner and time margin;
- milestone times and speeds;
- lead changes and largest lead; and
- synchronized time-series frames containing speed, RPM, gear, acceleration,
  position, shifting state, and other engine telemetry.

## 9. Local development

### Requirements

- Java 21 or newer
- Maven 3.9 or newer
- Docker Desktop

### Start PostgreSQL

```bash
cd "/path/to/Race Simulator/Backend"
docker compose up -d
docker compose ps
```

Wait until `race-simulator-postgres` reports `healthy`.

### Start Spring Boot

Jenkins is installed locally on port 8080 on the current development machine,
so port 8081 avoids that conflict:

```bash
cd "/path/to/Race Simulator/Backend"
SERVER_PORT=8081 mvn spring-boot:run
```

The command must be run from `Backend`, where the Spring Boot Maven plugin is
configured. Keep that terminal open while testing the API.

### Verify the catalog

```bash
curl http://localhost:8081/api/v1/vehicles/popular
```

### Verify a complete race

```bash
curl -X POST http://localhost:8081/api/v1/races/simulate \
  -H "Content-Type: application/json" \
  -d '{
    "vehicleAId": 1,
    "vehicleBId": 2,
    "race": {
      "goalType": "DISTANCE",
      "distanceMeters": 402.336,
      "startingSpeedMetersPerSecond": 0,
      "roadSurface": "PREPARED_DRAG_STRIP"
    }
  }'
```

### Stop local services

Press `Control-C` in the Spring Boot terminal. PostgreSQL can remain running or
be stopped with:

```bash
docker compose down
```

The named Docker volume preserves database data unless explicitly removed.

## 10. Verification completed

The following were verified on September 16, 2026:

- PostgreSQL 17 started successfully through Docker Compose.
- All six Flyway migrations applied successfully to a clean PostgreSQL database.
- Hibernate schema validation passed.
- Spring Boot started successfully on port 8081.
- `GET /api/v1/vehicles/popular` returned both seeded vehicles.
- `POST /api/v1/races/simulate` returned HTTP 200.
- The quarter-mile response contained 1,232 synchronized telemetry frames.
- The Corvette finished in approximately 11.18 seconds.
- The Mustang finished in approximately 12.31 seconds.
- The full Maven reactor completed with `BUILD SUCCESS`.
- All 16 engine tests passed.
- All 3 backend tests passed, including the PostgreSQL/Testcontainers integration test.

The exact integration-test result is more important than the sample race times:
it proves a clean PostgreSQL instance can run every migration, load the seed
data, map both vehicles into the engine, complete a race, and return telemetry.

## 11. Problems found and corrected

### PostgreSQL was unavailable

Spring originally failed because Docker Desktop was not running, so PostgreSQL
could not be reached. Starting Docker and the Compose service resolved it.

### Schema numeric types did not match Java types

PostgreSQL `NUMERIC` columns conflicted with Hibernate's expected type for Java
`double`/`Double`. Migration V6 converts simulation measurements to `DOUBLE
PRECISION`, matching the in-memory numerical model while retaining schema
validation.

### Multiple collection fetches

The original detail graph attempted to join-fetch several ordered `List`
collections at once. Hibernate rejected that with `MultipleBagFetchException`.
The query now fetches the aggregate's to-one relationships and loads ordered
child collections within the read-only service transaction.

### Port and working-directory confusion

Jenkins already uses IPv4 port 8080 locally, so port 8081 is recommended. The
Spring Boot command must also run from the `Backend` directory, not the reactor
root.

## 12. Recommended next backend increments

1. Share and freeze the current catalog and race API contract with the frontend team.
2. Add OpenAPI/Swagger documentation for interactive endpoint discovery.
3. Add an ingestion boundary for external vehicle providers without coupling provider payloads to domain tables.
4. Expand source provenance and add an approval workflow before imported data becomes simulation-ready.
5. Add additional validated vehicle fixtures across FWD, RWD, AWD, manual, automatic, and dual-clutch layouts.
6. Add saved race and garage persistence after the core anonymous workflow is stable.
7. Add authentication and user ownership after those persistence contracts are agreed upon.
8. Add AI result explanations only after the deterministic result payload is stable.

## 13. Commit and pull-request guidance

This increment is ready for a team review because the application starts, the
end-to-end API works, and all automated tests pass. It should be submitted as a
focused backend-integration branch or pull request.

Before committing:

- include backend Java sources, tests, migrations, Maven files, and documentation;
- include the repository-level `pom.xml` that joins the modules;
- do not include any `target/` directories;
- do not include temporary Office files whose names begin with `~$`;
- review generated `output/` documents separately before deciding whether they belong in source control; and
- review the existing uncommitted `simulation-engine/RaceSimulator.java` change separately because it predates this backend increment.

A clear pull-request title would be:

```text
Integrate PostgreSQL vehicle catalog with deterministic race API
```

The pull-request description should mention the API contract, migrations,
seeded development fixtures, engine mapping, test results, and deferred features.

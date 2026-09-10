# Race Simulator

Web-based vehicle racing simulation platform developed for CMPE 195A/195B.

Development is intentionally incremental. The first component is a standalone,
deterministic Java simulation engine. An independent Spring Boot and PostgreSQL
vehicle-catalog foundation is now in place. A `Frontend/` module has been
created for the frontend team; its toolchain is not yet scaffolded.
Authentication, external APIs, and the AI explanation layer will be added in
later milestones.

## Current project structure

```text
Race Simulator/
├── Backend/             Spring Boot API and PostgreSQL vehicle catalog
├── Frontend/            Web client owned by the frontend team
└── simulation-engine/   Physics model and automated tests
```

## Current milestone

Given two manually defined vehicles and a drag-race configuration, the engine:

- interpolates torque from each vehicle's RPM-based torque curve;
- applies transmission ratios, final drive, and drivetrain loss;
- varies drivetrain loss conservatively with drivetrain layout, transmission
  type, engine speed, and road speed;
- calculates longitudinal weight transfer from wheelbase, center-of-gravity
  height, and static weight distribution;
- limits wheel force using the resulting driven-axle traction;
- subtracts aerodynamic drag and rolling resistance;
- integrates acceleration, speed, and distance at fixed time steps;
- models timed sequential upshifts with power interruption and RPM drop;
- records explicit shift events for later gauge and animation playback;
- models vehicle-specific launch RPM and progressive torque transfer;
- exposes launch, traction-limit, and estimated wheel-slip telemetry;
- accepts temperature, pressure, humidity, road temperature, grade, and wind,
  using them in air-density, power, grip, drag, rolling, and grade forces;
- exposes engine power, wheel torque, effective drivetrain efficiency, air
  density, grade resistance, net force, and the dominant acceleration limit;
- records synchronized telemetry for both vehicles;
- interpolates 0-60 mph, 1/8-mile, and finish-line milestones; and
- returns a deterministic winner and margin;
- supports distance-based drag races and target-speed roll races;
- selects the strongest valid starting gear for a rolling start without running
  the standing-launch model;
- derives lead changes, overtakes, largest lead, and the distance gap when the
  winner finishes from the authoritative timeline.

The engine also contains an initial real-vehicle validation harness. It compares
simulated 0-60, quarter-mile elapsed time, and trap speed against published
instrumented tests while keeping estimated input data explicitly documented.
It validates torque curves against rated peak horsepower and reports aggregate
mean/worst errors plus separate 0-60, quarter-mile, and trap-speed errors.
The test suite also compares the standard 0.01-second time step with a five-times
finer simulation to detect numerical instability.

This is a transparent baseline model, not a claim of final real-world accuracy.
Launch behavior, tire behavior beyond the current longitudinal slip approximation,
shift strategy, powertrain inertia, turbocharger response, and broader validation
against published vehicle data remain areas for refinement.

## Run the backend

The backend currently provides the independent PostgreSQL foundation for the
vehicle catalog. It does not yet depend on or call the simulation engine.

Requirements: Java 21 or newer, Maven 3.9 or newer, and Docker Compose.

```bash
cd Backend
docker compose up -d
mvn spring-boot:run
```

See [`Backend/README.md`](Backend/README.md) for the available endpoints and
configuration options.

## Run the engine tests

Requirements: Java 21 or newer and Maven 3.9 or newer.

```bash
cd simulation-engine
mvn test
```

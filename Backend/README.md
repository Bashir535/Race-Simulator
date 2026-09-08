# Race Simulator Backend

Spring Boot and PostgreSQL foundation for the vehicle catalog. This module is
currently independent from the simulation engine.

## Requirements

- Java 21 or newer
- Maven 3.9 or newer
- Docker Desktop or another Docker Compose installation

## Start the database

```bash
cd Backend
docker compose up -d
```

PostgreSQL starts on `localhost:5432` with development-only credentials defined
in `compose.yaml`. The named volume preserves local data between restarts.

## Start the API

```bash
mvn spring-boot:run
```

On startup, Flyway creates the `makes` and `vehicle_models` tables and inserts
the four initial makes/models documented by the project.

## Try the endpoints

```bash
curl http://localhost:8080/api/v1/vehicles/makes
curl "http://localhost:8080/api/v1/vehicles/models?makeId=1"
```

Clients should discover generated IDs from the makes endpoint instead of
hard-coding them.

## Configuration

The defaults work with `compose.yaml`. Override them when needed:

- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `SERVER_PORT`

Do not commit production credentials.

## Run tests

```bash
mvn test
```

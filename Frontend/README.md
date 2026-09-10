# Race Simulator Frontend

Web client for the Race Simulator platform. This module is owned by the
frontend team and is developed independently of the simulation engine.

## Scope

The frontend consumes the backend REST API. It does not modify or depend on
the source of `simulation-engine/` or `Backend/`.

## Planned stack

- Vite
- React
- TypeScript

Application code lives in `src/`. The toolchain is not scaffolded yet.

## Backend API

The backend serves the vehicle catalog on `http://localhost:8080` by default.
Start it before running the client:

```bash
cd Backend
docker compose up -d
mvn spring-boot:run
```

Available endpoints are documented in [`../Backend/README.md`](../Backend/README.md).

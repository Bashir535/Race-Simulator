# Race Simulator Frontend

Web client for the Race Simulator platform. It talks to the Spring Boot API
over HTTP/JSON and nothing else: no direct database access, no engine code in
the browser, and no physics of its own.

## Stack

- Vite
- React (JSX components, TypeScript for the API and model layer)
- recharts (telemetry charts), lucide-react (icons)

```
src/
  RevMatch.jsx                    tab shell, lane state, race + playback
  theme.js                        shared design tokens
  lib/api/types.ts                one-to-one mirrors of the backend DTOs
  lib/api/client.ts               the only place that calls the API
  lib/raceModel.ts                race options, request shaping, timeline reads
  lib/units.ts                    every SI-to-display conversion
  lib/garage.js                   localStorage persistence for saved vehicles
  hooks/useVehicleSelection.ts    year -> make -> model -> trim cascade
  hooks/usePopularVehicles.ts     GET /vehicles/popular
  hooks/usePlayback.ts            requestAnimationFrame playback clock
  components/                     presentation only; no fetch calls
test/                             client wiring and playback model suites
```

## Configuration

The API base URL comes from an environment variable. Copy the example and
adjust if your backend differs:

```bash
cp Frontend/.env.example Frontend/.env.local
```

```
VITE_API_BASE_URL=http://localhost:8081/api/v1
```

`.env.local` is git-ignored. This value is compiled into the browser bundle,
so it is public — never put a credential in a `VITE_` variable.

Port 8081 rather than the backend's default 8080 follows
[`../Backend/BACKEND_DATABASE_INTEGRATION_GUIDE.md`](../Backend/BACKEND_DATABASE_INTEGRATION_GUIDE.md),
section 9: Jenkins occupies 8080 on the development machine.

No dev proxy is needed. The backend's `WebConfiguration` allows
`http://localhost:5173`, which is why the dev server pins that port.

## Running

```bash
npm install
npm run dev        # http://localhost:5173
npm run build      # outputs to Frontend/dist
npm test           # client + model suites
npm run typecheck  # tsc --noEmit, strict
npm run check:ui   # square corners, theme tokens
npm run check      # all of the above, then build
```

Start the backend first:

```bash
cd Backend
docker compose up -d
SERVER_PORT=8081 mvn spring-boot:run
```

## What talks to what

Every request goes through `lib/api/client.ts`, which covers all seven
endpoints, checks `response.ok`, parses the `ApiErrorResponse` envelope,
preserves `correlationId`, separates HTTP failures from network failures,
supports cancellation, and shares identical in-flight GETs so React's
double-mounted effects do not issue duplicate requests.

```
GET  /vehicles/years
GET  /vehicles/makes[?year=]
GET  /vehicles/models?makeId=[&year=]
GET  /vehicles/trims?modelId=&year=
GET  /vehicles/popular
GET  /vehicles/{trimId}
POST /races/simulate
```

Vehicles are identified by their database trim id throughout. Nothing is
matched by name and no id is hard-coded.

## Simulation and playback

Races are simulated by the Java engine behind `POST /api/v1/races/simulate`.
The response is a completed, deterministic result: both cars' outcomes plus a
synchronised frame timeline.

Playback is a recording, not a live calculation. `hooks/usePlayback.ts` runs a
`requestAnimationFrame` clock and writes positions, speeds, RPM, gears, shift
indicators and acceleration straight to the DOM through refs, so a twelve
second race re-renders React a handful of times rather than a thousand.
Interpolation between adjacent backend frames is a drawing convenience and can
never produce a value the engine did not bracket; gear and the traction and
launch flags are read from the earlier frame rather than blended. The response
is never mutated.

Start, pause, resume, replay and reset are all supported. Replay re-runs the
timeline already in hand without asking the backend again. When the viewer
prefers reduced motion, playback is skipped and the finished result is shown.

## Units

The backend speaks SI. Every conversion lives in `lib/units.ts` and nowhere
else. Converted values are for display only and are never fed back into a race
request or used to recompute anything the engine decided.

## Vehicle specifications

Weight, power, torque, gearing, torque curves and the full simulation
specification all come from `GET /vehicles/{trimId}`. The client holds no spec
table and no bundled catalog fallback.

## Known API gap

`VehicleTrimResponse` and `VehicleDetailResponse` carry make and model as
*names* but not as ids. A vehicle chosen from the popular list or the garage
therefore cannot rehydrate the Year/Make/Model selects above it; the card says
so and offers to start a fresh cascade. Adding `makeId` and `modelId` to
`VehicleTrimResponse` would close this. It does not affect racing, which needs
only the trim id.

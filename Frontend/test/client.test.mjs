/*
 * Wiring harness for Frontend/src/lib/api/client.ts.
 *
 * The stub below returns DTO-SHAPED payloads only. It verifies how the client
 * talks to a server: URLs, methods, bodies, error parsing, correlation ids,
 * de-duplication, cancellation. It does NOT verify race physics or real data.
 */
import { createServer } from "node:http";

const { getYears, getMakes, getModels, getTrims, getPopularVehicles, getVehicle,
        simulateRace, ApiError, NetworkError, isCancelled, describeError } =
  await import(`${process.env.BUNDLE_DIR}/client.bundle.mjs`);

const seen = [];
let hits = 0;

const server = createServer((req, res) => {
  seen.push(`${req.method} ${req.url}`);
  hits += 1;

  if (req.url.startsWith("/api/v1/vehicles/999")) {
    res.writeHead(404, { "Content-Type": "application/json", "X-Correlation-Id": "hdr-404" });
    res.end(JSON.stringify({
      timestamp: "2026-09-22T00:00:00Z", status: 404, code: "VEHICLE_NOT_FOUND",
      message: "Vehicle 999 was not found.", fieldErrors: null,
      correlationId: "corr-abc-123", retryable: false,
    }));
    return;
  }
  if (req.url.startsWith("/api/v1/races/simulate")) {
    let body = "";
    req.on("data", (c) => { body += c; });
    req.on("end", () => {
      seen.push(`BODY ${body}`);
      res.writeHead(400, { "Content-Type": "application/json" });
      res.end(JSON.stringify({
        timestamp: "2026-09-22T00:00:00Z", status: 400, code: "VALIDATION_FAILED",
        message: "Request validation failed.",
        fieldErrors: { "race.distanceMeters": "must be positive" },
        correlationId: "corr-xyz", retryable: false,
      }));
    });
    return;
  }
  if (req.url.startsWith("/api/v1/vehicles/slow")) {
    setTimeout(() => { res.writeHead(200, { "Content-Type": "application/json" }); res.end("[]"); }, 120);
    return;
  }
  res.writeHead(200, { "Content-Type": "application/json" });
  res.end(JSON.stringify([{ id: 1, name: "Ford", country: "United States" }]));
});

await new Promise((r) => server.listen(18081, r));

const results = [];
const check = (name, pass, detail = "") => results.push({ name, pass, detail });

/* --- URL construction ------------------------------------------------- */
seen.length = 0;
await getYears();
await getMakes(2024);
await getMakes();
await getModels(7, 2024);
await getModels(7);
await getTrims(3, 2024);
await getPopularVehicles();

check("GET /vehicles/years", seen.includes("GET /api/v1/vehicles/years"));
check("makes with year", seen.includes("GET /api/v1/vehicles/makes?year=2024"));
check("makes without year omits param", seen.includes("GET /api/v1/vehicles/makes"));
check("models with makeId+year", seen.includes("GET /api/v1/vehicles/models?makeId=7&year=2024"));
check("models without year", seen.includes("GET /api/v1/vehicles/models?makeId=7"));
check("trims with modelId+year", seen.includes("GET /api/v1/vehicles/trims?modelId=3&year=2024"));
check("popular", seen.includes("GET /api/v1/vehicles/popular"));

/* --- error envelope --------------------------------------------------- */
try {
  await getVehicle(999);
  check("404 raises", false);
} catch (e) {
  check("404 raises ApiError", e instanceof ApiError);
  check("404 isNotFound", e.isNotFound === true);
  check("correlationId from envelope", e.correlationId === "corr-abc-123", e.correlationId);
  check("code preserved", e.code === "VEHICLE_NOT_FOUND");
  check("retryable false", e.retryable === false);
  check("readable message", e.message === "Vehicle 999 was not found.");
}

/* --- race request body + field errors --------------------------------- */
seen.length = 0;
try {
  await simulateRace({
    vehicleAId: 1, vehicleBId: 2,
    race: { goalType: "DISTANCE", distanceMeters: 402.336,
            startingSpeedMetersPerSecond: 0, roadSurface: "PREPARED_DRAG_STRIP" },
  });
  check("400 raises", false);
} catch (e) {
  const body = seen.find((s) => s.startsWith("BODY "));
  check("POST /races/simulate", seen.includes("POST /api/v1/races/simulate"));
  check("request body exact", body === 'BODY {"vehicleAId":1,"vehicleBId":2,"race":{"goalType":"DISTANCE","distanceMeters":402.336,"startingSpeedMetersPerSecond":0,"roadSurface":"PREPARED_DRAG_STRIP"}}', body);
  check("fieldErrors surfaced", describeError(e).includes("race.distanceMeters: must be positive"), describeError(e));
}

/* --- de-duplication ---------------------------------------------------- */
hits = 0;
await Promise.all([getPopularVehicles(), getPopularVehicles(), getPopularVehicles()]);
check("3 concurrent identical GETs -> 1 request", hits === 1, `hits=${hits}`);

/* --- cancellation isolation -------------------------------------------- */
const a = new AbortController();
const p1 = getVehicle("slow", a.signal).catch((e) => (isCancelled(e) ? "cancelled" : "other"));
const p2 = getVehicle("slow").then(() => "completed", () => "failed");
a.abort();
check("aborted caller cancels", (await p1) === "cancelled");
check("other caller unaffected by peer abort", (await p2) === "completed");

/* --- network failure --------------------------------------------------- */
server.close();
await new Promise((r) => setTimeout(r, 60));
try {
  await getYears();
  check("network failure raises", false);
} catch (e) {
  check("connection refused -> NetworkError", e instanceof NetworkError);
  check("network message actionable", /backend is running/.test(e.message), e.message);
}

const failed = results.filter((r) => !r.pass);
for (const r of results) console.log(`${r.pass ? "PASS" : "FAIL"}  ${r.name}${r.detail ? "  -> " + r.detail : ""}`);
console.log(`\n${results.length - failed.length}/${results.length} passed`);
process.exit(failed.length ? 1 : 0);

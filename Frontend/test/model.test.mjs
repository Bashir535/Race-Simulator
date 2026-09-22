const { telemetryAt, progressOf, buildRaceRequest, chartRows, timelineDuration,
        findMilestone, RACE_OPTIONS } = await import(`${process.env.BUNDLE_DIR}/model.bundle.mjs`);

const results = [];
const check = (n, p, d = "") => results.push({ n, p, d });
const near = (a, b, eps = 1e-9) => Math.abs(a - b) < eps;

const tel = (d, s, a, rpm, g, extra = {}) => ({
  distanceMeters: d, speedMetersPerSecond: s, accelerationMetersPerSecondSquared: a,
  engineRpm: rpm, gear: g, tractionLimited: false, launchActive: false,
  accelerationLimit: null, ...extra,
});
const frames = [
  { timeSeconds: 0.0, vehicleA: tel(0, 0, 5, 1000, 1), vehicleB: tel(0, 0, 4, 900, 1) },
  { timeSeconds: 1.0, vehicleA: tel(10, 20, 6, 3000, 2), vehicleB: tel(8, 16, 5, 2800, 1) },
  { timeSeconds: 2.0, vehicleA: tel(40, 40, 7, 5000, 3), vehicleB: tel(32, 32, 6, 4600, 2) },
];

/* --- interpolation ---------------------------------------------------- */
const mid = telemetryAt(frames, 0.5, "vehicleA");
check("interpolates distance", near(mid.distanceMeters, 5), mid.distanceMeters);
check("interpolates speed", near(mid.speedMetersPerSecond, 10), mid.speedMetersPerSecond);
check("interpolates rpm", near(mid.engineRpm, 2000), mid.engineRpm);
check("gear is NOT blended", mid.gear === 1, `gear=${mid.gear}`);

const exact = telemetryAt(frames, 1.0, "vehicleA");
check("exact frame hit", exact.distanceMeters === 10 && exact.gear === 2);

check("clamps before start", telemetryAt(frames, -5, "vehicleA").distanceMeters === 0);
check("clamps past end", telemetryAt(frames, 99, "vehicleA").distanceMeters === 40);
check("reads vehicleB independently", telemetryAt(frames, 2, "vehicleB").distanceMeters === 32);

/* never invents a value outside the bracketing frames */
let outOfRange = 0;
for (let t = 0; t <= 2; t += 0.017) {
  const v = telemetryAt(frames, t, "vehicleA").speedMetersPerSecond;
  if (v < 0 || v > 40) outOfRange += 1;
}
check("interpolation stays inside engine bounds", outOfRange === 0, `${outOfRange} out of range`);

/* --- does not mutate the backend timeline ----------------------------- */
const snapshot = JSON.stringify(frames);
for (let t = 0; t <= 2; t += 0.1) { telemetryAt(frames, t, "vehicleA"); telemetryAt(frames, t, "vehicleB"); }
check("timeline not mutated", JSON.stringify(frames) === snapshot);

/* --- progress --------------------------------------------------------- */
const drag = RACE_OPTIONS["drag-quarter"];
check("drag progress at start", progressOf(tel(0, 0, 0, 0, 1), drag) === 0);
check("drag progress at finish", near(progressOf(tel(402.336, 0, 0, 0, 1), drag), 1));
check("drag progress clamped", progressOf(tel(9999, 0, 0, 0, 1), drag) === 1);

const roll = RACE_OPTIONS["roll-40-120"];
check("roll progress at band start", near(progressOf(tel(0, 40 * 0.44704, 0, 0, 1), roll), 0));
check("roll progress at band end", near(progressOf(tel(0, 120 * 0.44704, 0, 0, 1), roll), 1));

/* --- request shaping -------------------------------------------------- */
const distReq = buildRaceRequest(1, 2, RACE_OPTIONS["drag-eighth"], "DRY_ASPHALT");
check("DISTANCE goal type", distReq.race.goalType === "DISTANCE");
check("eighth mile metres", near(distReq.race.distanceMeters, 201.168));
check("no target speed on distance race", distReq.race.targetSpeedMetersPerSecond === undefined);
check("environment omitted", !("environment" in distReq.race));

const speedReq = buildRaceRequest(5, 6, roll, "WET_ASPHALT");
check("SPEED goal type", speedReq.race.goalType === "SPEED");
check("start speed in m/s", near(speedReq.race.startingSpeedMetersPerSecond, 40 * 0.44704));
check("target speed in m/s", near(speedReq.race.targetSpeedMetersPerSecond, 120 * 0.44704));
check("no distance on speed race", speedReq.race.distanceMeters === undefined);
check("trim ids passed through", speedReq.vehicleAId === 5 && speedReq.vehicleBId === 6);

/* --- summary helpers --------------------------------------------------- */
check("duration is last frame", timelineDuration({ timeline: frames }) === 2);
check("empty timeline duration 0", timelineDuration({ timeline: [] }) === 0);

const rows = chartRows({ timeline: frames });
check("chart keeps last frame", rows[rows.length - 1].t === 2);
check("chart gap is A minus B", rows[rows.length - 1].gap === 8);

const vr = { milestones: [{ name: "0-60 mph", elapsedSeconds: 4.2, speedMetersPerSecond: 26.8 }] };
check("finds milestone", findMilestone(vr, "0-60 mph").elapsedSeconds === 4.2);
check("missing milestone is null", findMilestone(vr, "1/8 mile") === null);
check("null milestones safe", findMilestone({ milestones: null }, "Finish") === null);

const failed = results.filter((r) => !r.p);
for (const r of results) console.log(`${r.p ? "PASS" : "FAIL"}  ${r.n}${r.d ? "  -> " + r.d : ""}`);
console.log(`\n${results.length - failed.length}/${results.length} passed`);
process.exit(failed.length ? 1 : 0);

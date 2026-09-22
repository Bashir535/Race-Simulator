/*
 * Reading the race the backend already ran.
 *
 * POST /races/simulate returns a finished, deterministic result: both cars'
 * outcomes and a synchronised frame timeline. This module only reads it.
 *
 * Nothing here computes motion. Interpolation between two adjacent backend
 * frames is a drawing convenience so lanes can move at display rate instead of
 * the engine's fixed step; it can never produce a value the engine did not
 * already bracket. The response object itself is never mutated.
 */
import type {
  Frame,
  Milestone,
  RaceConfiguration,
  RaceRequest,
  RaceResponse,
  RoadSurface,
  Side,
  VehicleResult,
  VehicleTelemetry,
} from "./api/types";
import { EIGHTH_MILE_METERS, QUARTER_MILE_METERS, mphToMps } from "./units";

/* Milestone names the engine emits (RaceSimulator.java). */
export const MILESTONE_ZERO_TO_SIXTY = "0-60 mph";
export const MILESTONE_EIGHTH = "1/8 mile";
export const MILESTONE_FINISH = "Finish";

/* ---------------------------------------------------------------------- */
/* Race options                                                           */
/*                                                                        */
/* The choices the UI offers, each carrying the SI values its request      */
/* needs. Speeds are converted once, here, so no component does it.        */
/* ---------------------------------------------------------------------- */
export interface RaceOption {
  readonly label: string;
  readonly kind: "drag" | "roll";
  /** Metres for a drag race; undefined for a roll race. */
  readonly distanceMeters?: number;
  readonly startingSpeedMetersPerSecond: number;
  readonly targetSpeedMetersPerSecond?: number;
}

export const RACE_OPTIONS: Record<string, RaceOption> = {
  "drag-quarter": {
    label: "Drag · quarter mile",
    kind: "drag",
    distanceMeters: QUARTER_MILE_METERS,
    startingSpeedMetersPerSecond: 0,
  },
  "drag-eighth": {
    label: "Drag · eighth mile",
    kind: "drag",
    distanceMeters: EIGHTH_MILE_METERS,
    startingSpeedMetersPerSecond: 0,
  },
  "roll-40-120": {
    label: "Roll · 40–120 mph",
    kind: "roll",
    startingSpeedMetersPerSecond: mphToMps(40),
    targetSpeedMetersPerSecond: mphToMps(120),
  },
  "roll-60-140": {
    label: "Roll · 60–140 mph",
    kind: "roll",
    startingSpeedMetersPerSecond: mphToMps(60),
    targetSpeedMetersPerSecond: mphToMps(140),
  },
};

export const ROAD_SURFACES: ReadonlyArray<{ value: RoadSurface; label: string }> = [
  { value: "PREPARED_DRAG_STRIP", label: "Prepared drag strip" },
  { value: "DRY_ASPHALT", label: "Dry asphalt" },
  { value: "WET_ASPHALT", label: "Wet asphalt" },
  { value: "GRAVEL", label: "Gravel" },
];

/** Builds the exact RaceRequest body the backend validates. */
export function buildRaceRequest(
  vehicleAId: number,
  vehicleBId: number,
  option: RaceOption,
  roadSurface: RoadSurface,
): RaceRequest {
  const race: RaceConfiguration = option.kind === "drag"
    ? {
      goalType: "DISTANCE",
      distanceMeters: option.distanceMeters,
      startingSpeedMetersPerSecond: option.startingSpeedMetersPerSecond,
      roadSurface,
    }
    : {
      goalType: "SPEED",
      targetSpeedMetersPerSecond: option.targetSpeedMetersPerSecond,
      startingSpeedMetersPerSecond: option.startingSpeedMetersPerSecond,
      roadSurface,
    };
  /* environment is omitted so the engine applies its standard conditions. */
  return { vehicleAId, vehicleBId, race };
}

/* ---------------------------------------------------------------------- */
/* Reading results                                                        */
/* ---------------------------------------------------------------------- */

export function findMilestone(result: VehicleResult, name: string): Milestone | null {
  return result.milestones?.find((m) => m.name === name) ?? null;
}

/** Last frame time in the timeline — how long playback runs for. */
export function timelineDuration(response: RaceResponse): number {
  const frames = response.timeline;
  return frames.length ? frames[frames.length - 1].timeSeconds : 0;
}

/**
 * The scalar each race type uses to place a car on the track, normalised to
 * 0..1. Distance for a drag race; speed travelled through the band for a roll.
 */
export function progressOf(
  telemetry: VehicleTelemetry,
  option: RaceOption,
): number {
  if (option.kind === "drag") {
    const total = option.distanceMeters || 1;
    return clamp01(telemetry.distanceMeters / total);
  }
  const start = option.startingSpeedMetersPerSecond;
  const span = (option.targetSpeedMetersPerSecond ?? start) - start;
  if (span <= 0) return 0;
  return clamp01((telemetry.speedMetersPerSecond - start) / span);
}

function clamp01(value: number): number {
  return value < 0 ? 0 : value > 1 ? 1 : value;
}

/* ---------------------------------------------------------------------- */
/* Frame lookup                                                           */
/* ---------------------------------------------------------------------- */

/** Index of the last frame at or before `time`. Binary search; frames are sorted. */
function frameIndexAt(frames: readonly Frame[], time: number): number {
  let low = 0;
  let high = frames.length - 1;
  while (low < high) {
    const mid = (low + high + 1) >> 1;
    if (frames[mid].timeSeconds <= time) low = mid;
    else high = mid - 1;
  }
  return low;
}

/**
 * Telemetry for one car at an arbitrary playback time.
 *
 * Continuous quantities are interpolated between the bracketing frames.
 * Discrete state — gear, traction and launch flags, the limit name — is taken
 * from the earlier frame rather than blended, because a gear of 3.4 is not a
 * thing the engine ever reported.
 */
export function telemetryAt(
  frames: readonly Frame[],
  time: number,
  side: Side,
): VehicleTelemetry {
  const index = frameIndexAt(frames, time);
  const current = frames[index][side];
  const next = index + 1 < frames.length ? frames[index + 1] : null;

  if (!next) return current;

  const t0 = frames[index].timeSeconds;
  const span = next.timeSeconds - t0;
  if (span <= 0) return current;

  const fraction = clamp01((time - t0) / span);
  const later = next[side];

  return {
    distanceMeters: lerp(current.distanceMeters, later.distanceMeters, fraction),
    speedMetersPerSecond: lerp(current.speedMetersPerSecond, later.speedMetersPerSecond, fraction),
    accelerationMetersPerSecondSquared: lerp(
      current.accelerationMetersPerSecondSquared,
      later.accelerationMetersPerSecondSquared,
      fraction,
    ),
    engineRpm: lerp(current.engineRpm, later.engineRpm, fraction),
    gear: current.gear,
    tractionLimited: current.tractionLimited,
    launchActive: current.launchActive,
    accelerationLimit: current.accelerationLimit,
  };
}

function lerp(from: number, to: number, fraction: number): number {
  return from + (to - from) * fraction;
}

/* ---------------------------------------------------------------------- */
/* Charts                                                                 */
/* ---------------------------------------------------------------------- */

export interface ChartRow {
  t: number;
  speedA: number;
  speedB: number;
  gap: number;
}

/*
 * The engine steps at 0.01s, so a quarter mile is well over a thousand frames.
 * A 600px chart cannot resolve that, so keep every Nth frame plus the last.
 */
const CHART_STRIDE = 5;

/** Chart rows in SI; the chart formats them for display. */
export function chartRows(response: RaceResponse): ChartRow[] {
  const frames = response.timeline;
  const rows: ChartRow[] = [];
  for (let i = 0; i < frames.length; i += 1) {
    if (i % CHART_STRIDE !== 0 && i !== frames.length - 1) continue;
    const frame = frames[i];
    rows.push({
      t: frame.timeSeconds,
      speedA: frame.vehicleA.speedMetersPerSecond,
      speedB: frame.vehicleB.speedMetersPerSecond,
      gap: frame.vehicleA.distanceMeters - frame.vehicleB.distanceMeters,
    });
  }
  return rows;
}

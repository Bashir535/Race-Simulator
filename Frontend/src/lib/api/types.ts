/*
 * Backend DTO mirrors.
 *
 * Every type here is a one-to-one transcription of a Java record in
 * Backend/src/main/java/com/racesimulator/backend/dto/. Nothing is invented:
 * if a field is not on the record, it is not here.
 *
 * Nullability follows Java's boxing. A boxed type (Double, String, Short) can
 * be null and is optional here; a primitive (double, short, int, boolean)
 * cannot be and is required.
 *
 * All quantities are SI — metres, metres per second, kilograms, newton-metres.
 * Convert for display through lib/units.ts only.
 */

/* ---------------------------------------------------------------------- */
/* Errors — ApiErrorResponse.java                                          */
/* ---------------------------------------------------------------------- */
export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  code: string | null;
  message: string | null;
  fieldErrors: Record<string, string> | null;
  correlationId: string | null;
  retryable: boolean;
}

/* ---------------------------------------------------------------------- */
/* Catalog                                                                 */
/* ---------------------------------------------------------------------- */

/** VehicleMakeResponse.java */
export interface VehicleMake {
  id: number;
  name: string;
  country: string | null;
}

/** VehicleModelResponse.java */
export interface VehicleModel {
  id: number;
  makeId: number;
  name: string;
  generation: string | null;
  bodyStyle: string | null;
  productionStartYear: number | null;
  productionEndYear: number | null;
}

/** VehicleTrimResponse.java — `id` is the trim id every race request uses. */
export interface VehicleTrim {
  id: number;
  year: number;
  make: string | null;
  model: string | null;
  generation: string | null;
  trim: string | null;
  drivetrain: string | null;
  transmission: string | null;
  horsepower: number | null;
  torqueNm: number | null;
  massKg: number;
  imageUrl: string | null;
  popular: boolean;
}

/** VehicleDetailResponse.FuelEfficiency */
export interface FuelEfficiency {
  cityMpg: number | null;
  highwayMpg: number | null;
  combinedMpg: number | null;
}

/** VehicleDetailResponse.TorquePoint */
export interface TorquePoint {
  rpm: number;
  torqueNm: number;
}

/** VehicleDetailResponse.Engine */
export interface VehicleEngine {
  name: string | null;
  displacementLiters: number | null;
  configuration: string | null;
  aspiration: string | null;
  fuelType: string | null;
  horsepower: number | null;
  peakHorsepowerRpm: number | null;
  torqueNm: number | null;
  peakTorqueRpm: number | null;
  idleRpm: number;
  shiftRpm: number;
  redlineRpm: number;
  torqueCurve: TorquePoint[] | null;
}

/** VehicleDetailResponse.Transmission */
export interface VehicleTransmission {
  name: string | null;
  type: string | null;
  numberOfGears: number;
  finalDriveRatio: number;
  shiftDurationSeconds: number;
  drivetrainEfficiency: number;
  gearRatios: number[] | null;
}

/** VehicleDetailResponse.SimulationSpecification */
export interface SimulationSpecification {
  massKg: number;
  frontWeightFraction: number;
  wheelbaseMeters: number;
  centerOfGravityHeightMeters: number;
  wheelRadiusMeters: number;
  dragCoefficient: number;
  frontalAreaSquareMeters: number;
  rollingResistanceCoefficient: number;
  tireFrictionCoefficient: number;
  launchRpm: number;
  launchEngagementDurationSeconds: number;
  initialTorqueTransferFraction: number;
  launchControlEnabled: boolean;
}

/** VehicleDetailResponse.PublishedPerformance */
export interface PublishedPerformance {
  zeroToSixtySeconds: number | null;
  quarterMileSeconds: number | null;
  quarterMileTrapSpeedMph: number | null;
  rolloutSeconds: number | null;
  source: string | null;
}

/** VehicleDetailResponse.java */
export interface VehicleDetail {
  identity: VehicleTrim;
  bodyStyle: string | null;
  originalMsrpUsd: number | null;
  tireDescription: string | null;
  fuelEfficiency: FuelEfficiency | null;
  engine: VehicleEngine | null;
  transmission: VehicleTransmission | null;
  simulationSpecification: SimulationSpecification | null;
  publishedPerformance: PublishedPerformance[] | null;
  dataStatus: string | null;
}

/* ---------------------------------------------------------------------- */
/* Race request — RaceRequest.java                                         */
/* ---------------------------------------------------------------------- */

/** RaceRequest.GoalType */
export type GoalType = "DISTANCE" | "SPEED";

/** engine model RoadSurface.java */
export type RoadSurface =
  | "PREPARED_DRAG_STRIP"
  | "DRY_ASPHALT"
  | "WET_ASPHALT"
  | "GRAVEL";

/** RaceRequest.Environment — every field is a primitive, so all are required. */
export interface RaceEnvironment {
  airTemperatureCelsius: number;
  airPressurePascals: number;
  relativeHumidity: number;
  roadTemperatureCelsius: number;
  roadGradePercent: number;
  headwindMetersPerSecond: number;
}

/** RaceRequest.RaceConfiguration */
export interface RaceConfiguration {
  goalType: GoalType;
  /** Required when goalType is DISTANCE. */
  distanceMeters?: number;
  /** Required when goalType is SPEED. */
  targetSpeedMetersPerSecond?: number;
  startingSpeedMetersPerSecond: number;
  roadSurface: RoadSurface;
  /** Omitted entirely means the engine uses its standard conditions. */
  environment?: RaceEnvironment;
}

/** RaceRequest.java */
export interface RaceRequest {
  vehicleAId: number;
  vehicleBId: number;
  race: RaceConfiguration;
}

/* ---------------------------------------------------------------------- */
/* Race response — RaceResponse.java                                       */
/* ---------------------------------------------------------------------- */

/** RaceResponse.Milestone — names the engine emits: "0-60 mph", "1/8 mile", "Finish". */
export interface Milestone {
  name: string;
  elapsedSeconds: number;
  speedMetersPerSecond: number;
}

/** RaceResponse.Shift */
export interface Shift {
  startTimeSeconds: number;
  endTimeSeconds: number;
  fromGear: number;
  toGear: number;
  rpmBefore: number;
  rpmAfter: number;
}

/** RaceResponse.VehicleResult */
export interface VehicleResult {
  vehicleId: number | null;
  name: string | null;
  finishTimeSeconds: number;
  finishSpeedMetersPerSecond: number;
  milestones: Milestone[] | null;
  shifts: Shift[] | null;
}

/** RaceResponse.LeadChange */
export interface LeadChange {
  timeSeconds: number;
  distanceMeters: number;
  newLeader: string | null;
}

/** RaceResponse.Summary */
export interface RaceSummary {
  winner: string | null;
  timeMarginSeconds: number;
  distanceGapAtWinnerFinishMeters: number;
  largestLeadVehicle: string | null;
  largestLeadMeters: number;
  leadChanges: LeadChange[] | null;
}

/** RaceResponse.VehicleTelemetry */
export interface VehicleTelemetry {
  distanceMeters: number;
  speedMetersPerSecond: number;
  accelerationMetersPerSecondSquared: number;
  engineRpm: number;
  gear: number;
  tractionLimited: boolean;
  launchActive: boolean;
  accelerationLimit: string | null;
}

/** RaceResponse.Frame */
export interface Frame {
  timeSeconds: number;
  vehicleA: VehicleTelemetry;
  vehicleB: VehicleTelemetry;
}

/** RaceResponse.Environment */
export interface ResponseEnvironment {
  airTemperatureCelsius: number;
  airPressurePascals: number;
  relativeHumidity: number;
  roadTemperatureCelsius: number;
  roadGradePercent: number;
  headwindMetersPerSecond: number;
}

/** RaceResponse.Configuration */
export interface ResponseConfiguration {
  goalType: string | null;
  distanceMeters: number | null;
  targetSpeedMetersPerSecond: number | null;
  startingSpeedMetersPerSecond: number;
  roadSurface: string | null;
  environment: ResponseEnvironment | null;
}

/** RaceResponse.java */
export interface RaceResponse {
  simulationVersion: string | null;
  config: ResponseConfiguration | null;
  vehicleA: VehicleResult;
  vehicleB: VehicleResult;
  winner: string | null;
  winningMarginSeconds: number;
  summary: RaceSummary | null;
  timeline: Frame[];
}

/** Which side of a race a value belongs to. */
export type Side = "vehicleA" | "vehicleB";

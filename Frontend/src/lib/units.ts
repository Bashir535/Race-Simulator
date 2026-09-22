/*
 * Display conversions.
 *
 * The backend speaks SI: metres, metres per second, kilograms, newton-metres.
 * Every conversion to the units this UI shows lives here and nowhere else, so
 * a constant is never redefined in a component and drift is impossible.
 *
 * These are for DISPLAY ONLY. A converted number must never be fed back into a
 * race request or used to recompute anything the engine already decided — the
 * engine's SI values stay authoritative all the way to the render call.
 */

/* Exact by definition: 1 mile = 1609.344 m, 1 international foot = 0.3048 m. */
const METERS_PER_MILE = 1609.344;
const METERS_PER_FOOT = 0.3048;
/* 1 mph = 0.44704 m/s exactly. */
const METERS_PER_SECOND_PER_MPH = 0.44704;
/* 1 lb = 0.45359237 kg exactly. */
const KILOGRAMS_PER_POUND = 0.45359237;
/* 1 lb-ft = 1.3558179483314004 N·m. */
const NEWTON_METERS_PER_POUND_FOOT = 1.3558179483314004;

/** Race distances the UI offers, in metres — the unit the backend wants. */
export const QUARTER_MILE_METERS = METERS_PER_MILE / 4;
export const EIGHTH_MILE_METERS = METERS_PER_MILE / 8;

export function mpsToMph(metersPerSecond: number): number {
  return metersPerSecond / METERS_PER_SECOND_PER_MPH;
}

/** Only used to turn a UI speed choice into the SI value a request carries. */
export function mphToMps(mph: number): number {
  return mph * METERS_PER_SECOND_PER_MPH;
}

export function metersToFeet(meters: number): number {
  return meters / METERS_PER_FOOT;
}

export function metersToMiles(meters: number): number {
  return meters / METERS_PER_MILE;
}

export function kgToLb(kilograms: number): number {
  return kilograms / KILOGRAMS_PER_POUND;
}

export function nmToLbFt(newtonMeters: number): number {
  return newtonMeters / NEWTON_METERS_PER_POUND_FOOT;
}

/* ---------------------------------------------------------------------- */
/* Formatters                                                             */
/*                                                                        */
/* Kept beside the conversions so a figure is rounded the same way         */
/* everywhere it appears.                                                  */
/* ---------------------------------------------------------------------- */

export function formatSeconds(seconds: number, digits = 2): string {
  return seconds.toFixed(digits);
}

export function formatMph(metersPerSecond: number, digits = 1): string {
  return mpsToMph(metersPerSecond).toFixed(digits);
}

export function formatPounds(kilograms: number): string {
  return Math.round(kgToLb(kilograms)).toLocaleString();
}

export function formatPoundFeet(newtonMeters: number): string {
  return Math.round(nmToLbFt(newtonMeters)).toLocaleString();
}

export function formatFeet(meters: number): string {
  return Math.round(metersToFeet(meters)).toLocaleString();
}

/** Power-to-weight, in the hp-per-ton the cards have always shown. */
export function horsepowerPerTon(horsepower: number, massKg: number): number {
  return horsepower / (kgToLb(massKg) / 2000);
}

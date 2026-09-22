/*
 * The one place the frontend talks to Spring Boot.
 *
 * Every HTTP call in the app goes through here. Components import the named
 * functions at the bottom and never call fetch themselves, so request shape,
 * error handling and cancellation are decided once.
 *
 * Base URL comes from VITE_API_BASE_URL (see Frontend/.env.example). It holds
 * no credentials and is safe to ship in a bundle.
 */
import type {
  ApiErrorResponse,
  RaceRequest,
  RaceResponse,
  VehicleDetail,
  VehicleMake,
  VehicleModel,
  VehicleTrim,
} from "./types";

const DEFAULT_BASE_URL = "http://localhost:8081/api/v1";

/* import.meta.env is Vite's; the cast keeps this file free of a global .d.ts. */
const ENV = (import.meta as unknown as { env?: Record<string, string | undefined> }).env;

/** Trailing slashes are stripped so path joining is predictable. */
export const API_BASE_URL: string = (ENV?.VITE_API_BASE_URL || DEFAULT_BASE_URL).replace(/\/+$/, "");

/* ---------------------------------------------------------------------- */
/* Failure modes                                                          */
/*                                                                        */
/* Two distinct things go wrong and the UI says different things about     */
/* each: the server answered with an error (ApiError), or it was never     */
/* reached at all (NetworkError).                                         */
/* ---------------------------------------------------------------------- */

/** The server answered, but not with success. Carries the backend envelope. */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string | null;
  readonly correlationId: string | null;
  readonly fieldErrors: Record<string, string> | null;
  readonly retryable: boolean;

  constructor(message: string, status: number, envelope: Partial<ApiErrorResponse> | null, correlationId: string | null) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = envelope?.code ?? null;
    this.correlationId = envelope?.correlationId ?? correlationId;
    this.fieldErrors = envelope?.fieldErrors ?? null;
    this.retryable = envelope?.retryable ?? status >= 500;
  }

  /** True when the trim id in the URL does not exist. */
  get isNotFound(): boolean {
    return this.status === 404;
  }
}

/** The request never produced a response: server down, DNS, CORS, offline. */
export class NetworkError extends Error {
  constructor(message: string, readonly cause?: unknown) {
    super(message);
    this.name = "NetworkError";
  }
}

/** Raised when the caller's AbortSignal fires. Callers ignore these. */
export class CancelledError extends Error {
  constructor() {
    super("Request cancelled");
    this.name = "CancelledError";
  }
}

export function isCancelled(error: unknown): boolean {
  return error instanceof CancelledError
    || (error instanceof DOMException && error.name === "AbortError");
}

/**
 * A sentence a person can act on. Field errors from bean validation are
 * appended, because they name exactly which input the backend rejected.
 */
export function describeError(error: unknown): string {
  if (error instanceof ApiError) {
    const detail = error.fieldErrors
      ? Object.entries(error.fieldErrors).map(([field, msg]) => `${field}: ${msg}`).join("; ")
      : "";
    const base = error.message || `The server returned ${error.status}.`;
    return detail ? `${base} (${detail})` : base;
  }
  if (error instanceof NetworkError) return error.message;
  if (error instanceof Error && error.message) return error.message;
  return "Something went wrong.";
}

/* ---------------------------------------------------------------------- */
/* Transport                                                              */
/* ---------------------------------------------------------------------- */

function buildUrl(path: string, params?: Record<string, string | number | undefined | null>): string {
  const url = new URL(`${API_BASE_URL}${path}`);
  if (params) {
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== null && value !== "") {
        url.searchParams.set(key, String(value));
      }
    }
  }
  return url.toString();
}

async function readErrorEnvelope(response: Response): Promise<Partial<ApiErrorResponse> | null> {
  try {
    const body = await response.json();
    return body && typeof body === "object" ? (body as Partial<ApiErrorResponse>) : null;
  } catch {
    /* Not every failure is JSON — a proxy 502 is usually HTML. */
    return null;
  }
}

/**
 * Performs one request and converts anything that is not a 2xx JSON body into
 * a typed error. `X-Correlation-Id` is read off the header too, because the
 * backend exposes it there as well as in the envelope.
 */
async function send<T>(url: string, init: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(url, init);
  } catch (cause) {
    if (isCancelled(cause)) throw new CancelledError();
    throw new NetworkError(
      "Could not reach the Race Simulator API. Check that the backend is running.",
      cause,
    );
  }

  const headerCorrelationId = response.headers.get("X-Correlation-Id");

  if (!response.ok) {
    const envelope = await readErrorEnvelope(response);
    const message = envelope?.message || `The server returned ${response.status} ${response.statusText}.`;
    throw new ApiError(message, response.status, envelope, headerCorrelationId);
  }

  try {
    return (await response.json()) as T;
  } catch (cause) {
    throw new ApiError(
      "The server sent a response the app could not read.",
      response.status,
      null,
      headerCorrelationId,
    );
  }
}

/*
 * In-flight de-duplication.
 *
 * React 19 StrictMode mounts effects twice in development, so an unguarded
 * catalog effect fires two identical GETs. Identical in-flight GETs share one
 * network request here instead.
 *
 * The shared request deliberately does not receive any caller's AbortSignal:
 * if it did, the first caller unmounting would cancel the request the second
 * caller is still waiting on. Callers get cancellation by racing their own
 * signal against the shared promise, which leaves the request itself alone.
 */
const inFlight = new Map<string, Promise<unknown>>();

function dedupedGet<T>(url: string): Promise<T> {
  const existing = inFlight.get(url);
  if (existing) return existing as Promise<T>;

  const request = send<T>(url, { method: "GET", headers: { Accept: "application/json" } })
    .finally(() => { inFlight.delete(url); });

  inFlight.set(url, request);
  return request;
}

/** Rejects as soon as `signal` aborts, without disturbing the shared request. */
function withCancellation<T>(promise: Promise<T>, signal?: AbortSignal): Promise<T> {
  if (!signal) return promise;
  if (signal.aborted) return Promise.reject(new CancelledError());

  return new Promise<T>((resolve, reject) => {
    const onAbort = () => reject(new CancelledError());
    signal.addEventListener("abort", onAbort, { once: true });
    promise.then(
      (value) => { signal.removeEventListener("abort", onAbort); resolve(value); },
      (error) => { signal.removeEventListener("abort", onAbort); reject(error); },
    );
  });
}

function get<T>(path: string, params?: Record<string, string | number | undefined | null>, signal?: AbortSignal): Promise<T> {
  return withCancellation(dedupedGet<T>(buildUrl(path, params)), signal);
}

/* ---------------------------------------------------------------------- */
/* Endpoints                                                              */
/*                                                                        */
/* One function per route in VehicleCatalogController and RaceController.  */
/* ---------------------------------------------------------------------- */

/** GET /vehicles/years */
export function getYears(signal?: AbortSignal): Promise<number[]> {
  return get<number[]>("/vehicles/years", undefined, signal);
}

/** GET /vehicles/makes[?year=] */
export function getMakes(year?: number, signal?: AbortSignal): Promise<VehicleMake[]> {
  return get<VehicleMake[]>("/vehicles/makes", { year }, signal);
}

/** GET /vehicles/models?makeId=[&year=] */
export function getModels(makeId: number, year?: number, signal?: AbortSignal): Promise<VehicleModel[]> {
  return get<VehicleModel[]>("/vehicles/models", { makeId, year }, signal);
}

/** GET /vehicles/trims?modelId=&year= — both parameters are required. */
export function getTrims(modelId: number, year: number, signal?: AbortSignal): Promise<VehicleTrim[]> {
  return get<VehicleTrim[]>("/vehicles/trims", { modelId, year }, signal);
}

/** GET /vehicles/popular */
export function getPopularVehicles(signal?: AbortSignal): Promise<VehicleTrim[]> {
  return get<VehicleTrim[]>("/vehicles/popular", undefined, signal);
}

/** GET /vehicles/{trimId} — 404 surfaces as ApiError with isNotFound set. */
export function getVehicle(trimId: number, signal?: AbortSignal): Promise<VehicleDetail> {
  return get<VehicleDetail>(`/vehicles/${trimId}`, undefined, signal);
}

/**
 * POST /races/simulate
 *
 * Not de-duplicated: a race is not idempotent from the user's point of view,
 * and the caller already guards against double submission.
 */
export function simulateRace(request: RaceRequest, signal?: AbortSignal): Promise<RaceResponse> {
  return withCancellation(
    send<RaceResponse>(buildUrl("/races/simulate"), {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify(request),
    }),
    signal,
  );
}

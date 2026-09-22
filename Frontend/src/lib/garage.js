/*
 * Garage persistence.
 *
 * A saved entry is a reference to a catalog vehicle — its database trim id
 * plus enough identity to label the card — and a nickname the viewer can
 * change. Specifications are not copied here: they belong to the backend and
 * are read fresh from GET /vehicles/{trimId} whenever the car is loaded, so a
 * saved entry can never go stale against the catalog.
 *
 * Every access is guarded: localStorage throws in private windows and when
 * site data is blocked, and the app has to keep working without it.
 */

/* v2 stores trim references. v1 stored hand-tuned specs, which no longer have
 * a meaning now that vehicles come from the catalog, so it is not migrated. */
const KEY = "revmatch:garage:v2";

function readRaw() {
  try {
    const raw = window.localStorage.getItem(KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch (e) {
    return [];
  }
}

function writeRaw(builds) {
  try {
    window.localStorage.setItem(KEY, JSON.stringify(builds));
    return true;
  } catch (e) {
    return false;
  }
}

/* Anything without a usable trim id cannot be loaded or raced, so it is dropped. */
function isValidBuild(b) {
  return b
    && typeof b.id === "string"
    && typeof b.name === "string"
    && Number.isInteger(b.trimId);
}

export function loadGarage() {
  return readRaw().filter(isValidBuild);
}

export function newBuildId() {
  return `build-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
}

export function saveGarage(builds) {
  return writeRaw(builds);
}

/** Turns a VehicleTrim from the catalog into a saved entry. */
export function toBuild(trim, id = newBuildId()) {
  const name = [trim.year, trim.make, trim.model].filter(Boolean).join(" ");
  return {
    id,
    trimId: trim.id,
    name: name || `Vehicle ${trim.id}`,
    year: trim.year ?? null,
    make: trim.make ?? null,
    model: trim.model ?? null,
    trim: trim.trim ?? null,
    generation: trim.generation ?? null,
    savedAt: Date.now(),
  };
}

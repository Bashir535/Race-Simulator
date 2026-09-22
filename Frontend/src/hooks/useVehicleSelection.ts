/*
 * The dealership cascade: Year -> Make -> Model -> Trim -> details.
 *
 * Each level fetches from the catalog endpoints when the level above it
 * settles, and changing a parent clears every dependent selection so a stale
 * model id can never be sent with a new make.
 *
 * Every request is cancelled when its inputs change or the component unmounts,
 * so a slow reply cannot land on a selection the viewer has moved past.
 */
import { useCallback, useEffect, useState } from "react";

import {
  describeError,
  getMakes,
  getModels,
  getTrims,
  getVehicle,
  getYears,
  isCancelled,
  ApiError,
} from "../lib/api/client";
import type { VehicleDetail, VehicleMake, VehicleModel, VehicleTrim } from "../lib/api/types";

export type LoadStatus = "idle" | "loading" | "ready" | "empty" | "error";

export interface Loadable<T> {
  status: LoadStatus;
  data: T;
  error: string | null;
}

function idle<T>(empty: T): Loadable<T> {
  return { status: "idle", data: empty, error: null };
}

/**
 * Runs `fetcher` whenever `enabled` or the dependency key changes, tracking
 * loading, empty, ready and error states for a list endpoint.
 */
function useList<T>(
  enabled: boolean,
  key: string,
  fetcher: (signal: AbortSignal) => Promise<T[]>,
): [Loadable<T[]>, () => void] {
  const [state, setState] = useState<Loadable<T[]>>(idle<T[]>([]));
  const [reloadToken, setReloadToken] = useState(0);
  const reload = useCallback(() => setReloadToken((n) => n + 1), []);

  useEffect(() => {
    if (!enabled) {
      setState(idle<T[]>([]));
      return undefined;
    }
    const controller = new AbortController();
    setState({ status: "loading", data: [], error: null });

    fetcher(controller.signal).then(
      (data) => {
        if (controller.signal.aborted) return;
        setState({
          status: data.length ? "ready" : "empty",
          data,
          error: null,
        });
      },
      (error) => {
        if (controller.signal.aborted || isCancelled(error)) return;
        setState({ status: "error", data: [], error: describeError(error) });
      },
    );

    return () => controller.abort();
    /* `key` stands in for the fetcher's inputs; the fetcher itself is inline. */
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enabled, key, reloadToken]);

  return [state, reload];
}

export interface VehicleSelection {
  years: Loadable<number[]>;
  makes: Loadable<VehicleMake[]>;
  models: Loadable<VehicleModel[]>;
  trims: Loadable<VehicleTrim[]>;
  detail: Loadable<VehicleDetail | null>;

  year: number | null;
  makeId: number | null;
  modelId: number | null;
  trimId: number | null;

  setYear: (year: number | null) => void;
  setMakeId: (makeId: number | null) => void;
  setModelId: (modelId: number | null) => void;
  setTrimId: (trimId: number | null) => void;
  /** Jump straight to a trim, as picking a popular vehicle does. */
  selectTrim: (trim: VehicleTrim) => void;
  clear: () => void;
  reloadYears: () => void;
}

export function useVehicleSelection(): VehicleSelection {
  const [year, setYearState] = useState<number | null>(null);
  const [makeId, setMakeIdState] = useState<number | null>(null);
  const [modelId, setModelIdState] = useState<number | null>(null);
  const [trimId, setTrimIdState] = useState<number | null>(null);

  const [years, reloadYears] = useList<number>(true, "years", (signal) => getYears(signal));

  const [makes] = useList<VehicleMake>(
    year !== null,
    `makes:${year}`,
    (signal) => getMakes(year ?? undefined, signal),
  );

  const [models] = useList<VehicleModel>(
    year !== null && makeId !== null,
    `models:${makeId}:${year}`,
    (signal) => getModels(makeId as number, year ?? undefined, signal),
  );

  const [trims] = useList<VehicleTrim>(
    year !== null && modelId !== null,
    `trims:${modelId}:${year}`,
    (signal) => getTrims(modelId as number, year as number, signal),
  );

  /* Vehicle details are a single object rather than a list, so they get their
   * own effect: "not found" is a distinct state a list never has. */
  const [detail, setDetail] = useState<Loadable<VehicleDetail | null>>(idle<VehicleDetail | null>(null));

  useEffect(() => {
    if (trimId === null) {
      setDetail(idle<VehicleDetail | null>(null));
      return undefined;
    }
    const controller = new AbortController();
    setDetail({ status: "loading", data: null, error: null });

    getVehicle(trimId, controller.signal).then(
      (data) => {
        if (controller.signal.aborted) return;
        setDetail({ status: "ready", data, error: null });
      },
      (error) => {
        if (controller.signal.aborted || isCancelled(error)) return;
        const message = error instanceof ApiError && error.isNotFound
          ? "That vehicle is no longer in the catalog."
          : describeError(error);
        setDetail({ status: "error", data: null, error: message });
      },
    );

    return () => controller.abort();
  }, [trimId]);

  /* Clearing downward is what keeps a selection internally consistent. */
  const setYear = useCallback((next: number | null) => {
    setYearState(next);
    setMakeIdState(null);
    setModelIdState(null);
    setTrimIdState(null);
  }, []);

  const setMakeId = useCallback((next: number | null) => {
    setMakeIdState(next);
    setModelIdState(null);
    setTrimIdState(null);
  }, []);

  const setModelId = useCallback((next: number | null) => {
    setModelIdState(next);
    setTrimIdState(null);
  }, []);

  const setTrimId = useCallback((next: number | null) => {
    setTrimIdState(next);
  }, []);

  const selectTrim = useCallback((trim: VehicleTrim) => {
    setYearState(trim.year);
    setMakeIdState(null);
    setModelIdState(null);
    setTrimIdState(trim.id);
  }, []);

  const clear = useCallback(() => {
    setYearState(null);
    setMakeIdState(null);
    setModelIdState(null);
    setTrimIdState(null);
  }, []);

  return {
    years, makes, models, trims, detail,
    year, makeId, modelId, trimId,
    setYear, setMakeId, setModelId, setTrimId, selectTrim, clear, reloadYears,
  };
}

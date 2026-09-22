/*
 * GET /vehicles/popular.
 *
 * Kept out of the component so the list stays a presentation concern and the
 * request keeps its cancellation and retry behaviour in one place.
 */
import { useCallback, useEffect, useState } from "react";

import { describeError, getPopularVehicles, isCancelled, ApiError, NetworkError } from "../lib/api/client";
import type { VehicleTrim } from "../lib/api/types";
import type { LoadStatus } from "./useVehicleSelection";

export interface PopularVehicles {
  status: LoadStatus;
  data: VehicleTrim[];
  error: string | null;
  correlationId: string | null;
  /** True when the API was never reached, which reads differently to a 500. */
  offline: boolean;
  reload: () => void;
}

export function usePopularVehicles(): PopularVehicles {
  const [state, setState] = useState<Omit<PopularVehicles, "reload">>({
    status: "loading",
    data: [],
    error: null,
    correlationId: null,
    offline: false,
  });
  const [token, setToken] = useState(0);
  const reload = useCallback(() => setToken((n) => n + 1), []);

  useEffect(() => {
    const controller = new AbortController();
    setState((s) => ({ ...s, status: "loading", error: null }));

    getPopularVehicles(controller.signal).then(
      (data) => {
        if (controller.signal.aborted) return;
        setState({
          status: data.length ? "ready" : "empty",
          data,
          error: null,
          correlationId: null,
          offline: false,
        });
      },
      (error) => {
        if (controller.signal.aborted || isCancelled(error)) return;
        setState({
          status: "error",
          data: [],
          error: describeError(error),
          correlationId: error instanceof ApiError ? error.correlationId : null,
          offline: error instanceof NetworkError,
        });
      },
    );

    return () => controller.abort();
  }, [token]);

  return { ...state, reload };
}

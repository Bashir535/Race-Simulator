/*
 * Playback clock for a finished race.
 *
 * The backend already ran the race; this only decides what moment of it is
 * on screen. It advances a clock with requestAnimationFrame and hands the
 * elapsed race time to a callback.
 *
 * That callback writes straight to the DOM through refs. React state changes
 * here only when the status does — idle, playing, paused, finished — so a
 * twelve-second race is four re-renders, not twelve hundred.
 *
 * Playback runs at real time: one second of race is one second on screen.
 */
import { useCallback, useEffect, useRef, useState } from "react";

export type PlaybackStatus = "idle" | "playing" | "paused" | "finished";

export interface PlaybackControls {
  status: PlaybackStatus;
  /** True when the viewer asked for reduced motion; playback jumps to the end. */
  reducedMotion: boolean;
  start: () => void;
  pause: () => void;
  resume: () => void;
  restart: () => void;
}

function prefersReducedMotion(): boolean {
  if (typeof window === "undefined" || !window.matchMedia) return false;
  return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

/**
 * @param durationSeconds length of the backend timeline, 0 when there is none
 * @param onFrame         called with the current race time, every animation frame
 * @param onComplete      called once when playback reaches the end
 */
export function usePlayback(
  durationSeconds: number,
  onFrame: (timeSeconds: number) => void,
  onComplete?: () => void,
): PlaybackControls {
  const [status, setStatus] = useState<PlaybackStatus>("idle");
  /* Mirrors `status` for the callbacks below: a state updater must stay
   * pure, and React may invoke one twice, so the transitions read the ref
   * and set state as a plain value rather than deciding inside an updater. */
  const statusRef = useRef<PlaybackStatus>("idle");

  const applyStatus = useCallback((next: PlaybackStatus) => {
    statusRef.current = next;
    setStatus(next);
  }, []);
  const [reducedMotion, setReducedMotion] = useState(prefersReducedMotion);

  const rafRef = useRef<number | null>(null);
  /* Race time already played, banked when paused. */
  const elapsedRef = useRef(0);
  /* Wall-clock reading when the current run segment began. */
  const segmentStartRef = useRef(0);

  /* Latest callbacks, so the rAF loop never closes over a stale render. */
  const onFrameRef = useRef(onFrame);
  const onCompleteRef = useRef(onComplete);
  useEffect(() => { onFrameRef.current = onFrame; }, [onFrame]);
  useEffect(() => { onCompleteRef.current = onComplete; }, [onComplete]);

  useEffect(() => {
    if (typeof window === "undefined" || !window.matchMedia) return undefined;
    const query = window.matchMedia("(prefers-reduced-motion: reduce)");
    const update = () => setReducedMotion(query.matches);
    query.addEventListener("change", update);
    return () => query.removeEventListener("change", update);
  }, []);

  const cancel = useCallback(() => {
    if (rafRef.current !== null) {
      cancelAnimationFrame(rafRef.current);
      rafRef.current = null;
    }
  }, []);

  useEffect(() => cancel, [cancel]);

  const finish = useCallback(() => {
    cancel();
    elapsedRef.current = durationSeconds;
    onFrameRef.current(durationSeconds);
    applyStatus("finished");
    onCompleteRef.current?.();
  }, [applyStatus, cancel, durationSeconds]);

  const tick = useCallback(() => {
    const now = performance.now();
    const time = elapsedRef.current + (now - segmentStartRef.current) / 1000;

    if (time >= durationSeconds) {
      finish();
      return;
    }
    onFrameRef.current(time);
    rafRef.current = requestAnimationFrame(tick);
  }, [durationSeconds, finish]);

  const run = useCallback(() => {
    segmentStartRef.current = performance.now();
    cancel();
    rafRef.current = requestAnimationFrame(tick);
    applyStatus("playing");
  }, [applyStatus, cancel, tick]);

  const start = useCallback(() => {
    if (durationSeconds <= 0) return;
    elapsedRef.current = 0;
    /* Nobody asked for motion, so show the finished race rather than animate. */
    if (reducedMotion) {
      finish();
      return;
    }
    onFrameRef.current(0);
    run();
  }, [durationSeconds, finish, reducedMotion, run]);

  const pause = useCallback(() => {
    if (statusRef.current !== "playing") return;
    cancel();
    elapsedRef.current += (performance.now() - segmentStartRef.current) / 1000;
    applyStatus("paused");
  }, [applyStatus, cancel]);

  const resume = useCallback(() => {
    if (statusRef.current !== "paused") return;
    segmentStartRef.current = performance.now();
    cancel();
    rafRef.current = requestAnimationFrame(tick);
    applyStatus("playing");
  }, [applyStatus, cancel, tick]);

  const restart = useCallback(() => {
    cancel();
    elapsedRef.current = 0;
    applyStatus("idle");
    onFrameRef.current(0);
  }, [applyStatus, cancel]);

  return { status, reducedMotion, start, pause, resume, restart };
}

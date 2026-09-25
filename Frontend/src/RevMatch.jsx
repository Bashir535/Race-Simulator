import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer,
} from "recharts";
import {
  Flag, Gauge, Play, Pause, Repeat, RotateCcw, Car, Warehouse,
} from "lucide-react";

import { C, F, RADIUS, inputStyle } from "./theme.js";
import { ApiError, NetworkError, describeError, isCancelled, simulateRace } from "./lib/api/client.ts";
import {
  RACE_OPTIONS, ROAD_SURFACES, buildRaceRequest, chartRows,
  progressOf, telemetryAt, timelineDuration,
} from "./lib/raceModel.ts";
import { mpsToMph } from "./lib/units.ts";
import { loadGarage, saveGarage, toBuild } from "./lib/garage.js";
import { usePlayback } from "./hooks/usePlayback.ts";
import { usePopularVehicles } from "./hooks/usePopularVehicles.ts";
import { useVehicleSelection } from "./hooks/useVehicleSelection.ts";
import GarageTab from "./components/GarageTab.jsx";
import VehiclePicker from "./components/VehiclePicker.jsx";
import StartTree, { TREE_STAGES } from "./components/StartTree.jsx";
import TrackScene from "./components/TrackScene.jsx";
import TelemetryHud from "./components/TelemetryHud.jsx";
import RaceResults from "./components/RaceResults.jsx";
import RaceExplanation from "./components/RaceExplanation.jsx";
import Field from "./components/Field.jsx";
import { Announcer, ErrorBlock } from "./components/States.jsx";

/* Start-tree countdown, in the spirit of a sportsman tree: the cars stage, then
 * three ambers drop in sequence before the green releases them. */
const STAGE_HOLD_MS = 700;
const AMBER_INTERVAL_MS = 600;
const GREEN_HOLD_MS = 500;

function treeLabel(status, stage) {
  if (status === "finished") return "FINISH";
  if (status === "playing" || stage >= TREE_STAGES) return "GO";
  if (stage >= 1) return String(TREE_STAGES - stage);
  if (stage === 0) return "STAGE";
  return "READY";
}

/* ---------------------------------------------------------------------- */
/* Lane                                                                   */
/*                                                                        */
/* Positioned imperatively during playback, for the same reason the        */
/* telemetry gauges are: sixty state updates a second would re-render the  */
/* page around it.                                                        */
/* ---------------------------------------------------------------------- */
const Lane = React.forwardRef(function Lane({ name, color, running }, ref) {
  const carRef = useRef(null);

  React.useImperativeHandle(ref, () => ({
    setProgress(fraction) {
      const clamped = Math.min(1, Math.max(0, fraction || 0));
      if (carRef.current) {
        carRef.current.style.left = `calc(${clamped * 100}% - 19px)`;
      }
    },
  }), []);

  return (
    <div style={{
      position: "relative", height: 54, marginBottom: 12, borderRadius: RADIUS,
      background: "#ffffff14", border: "1px solid #ffffff22", overflow: "hidden",
    }}>
      {/* Speed smear trailing the car, in that lane's color. */}
      <div style={{
        position: "absolute", inset: 0, opacity: 0.3,
        background: `linear-gradient(90deg, transparent, ${color})`,
      }} />
      <div
        ref={carRef}
        style={{
          position: "absolute", top: "50%", left: "calc(0% - 19px)",
          transform: "translateY(-50%)", transition: running ? "none" : "left .35s ease",
          display: "flex", alignItems: "center", gap: 4,
        }}
      >
        <div style={{
          width: 38, height: 38, borderRadius: 999, background: color, display: "flex",
          alignItems: "center", justifyContent: "center",
          border: "2px solid #ffffffcc", boxShadow: "0 2px 6px #00000055",
        }}>
          <Car size={19} color={C.onAccent} aria-hidden="true" />
        </div>
      </div>
      <div style={{
        position: "absolute", left: 14, top: "50%", transform: "translateY(-50%)",
        fontFamily: F.mono, fontSize: 13, color: "#ffffffdd",
        textShadow: "0 1px 2px #00000066", pointerEvents: "none",
      }}>
        {name}
      </div>
    </div>
  );
});

function TabButton({ active, onClick, icon, children, badge }) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-current={active ? "page" : undefined}
      style={{
        display: "flex", alignItems: "center", gap: 7, background: active ? C.panel : "transparent",
        border: `1px solid ${active ? C.line : "transparent"}`, borderRadius: RADIUS,
        boxShadow: active ? "0 1px 3px #12384a1f" : "none",
        color: active ? C.text : C.dim, padding: "8px 14px", cursor: "pointer",
        fontFamily: F.mono, fontSize: 13, letterSpacing: 0.4,
      }}
    >
      {icon}
      {children}
      {badge != null && badge > 0 && (
        <span style={{
          background: C.orange, color: C.onAccent, borderRadius: RADIUS, fontSize: 10,
          fontWeight: 700, padding: "1px 6px", lineHeight: 1.6,
        }}>
          {badge}
        </span>
      )}
    </button>
  );
}

const controlButton = (enabled) => ({
  display: "flex", alignItems: "center", gap: 6, background: "transparent",
  color: C.dim, border: `1px solid ${C.line}`, borderRadius: RADIUS,
  padding: "10px 12px", cursor: enabled ? "pointer" : "default",
  opacity: enabled ? 1 : 0.5,
});

/* ---------------------------------------------------------------------- */
/* Main component                                                         */
/* ---------------------------------------------------------------------- */
export default function RevMatch() {
  const [tab, setTab] = useState("dragstrip");

  const laneA = useVehicleSelection();
  const laneB = useVehicleSelection();
  const popular = usePopularVehicles();

  const [raceKey, setRaceKey] = useState("drag-quarter");
  const [roadSurface, setRoadSurface] = useState("PREPARED_DRAG_STRIP");

  /* The request, kept separate from playback: a race can be simulated and then
   * replayed many times without asking the backend again. */
  const [requestStatus, setRequestStatus] = useState("idle");
  const [raceError, setRaceError] = useState(null);
  const [response, setResponse] = useState(null);
  const [resultsVisible, setResultsVisible] = useState(false);
  const [treeStage, setTreeStage] = useState(-1);

  const [garage, setGarage] = useState(() => loadGarage());
  const [storageAvailable, setStorageAvailable] = useState(true);
  const [savedFlash, setSavedFlash] = useState({ a: false, b: false });

  const laneARef = useRef(null);
  const laneBRef = useRef(null);
  const hudARef = useRef(null);
  const hudBRef = useRef(null);
  const trackRef = useRef(null);
  const timersRef = useRef([]);
  const flashTimersRef = useRef([]);
  const abortRef = useRef(null);
  /* Read inside the animation loop, which must not close over stale renders. */
  const responseRef = useRef(null);
  const optionRef = useRef(RACE_OPTIONS[raceKey]);
  const redlinesRef = useRef({ a: null, b: null });

  const option = RACE_OPTIONS[raceKey];
  useEffect(() => { optionRef.current = RACE_OPTIONS[raceKey]; }, [raceKey]);
  useEffect(() => { responseRef.current = response; }, [response]);

  useEffect(() => {
    redlinesRef.current = {
      a: laneA.detail.data?.engine?.redlineRpm ?? null,
      b: laneB.detail.data?.engine?.redlineRpm ?? null,
    };
  }, [laneA.detail.data, laneB.detail.data]);

  const clearTimers = useCallback(() => {
    timersRef.current.forEach(clearTimeout);
    timersRef.current = [];
  }, []);

  useEffect(() => () => {
    clearTimers();
    flashTimersRef.current.forEach(clearTimeout);
    abortRef.current?.abort();
  }, [clearTimers]);

  /* ---- playback ------------------------------------------------------- */

  /* One frame of playback: read both cars out of the backend timeline and
   * write them to the DOM. No state is set here. */
  const handleFrame = useCallback((time) => {
    const current = responseRef.current;
    if (!current) return;
    const opt = optionRef.current;
    const frames = current.timeline;

    const a = telemetryAt(frames, time, "vehicleA");
    const b = telemetryAt(frames, time, "vehicleB");
    const progressA = progressOf(a, opt);
    const progressB = progressOf(b, opt);

    laneARef.current?.setProgress(progressA);
    laneBRef.current?.setProgress(progressB);
    trackRef.current?.setProgress(Math.max(progressA, progressB));
    hudARef.current?.update(a, redlinesRef.current.a);
    hudBRef.current?.update(b, redlinesRef.current.b);
  }, []);

  const handleComplete = useCallback(() => {
    setResultsVisible(true);
  }, []);

  const duration = response ? timelineDuration(response) : 0;
  const playback = usePlayback(duration, handleFrame, handleComplete);
  const { start: startPlayback, restart: restartPlayback } = playback;
  const playbackStatus = playback.status;

  /* ---- selection validity --------------------------------------------- */

  const trimIdA = laneA.trimId;
  const trimIdB = laneB.trimId;
  const bothChosen = trimIdA !== null && trimIdB !== null;
  const sameVehicle = bothChosen && trimIdA === trimIdB;
  const busy = requestStatus === "pending" || playback.status === "playing";

  const invalidReason = !bothChosen
    ? "Choose a vehicle for both lanes to race."
    : sameVehicle
      ? "Pick two different vehicles — a car cannot race itself."
      : null;

  const canRace = bothChosen && !sameVehicle && !busy;

  const nameA = laneA.detail.data
    ? [laneA.detail.data.identity.year, laneA.detail.data.identity.make, laneA.detail.data.identity.model]
      .filter(Boolean).join(" ")
    : "Lane A";
  const nameB = laneB.detail.data
    ? [laneB.detail.data.identity.year, laneB.detail.data.identity.make, laneB.detail.data.identity.model]
      .filter(Boolean).join(" ")
    : "Lane B";

  /* ---- running a race -------------------------------------------------- */

  /* Replay the timeline already in hand. Stage 6 playback is a recording, so
   * watching it again must not cost another simulation. */
  const replayRace = useCallback(() => {
    if (!responseRef.current) return;
    restartPlayback();
    setResultsVisible(false);
    hudARef.current?.reset();
    hudBRef.current?.reset();
    /* restart() lands on "idle", which the effect below turns back into play. */
  }, [restartPlayback]);

  const resetRace = useCallback(() => {
    clearTimers();
    abortRef.current?.abort();
    abortRef.current = null;
    setRequestStatus("idle");
    setRaceError(null);
    setResponse(null);
    setResultsVisible(false);
    setTreeStage(-1);
    restartPlayback();
    laneARef.current?.setProgress(0);
    laneBRef.current?.setProgress(0);
    trackRef.current?.setProgress(0);
    hudARef.current?.reset();
    hudBRef.current?.reset();
  }, [clearTimers, restartPlayback]);

  function runRace() {
    if (!canRace) return;

    clearTimers();
    abortRef.current?.abort();
    /* A completed playback stays in the "finished" state. Return it and all
     * imperative displays to the starting line before requesting another
     * simulation, otherwise the new response cannot auto-start. */
    restartPlayback();
    laneARef.current?.setProgress(0);
    laneBRef.current?.setProgress(0);
    trackRef.current?.setProgress(0);
    const controller = new AbortController();
    abortRef.current = controller;

    setRaceError(null);
    setResponse(null);
    setResultsVisible(false);
    setRequestStatus("pending");
    setTreeStage(0);
    hudARef.current?.reset();
    hudBRef.current?.reset();

    const request = buildRaceRequest(trimIdA, trimIdB, RACE_OPTIONS[raceKey], roadSurface);

    /* Ask the backend while the tree counts down, so the wait sits behind the
     * ambers instead of stalling on green. */
    const pending = simulateRace(request, controller.signal).then(
      (race) => ({ race }),
      (error) => ({ error }),
    );

    const at = (ms, fn) => timersRef.current.push(setTimeout(fn, ms));
    for (let n = 1; n <= 3; n += 1) {
      at(STAGE_HOLD_MS + (n - 1) * AMBER_INTERVAL_MS, () => setTreeStage(n));
    }
    at(STAGE_HOLD_MS + 3 * AMBER_INTERVAL_MS, () => {
      setTreeStage(TREE_STAGES);
      at(GREEN_HOLD_MS, async () => {
        const { race, error } = await pending;
        if (controller.signal.aborted) return;

        if (error) {
          if (isCancelled(error)) return;
          setRaceError({
            message: describeError(error),
            correlationId: error instanceof ApiError ? error.correlationId : null,
            offline: error instanceof NetworkError,
          });
          setRequestStatus("error");
          setTreeStage(-1);
          return;
        }

        setResponse(race);
        responseRef.current = race;
        setRequestStatus("ready");
      });
    });
  }

  /* Playback begins once the response is in and the tree has released. */
  useEffect(() => {
    if (requestStatus === "ready" && response && playbackStatus === "idle") {
      startPlayback();
    }
  }, [requestStatus, response, playbackStatus, startPlayback]);

  /* ---- garage ---------------------------------------------------------- */

  const saveTrim = useCallback((lane) => (trim) => {
    setGarage((current) => {
      const next = [...current, toBuild(trim)];
      setStorageAvailable(saveGarage(next));
      return next;
    });
    setSavedFlash((f) => ({ ...f, [lane]: true }));
    flashTimersRef.current.push(
      setTimeout(() => setSavedFlash((f) => ({ ...f, [lane]: false })), 1600),
    );
  }, []);

  const removeFromGarage = useCallback((id) => {
    setGarage((current) => {
      const next = current.filter((b) => b.id !== id);
      setStorageAvailable(saveGarage(next));
      return next;
    });
  }, []);

  const renameBuild = useCallback((id, name) => {
    setGarage((current) => {
      const next = current.map((b) => (b.id === id ? { ...b, name } : b));
      setStorageAvailable(saveGarage(next));
      return next;
    });
  }, []);

  /* Loading a saved or popular car drops you back on the strip with it fitted. */
  const loadTrimInto = useCallback((selection) => (trimLike) => {
    selection.selectTrim({
      id: trimLike.trimId ?? trimLike.id,
      year: trimLike.year,
    });
    setTab("dragstrip");
    resetRace();
  }, [resetRace]);

  /* ---- charts ---------------------------------------------------------- */

  const chartData = useMemo(() => {
    if (!response) return [];
    return chartRows(response).map((row) => ({
      t: +row.t.toFixed(2),
      vA: mpsToMph(row.speedA),
      vB: mpsToMph(row.speedB),
    }));
  }, [response]);

  const running = playback.status === "playing";
  const statusMessage = requestStatus === "pending" ? "Simulating the race…"
    : requestStatus === "error" ? "The race could not be simulated."
      : playback.status === "finished" ? "Race complete. Results are below."
        : "";

  return (
    <div style={{
      background: C.bg, minHeight: "100%", color: C.text,
      fontFamily: F.body, padding: "28px 20px",
    }}>
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=IBM+Plex+Mono:wght@400;500;600&family=Inter:wght@400;500;600;700&display=swap');
        select:focus-visible, button:focus-visible, input:focus-visible {
          outline: 2px solid ${C.cyan};
          outline-offset: 2px;
        }
        select option, select optgroup { background: ${C.panel}; }
        @media (max-width: 720px) {
          .rb-grid { grid-template-columns: 1fr !important; }
          .rb-strip { flex-direction: column !important; align-items: stretch !important; gap: 14px !important; }
          .rb-strip > :first-child { align-self: center; }
        }
      `}</style>

      <Announcer message={statusMessage} />

      <div style={{ maxWidth: 1080, margin: "0 auto" }}>
        {/* Header */}
        <div style={{
          display: "flex", flexWrap: "wrap", justifyContent: "space-between",
          alignItems: "flex-end", gap: 16, marginBottom: 18,
        }}>
          <div>
            <h1 style={{ fontFamily: F.display, fontWeight: 700, fontSize: 42, margin: 0 }}>
              RevMatch
            </h1>
            <p style={{ color: C.dim, margin: "4px 0 0", fontSize: 14, maxWidth: 460 }}>
              Pick two cars out of the catalog and let the physics engine settle the dispute.
            </p>
          </div>

          {tab === "dragstrip" && (
            <div style={{ display: "flex", gap: 10, alignItems: "center", flexWrap: "wrap" }}>
              <select
                aria-label="Race type"
                style={{ ...inputStyle, width: "auto" }}
                value={raceKey}
                disabled={busy}
                onChange={(e) => { setRaceKey(e.target.value); resetRace(); }}
              >
                {Object.entries(RACE_OPTIONS).map(([key, value]) => (
                  <option key={key} value={key}>{value.label}</option>
                ))}
              </select>

              <select
                aria-label="Road surface"
                style={{ ...inputStyle, width: "auto" }}
                value={roadSurface}
                disabled={busy}
                onChange={(e) => { setRoadSurface(e.target.value); resetRace(); }}
              >
                {ROAD_SURFACES.map((s) => (
                  <option key={s.value} value={s.value}>{s.label}</option>
                ))}
              </select>

              <button
                type="button"
                onClick={runRace}
                disabled={!canRace}
                title={invalidReason || undefined}
                style={{
                  display: "flex", alignItems: "center", gap: 8, background: C.orange, color: C.onAccent,
                  border: "none", borderRadius: RADIUS, padding: "10px 18px", fontWeight: 700, fontSize: 14,
                  boxShadow: canRace ? "0 2px 6px #e2443640" : "none",
                  cursor: canRace ? "pointer" : "default", opacity: canRace ? 1 : 0.6,
                }}
              >
                <Play size={15} aria-hidden="true" />
                {requestStatus === "pending" ? "Simulating…" : running ? "Racing…" : "Run race"}
              </button>

              {response && (
                <button
                  type="button"
                  onClick={() => (playback.status === "playing" ? playback.pause() : playback.resume())}
                  disabled={playback.status !== "playing" && playback.status !== "paused"}
                  style={controlButton(playback.status === "playing" || playback.status === "paused")}
                >
                  {playback.status === "playing"
                    ? <><Pause size={14} aria-hidden="true" /> Pause</>
                    : <><Play size={14} aria-hidden="true" /> Resume</>}
                </button>
              )}

              {response && (
                <button
                  type="button"
                  onClick={replayRace}
                  disabled={playback.status === "playing"}
                  style={controlButton(playback.status !== "playing")}
                >
                  <Repeat size={14} aria-hidden="true" /> Replay
                </button>
              )}

              <button
                type="button"
                onClick={resetRace}
                style={controlButton(true)}
                aria-label="Clear the race and start over"
                title="Clear the race"
              >
                <RotateCcw size={14} aria-hidden="true" />
              </button>
            </div>
          )}
        </div>

        {/* Tab strip */}
        <div style={{
          display: "flex", gap: 4, borderBottom: `1px solid ${C.line}`,
          paddingBottom: 8, marginBottom: 22,
        }}>
          <TabButton active={tab === "dragstrip"} onClick={() => setTab("dragstrip")} icon={<Gauge size={14} aria-hidden="true" />}>
            DRAGSTRIP
          </TabButton>
          <TabButton active={tab === "garage"} onClick={() => setTab("garage")} icon={<Warehouse size={14} aria-hidden="true" />} badge={garage.length}>
            GARAGE
          </TabButton>
        </div>

        {tab === "garage" ? (
          <GarageTab
            garage={garage}
            popular={popular}
            storageAvailable={storageAvailable}
            onLoadA={loadTrimInto(laneA)}
            onLoadB={loadTrimInto(laneB)}
            onLoadPopularA={loadTrimInto(laneA)}
            onLoadPopularB={loadTrimInto(laneB)}
            onRemove={removeFromGarage}
            onRename={renameBuild}
          />
        ) : (
          <>
            {/* Track — the race is the point of the page, so it leads. */}
            <div style={{
              background: C.panel, border: `1px solid ${C.line}`, borderRadius: RADIUS,
              padding: 20, boxShadow: "0 1px 3px #12384a14", marginBottom: 20,
            }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 12 }}>
                <div style={{ display: "flex", alignItems: "center", gap: 6, color: C.dim, fontSize: 13 }}>
                  <Gauge size={15} aria-hidden="true" /> {option.label}
                </div>
                <Flag size={15} color={C.dim} aria-hidden="true" />
              </div>

              <div style={{ display: "flex", alignItems: "center", gap: 20 }} className="rb-strip">
                <StartTree stage={treeStage} label={treeLabel(playback.status, treeStage)} />
                <div style={{ flex: 1, minWidth: 0 }}>
                  <TrackScene ref={trackRef} progress={0} running={running}>
                    <Lane ref={laneARef} name={nameA} color={C.orange} running={running} />
                    <Lane ref={laneBRef} name={nameB} color={C.cyan} running={running} />
                  </TrackScene>
                </div>
              </div>

              {/* Gauges, fed from the timeline during playback. */}
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, marginTop: 14 }} className="rb-grid">
                <TelemetryHud ref={hudARef} name={nameA} color={C.orange} />
                <TelemetryHud ref={hudBRef} name={nameB} color={C.cyan} />
              </div>

              {playback.reducedMotion && response && (
                <p style={{ fontSize: 12, color: C.dim, margin: "10px 0 0" }}>
                  Playback is skipped because this device asks for reduced motion. The results
                  below are the complete race.
                </p>
              )}
            </div>

            {/* Why the race button is off. */}
            {invalidReason && (
              <div style={{
                background: C.panelAlt, border: `1px solid ${C.line}`, borderRadius: RADIUS,
                padding: "12px 14px", fontSize: 13, color: C.dim, marginBottom: 20,
              }}>
                {invalidReason}
              </div>
            )}

            {/* Vehicle pickers */}
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16, marginBottom: 20 }} className="rb-grid">
              <VehiclePicker
                label="Lane A" color={C.orange} selection={laneA} disabled={busy}
                onSaveToGarage={saveTrim("a")} garage={garage}
                onLoadFromGarage={loadTrimInto(laneA)} savedFlash={savedFlash.a}
              />
              <VehiclePicker
                label="Lane B" color={C.cyan} selection={laneB} disabled={busy}
                onSaveToGarage={saveTrim("b")} garage={garage}
                onLoadFromGarage={loadTrimInto(laneB)} savedFlash={savedFlash.b}
              />
            </div>

            {/* The simulation could not be run. */}
            {raceError && (
              <div style={{ marginBottom: 20 }}>
                <ErrorBlock
                  message={raceError.message}
                  correlationId={raceError.correlationId}
                  offline={raceError.offline}
                  onRetry={canRace ? runRace : undefined}
                />
              </div>
            )}

            {/* Results, once the playback has run through. */}
            {resultsVisible && <RaceResults response={response} />}

            {resultsVisible && response && (
              <div style={{ maxWidth: 760, margin: "0 auto 20px" }}>
                <div style={{
                  background: C.panel, border: `1px solid ${C.line}`, borderRadius: RADIUS,
                  padding: "16px 8px 8px 0", boxShadow: "0 1px 3px #12384a14",
                }}>
                  <div style={{ fontSize: 12, color: C.dim, padding: "0 16px 8px", fontFamily: F.mono }}>
                    SPEED VS TIME
                  </div>
                  <ResponsiveContainer width="100%" height={200}>
                    <LineChart data={chartData}>
                      <CartesianGrid stroke={C.line} strokeDasharray="3 3" />
                      <XAxis dataKey="t" stroke={C.dim} fontSize={11} unit="s" />
                      <YAxis stroke={C.dim} fontSize={11} unit="mph" width={50} />
                      <Tooltip contentStyle={{ background: C.panelAlt, border: `1px solid ${C.line}`, fontSize: 12 }} />
                      <Line type="monotone" dataKey="vA" stroke={C.orange} strokeWidth={2} dot={false} name={nameA} isAnimationActive={false} />
                      <Line type="monotone" dataKey="vB" stroke={C.cyan} strokeWidth={2} dot={false} name={nameB} isAnimationActive={false} />
                    </LineChart>
                  </ResponsiveContainer>
                </div>
              </div>
            )}

            {resultsVisible && <RaceExplanation response={response} />}

            {/* Engine flow */}
            <div style={{
              background: C.panel, border: `1px solid ${C.line}`, borderRadius: RADIUS,
              padding: 20, boxShadow: "0 1px 3px #12384a14",
            }}>
              <div style={{ fontSize: 12, color: C.dim, marginBottom: 12, fontFamily: F.mono }}>
                HOW THE ENGINE GETS FROM SPEC SHEET TO RESULT
              </div>
              <div style={{ display: "flex", flexWrap: "wrap", gap: 8, alignItems: "center" }}>
                {["Catalog", "Spec", "Power / torque", "Traction", "Acceleration", "Velocity", "Distance", "Result"]
                  .map((step, i, arr) => (
                    <React.Fragment key={step}>
                      <span style={{
                        border: `1px solid ${C.line}`, borderRadius: RADIUS, padding: "6px 12px",
                        fontSize: 12, color: C.text, fontFamily: F.mono,
                      }}>
                        {step}
                      </span>
                      {i < arr.length - 1 && <span style={{ color: C.dim }}>&rarr;</span>}
                    </React.Fragment>
                  ))}
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

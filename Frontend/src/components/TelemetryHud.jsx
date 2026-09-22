/*
 * Live gauges for one lane during playback.
 *
 * This component renders once and is then driven imperatively: the playback
 * loop calls `update(telemetry)` on its ref roughly sixty times a second and
 * the values are written straight to the DOM. Putting those values in React
 * state instead would re-render the whole tree on every animation frame.
 *
 * Every figure comes from a backend frame. The only arithmetic is the unit
 * conversion needed to print it.
 */
import React, { forwardRef, useImperativeHandle, useRef } from "react";

import { C, F, RADIUS } from "../theme.js";
import { mpsToMph } from "../lib/units.ts";

function Readout({ label, valueRef, unit, width }) {
  return (
    <div style={{ minWidth: width || 78 }}>
      <div style={{ fontFamily: F.mono, fontSize: 10, color: C.dim, letterSpacing: 0.3 }}>{label}</div>
      <div style={{ display: "flex", alignItems: "baseline", gap: 3 }}>
        <span
          ref={valueRef}
          style={{ fontFamily: F.mono, fontSize: 18, color: C.text, fontVariantNumeric: "tabular-nums" }}
        >
          0
        </span>
        {unit && <span style={{ fontSize: 11, color: C.dim }}>{unit}</span>}
      </div>
    </div>
  );
}

const TelemetryHud = forwardRef(function TelemetryHud({ name, color }, ref) {
  const speedRef = useRef(null);
  const rpmRef = useRef(null);
  const gearRef = useRef(null);
  const accelRef = useRef(null);
  const barRef = useRef(null);
  const flagRef = useRef(null);
  /* Remembered so a gear change can be spotted between two frames. */
  const lastGearRef = useRef(null);
  const shiftUntilRef = useRef(0);

  useImperativeHandle(ref, () => ({
    update(telemetry, redlineRpm) {
      if (speedRef.current) {
        speedRef.current.textContent = mpsToMph(telemetry.speedMetersPerSecond).toFixed(0);
      }
      if (rpmRef.current) {
        rpmRef.current.textContent = Math.round(telemetry.engineRpm).toLocaleString();
      }
      if (gearRef.current) {
        gearRef.current.textContent = telemetry.gear > 0 ? String(telemetry.gear) : "N";
      }
      if (accelRef.current) {
        accelRef.current.textContent = telemetry.accelerationMetersPerSecondSquared.toFixed(1);
      }
      /* Tachometer fill, against the engine's own redline when we know it. */
      if (barRef.current) {
        const ceiling = redlineRpm || 8000;
        const fraction = Math.max(0, Math.min(1, telemetry.engineRpm / ceiling));
        barRef.current.style.transform = `scaleX(${fraction})`;
      }
      /* A shift is a gear change between frames; hold the light briefly so it
       * is visible at playback speed. */
      const now = performance.now();
      if (lastGearRef.current !== null && telemetry.gear !== lastGearRef.current) {
        shiftUntilRef.current = now + 260;
      }
      lastGearRef.current = telemetry.gear;

      if (flagRef.current) {
        const shifting = now < shiftUntilRef.current;
        const label = shifting ? "SHIFT"
          : telemetry.launchActive ? "LAUNCH"
            : telemetry.tractionLimited ? "TRACTION"
              : telemetry.accelerationLimit || "";
        flagRef.current.textContent = label;
        flagRef.current.style.opacity = label ? "1" : "0";
        flagRef.current.style.color = shifting ? C.amberInk
          : telemetry.tractionLimited ? C.red : C.dim;
      }
    },
    reset() {
      lastGearRef.current = null;
      shiftUntilRef.current = 0;
      if (speedRef.current) speedRef.current.textContent = "0";
      if (rpmRef.current) rpmRef.current.textContent = "0";
      if (gearRef.current) gearRef.current.textContent = "N";
      if (accelRef.current) accelRef.current.textContent = "0.0";
      if (barRef.current) barRef.current.style.transform = "scaleX(0)";
      if (flagRef.current) flagRef.current.style.opacity = "0";
    },
  }), []);

  return (
    <div style={{
      background: C.panelAlt, border: `1px solid ${C.line}`, borderRadius: RADIUS, padding: "10px 12px",
    }}>
      <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 8 }}>
        <span style={{ width: 4, height: 14, borderRadius: RADIUS, background: color }} />
        <span style={{
          fontFamily: F.mono, fontSize: 11, color: C.dim, overflow: "hidden",
          textOverflow: "ellipsis", whiteSpace: "nowrap",
        }}>
          {name}
        </span>
        <span
          ref={flagRef}
          style={{
            marginLeft: "auto", fontFamily: F.mono, fontSize: 10, letterSpacing: 0.6,
            color: C.dim, opacity: 0, transition: "opacity .12s linear",
          }}
        />
      </div>

      {/* Tachometer. Scales from the left, so no layout work per frame. */}
      <div style={{ height: 4, background: C.line, borderRadius: RADIUS, overflow: "hidden", marginBottom: 10 }}>
        <div
          ref={barRef}
          style={{
            height: "100%", background: color, transform: "scaleX(0)",
            transformOrigin: "left center", borderRadius: RADIUS,
          }}
        />
      </div>

      <div style={{ display: "flex", flexWrap: "wrap", gap: 12 }}>
        <Readout label="SPEED" valueRef={speedRef} unit="mph" />
        <Readout label="RPM" valueRef={rpmRef} width={86} />
        <Readout label="GEAR" valueRef={gearRef} width={48} />
        <Readout label="ACCEL" valueRef={accelRef} unit="m/s²" width={86} />
      </div>
    </div>
  );
});

export default TelemetryHud;

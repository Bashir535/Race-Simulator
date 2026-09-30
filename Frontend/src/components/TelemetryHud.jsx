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

const DIAL_START = -125;
const DIAL_SWEEP = 250;

function Dial({ label, unit, color, needleRef, valueRef, labels }) {
  return (
    <div style={{ width: "min(150px, 38vw)", aspectRatio: "1", position: "relative", flex: "0 1 150px" }}>
      <svg viewBox="0 0 160 160" aria-hidden="true" style={{ width: "100%", height: "100%", display: "block" }}>
        <defs>
          <radialGradient id={`dial-${label}`} cx="50%" cy="42%" r="60%">
            <stop offset="0" stopColor="#263944" />
            <stop offset=".72" stopColor="#101a20" />
            <stop offset="1" stopColor="#070c0f" />
          </radialGradient>
        </defs>
        <circle cx="80" cy="80" r="74" fill={`url(#dial-${label})`} stroke="#78909c" strokeWidth="4" />
        <path d="M28 126 A68 68 0 1 1 132 126" fill="none" stroke={color} strokeWidth="5" opacity=".8" />
        {Array.from({ length: 21 }, (_, index) => {
          const angle = DIAL_START + (index / 20) * DIAL_SWEEP;
          const radians = (angle - 90) * Math.PI / 180;
          const major = index % 4 === 0;
          const outer = 68;
          const inner = major ? 57 : 62;
          return (
            <line key={index}
              x1={80 + inner * Math.cos(radians)} y1={80 + inner * Math.sin(radians)}
              x2={80 + outer * Math.cos(radians)} y2={80 + outer * Math.sin(radians)}
              stroke={major ? "#f4fbff" : "#78909c"} strokeWidth={major ? 2.5 : 1.2}
            />
          );
        })}
        {labels.map((tick, index) => {
          const angle = DIAL_START + (index / (labels.length - 1)) * DIAL_SWEEP;
          const radians = (angle - 90) * Math.PI / 180;
          return <text key={tick} x={80 + 48 * Math.cos(radians)} y={84 + 48 * Math.sin(radians)}
            fill="#dcebf1" fontSize="11" fontFamily={F.mono} textAnchor="middle">{tick}</text>;
        })}
        <g ref={needleRef} style={{ transformOrigin: "80px 80px", transform: `rotate(${DIAL_START}deg)`, transition: "transform 70ms linear" }}>
          <path d="M76 82 80 24 84 82Z" fill="#ff4d45" stroke="#ffd3cf" strokeWidth="1" />
        </g>
        <circle cx="80" cy="80" r="8" fill="#263238" stroke="#9fb2bb" strokeWidth="2" />
      </svg>
      <div style={{ position: "absolute", left: 0, right: 0, bottom: 20, textAlign: "center" }}>
        <div ref={valueRef} style={{ fontFamily: F.mono, fontSize: 22, lineHeight: 1, fontWeight: 700, color: "#fff", fontVariantNumeric: "tabular-nums" }}>0</div>
        <div style={{ fontFamily: F.mono, fontSize: 9, color, letterSpacing: 1 }}>{unit}</div>
      </div>
      <div style={{ position: "absolute", left: 0, right: 0, top: 42, textAlign: "center", fontFamily: F.mono, fontSize: 9, color: "#9fb2bb", letterSpacing: 1 }}>{label}</div>
    </div>
  );
}

const TelemetryHud = forwardRef(function TelemetryHud({ name, color }, ref) {
  const speedRef = useRef(null);
  const rpmRef = useRef(null);
  const gearRef = useRef(null);
  const accelRef = useRef(null);
  const speedNeedleRef = useRef(null);
  const rpmNeedleRef = useRef(null);
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
      const speedFraction = Math.max(0, Math.min(1, mpsToMph(telemetry.speedMetersPerSecond) / 200));
      const rpmCeiling = Math.max(1000, redlineRpm || 8000);
      const rpmFraction = Math.max(0, Math.min(1, telemetry.engineRpm / rpmCeiling));
      if (speedNeedleRef.current) speedNeedleRef.current.style.transform = `rotate(${DIAL_START + speedFraction * DIAL_SWEEP}deg)`;
      if (rpmNeedleRef.current) rpmNeedleRef.current.style.transform = `rotate(${DIAL_START + rpmFraction * DIAL_SWEEP}deg)`;
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
      if (speedNeedleRef.current) speedNeedleRef.current.style.transform = `rotate(${DIAL_START}deg)`;
      if (rpmNeedleRef.current) rpmNeedleRef.current.style.transform = `rotate(${DIAL_START}deg)`;
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

      <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: 8, flexWrap: "wrap" }}>
        <Dial label="RPM ×1000" unit="RPM" color={color} needleRef={rpmNeedleRef} valueRef={rpmRef} labels={[0, 2, 4, 6, 8]} />
        <div style={{ minWidth: 66, textAlign: "center", alignSelf: "center" }}>
          <div style={{ fontFamily: F.mono, fontSize: 9, color: C.dim, letterSpacing: 1 }}>GEAR</div>
          <div ref={gearRef} style={{ fontFamily: F.mono, fontSize: 46, lineHeight: 1.05, fontWeight: 700, color, fontVariantNumeric: "tabular-nums" }}>N</div>
          <div style={{ marginTop: 8, fontFamily: F.mono, fontSize: 9, color: C.dim }}>ACCEL</div>
          <div style={{ fontFamily: F.mono, fontSize: 13, color: C.text }}><span ref={accelRef}>0.0</span> m/s²</div>
        </div>
        <Dial label="SPEED" unit="MPH" color={color} needleRef={speedNeedleRef} valueRef={speedRef} labels={[0, 50, 100, 150, 200]} />
      </div>
    </div>
  );
});

export default TelemetryHud;

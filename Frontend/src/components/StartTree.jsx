import React from "react";
import { C, F, RADIUS } from "../theme.js";

/*
 * Drag-strip start tree.
 *
 * stage is the countdown position:
 *   -1  dark, nothing staged
 *    0  pre-stage / stage bulbs lit, cars on the line
 *  1-3  that many amber bulbs lit, counting down
 *    4  green — the race is under way
 */
export const TREE_STAGES = 4;

const BULB = 30;
const SMALL_BULB = 11;

function Bulb({ color, lit, size = BULB }) {
  return (
    <div
      style={{
        width: size,
        height: size,
        borderRadius: "50%",
        /* Unlit bulbs read as recessed glass; lit ones bloom out of the housing. */
        background: lit
          ? `radial-gradient(circle at 35% 30%, #fff 0%, ${color} 45%, ${color} 100%)`
          : "#151F27",
        border: `1px solid ${lit ? color : "#0B1218"}`,
        boxShadow: lit
          ? `0 0 ${size * 0.5}px ${color}, 0 0 ${size * 1.3}px ${color}66, inset 0 0 6px #ffffff55`
          : "inset 0 2px 5px #0009, inset 0 -1px 2px #ffffff14",
        transition: "background .09s linear, box-shadow .09s linear, border-color .09s linear",
      }}
    />
  );
}

export default function StartTree({ stage, label }) {
  const staged = stage >= 0;
  const ambersLit = Math.min(Math.max(stage, 0), 3);
  const green = stage >= TREE_STAGES;

  return (
    <div
      role="img"
      aria-label={
        green ? "Start tree: green, go"
          : ambersLit > 0 ? `Start tree: ${ambersLit} of 3 amber lights`
            : staged ? "Start tree: staged" : "Start tree: idle"
      }
      style={{ display: "flex", flexDirection: "column", alignItems: "center", userSelect: "none" }}
    >
      {/* Housing */}
      <div
        style={{
          display: "flex", flexDirection: "column", alignItems: "center", gap: 9,
          background: "linear-gradient(180deg, #2E3B45 0%, #18222A 100%)",
          border: "1px solid #141C22",
          borderRadius: RADIUS,
          padding: "12px 13px",
          boxShadow: "0 10px 20px #12384a2e",
        }}
      >
        {/* Pre-stage / stage bulbs: the cars are on the line. */}
        <div style={{ display: "flex", gap: 6, marginBottom: 2 }}>
          <Bulb color="#EDEFF2" lit={staged} size={SMALL_BULB} />
          <Bulb color="#EDEFF2" lit={staged} size={SMALL_BULB} />
        </div>

        {[1, 2, 3].map((n) => (
          <Bulb key={n} color={C.amber} lit={!green && ambersLit >= n} />
        ))}

        <Bulb color={C.green} lit={green} />
      </div>

      {/* Mast */}
      <div style={{ width: 8, height: 26, background: "linear-gradient(90deg, #18222A, #55656F, #18222A)" }} />
      <div style={{ width: 34, height: 5, borderRadius: RADIUS, background: "#55656F" }} />

      {label && (
        <div style={{
          marginTop: 8, fontFamily: F.mono, fontSize: 11,
          letterSpacing: 1.2, color: green ? C.greenInk : ambersLit > 0 ? C.amberInk : C.dim,
          transition: "color .12s linear",
        }}>
          {label}
        </div>
      )}
    </div>
  );
}

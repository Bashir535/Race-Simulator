/*
 * The popular vehicles the catalog flags, straight from GET /vehicles/popular.
 *
 * Each card carries its database trim id, which is what a lane and a race
 * request both use; nothing is matched by name.
 */
import React from "react";
import { Star } from "lucide-react";

import { C, F, RADIUS } from "../theme.js";
import { EmptyBlock, ErrorBlock, LoadingBlock } from "./States.jsx";
import { formatPounds, horsepowerPerTon } from "../lib/units.ts";

const laneButton = (color) => ({
  background: "transparent",
  border: `1px solid ${color}`,
  color,
  borderRadius: RADIUS,
  fontSize: 12,
  fontWeight: 600,
  padding: "5px 9px",
  cursor: "pointer",
});

function PopularCard({ trim, onLoadA, onLoadB }) {
  const name = [trim.year, trim.make, trim.model].filter(Boolean).join(" ");
  return (
    <div style={{
      background: C.panel, border: `1px solid ${C.line}`, borderLeft: `3px solid ${C.amber}`,
      borderRadius: RADIUS, padding: 16, boxShadow: "0 1px 3px #12384a14",
    }}>
      <div style={{ fontFamily: F.display, fontSize: 20, fontWeight: 700 }}>{name}</div>
      <div style={{ fontFamily: F.mono, fontSize: 11, color: C.dim }}>
        {[trim.trim, trim.generation].filter(Boolean).join(" · ")}
      </div>

      <div style={{ display: "flex", flexWrap: "wrap", gap: 14, margin: "10px 0 12px" }}>
        <span style={{ fontFamily: F.mono, fontSize: 12, color: C.text }}>
          {formatPounds(trim.massKg)}<span style={{ color: C.dim }}> lb</span>
        </span>
        {trim.horsepower != null && (
          <span style={{ fontFamily: F.mono, fontSize: 12, color: C.text }}>
            {Math.round(trim.horsepower)}<span style={{ color: C.dim }}> hp</span>
          </span>
        )}
        {trim.drivetrain && (
          <span style={{ fontFamily: F.mono, fontSize: 12, color: C.text }}>{trim.drivetrain}</span>
        )}
      </div>

      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 8 }}>
        <span style={{ fontFamily: F.mono, fontSize: 11, color: C.dim }}>
          {trim.horsepower != null ? `${Math.round(horsepowerPerTon(trim.horsepower, trim.massKg))} hp/ton` : ""}
        </span>
        <div style={{ display: "flex", gap: 6 }}>
          <button type="button" style={laneButton(C.orange)} onClick={() => onLoadA(trim)}>
            Lane A
          </button>
          <button type="button" style={laneButton(C.cyan)} onClick={() => onLoadB(trim)}>
            Lane B
          </button>
        </div>
      </div>
    </div>
  );
}

export default function PopularVehicles({ popular, onLoadA, onLoadB }) {
  return (
    <section style={{ marginBottom: 20 }}>
      <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 12 }}>
        <Star size={16} color={C.amberInk} aria-hidden="true" />
        <h2 style={{ fontFamily: F.display, fontSize: 22, fontWeight: 700, margin: 0 }}>
          Popular vehicles
        </h2>
        {popular.status === "ready" && (
          <span style={{ fontSize: 12, color: C.dim }}>{popular.data.length} from the catalog</span>
        )}
      </div>

      {popular.status === "loading" && <LoadingBlock label="Loading popular vehicles…" />}
      {popular.status === "empty" && <EmptyBlock label="The catalog has no popular vehicles yet." />}
      {popular.status === "error" && (
        <ErrorBlock
          message={popular.error}
          correlationId={popular.correlationId}
          offline={popular.offline}
          onRetry={popular.reload}
        />
      )}

      {popular.status === "ready" && (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(260px, 1fr))", gap: 12 }}>
          {popular.data.map((trim) => (
            <PopularCard key={trim.id} trim={trim} onLoadA={onLoadA} onLoadB={onLoadB} />
          ))}
        </div>
      )}
    </section>
  );
}

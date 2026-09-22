/*
 * Saved vehicles, plus the catalog's popular list.
 *
 * A saved entry holds a trim id and a nickname. Its specifications are not
 * stored: loading one into a lane re-reads it from the backend, so the garage
 * cannot drift away from the catalog.
 */
import React from "react";
import { Car, Trash2, Warehouse } from "lucide-react";

import { C, F, RADIUS, inputStyle } from "../theme.js";
import PopularVehicles from "./PopularVehicles.jsx";

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

function BuildCard({ build, onLoadA, onLoadB, onRemove, onRename }) {
  const provenance = [build.trim, build.generation].filter(Boolean).join(" · ");

  return (
    <div style={{
      background: C.panel, border: `1px solid ${C.line}`, borderLeft: `3px solid ${C.amber}`,
      borderRadius: RADIUS, padding: 16, boxShadow: "0 1px 3px #12384a14",
    }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: 8 }}>
        <div style={{ minWidth: 0 }}>
          <input
            aria-label="Saved vehicle name"
            value={build.name}
            onChange={(e) => onRename(build.id, e.target.value)}
            style={{
              ...inputStyle,
              background: "transparent",
              border: "1px solid transparent",
              padding: "2px 4px",
              marginLeft: -4,
              fontFamily: F.display,
              fontSize: 20,
              fontWeight: 700,
            }}
            onFocus={(e) => { e.target.style.border = `1px solid ${C.line}`; }}
            onBlur={(e) => { e.target.style.border = "1px solid transparent"; }}
          />
          {provenance && (
            <div style={{ fontSize: 11, color: C.dim, fontFamily: F.mono }}>{provenance}</div>
          )}
        </div>
        <button
          type="button"
          onClick={() => onRemove(build.id)}
          title="Remove from garage"
          aria-label={`Remove ${build.name} from garage`}
          style={{ background: "none", border: "none", color: C.dim, cursor: "pointer", display: "flex", padding: 4 }}
        >
          <Trash2 size={14} aria-hidden="true" />
        </button>
      </div>

      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 8, marginTop: 14 }}>
        <span style={{ fontFamily: F.mono, fontSize: 11, color: C.dim }}>
          trim #{build.trimId}
        </span>
        <div style={{ display: "flex", gap: 6 }}>
          <button type="button" style={laneButton(C.orange)} onClick={() => onLoadA(build)}>Lane A</button>
          <button type="button" style={laneButton(C.cyan)} onClick={() => onLoadB(build)}>Lane B</button>
        </div>
      </div>
    </div>
  );
}

export default function GarageTab({
  garage, popular, storageAvailable, onLoadA, onLoadB, onRemove, onRename,
  onLoadPopularA, onLoadPopularB,
}) {
  return (
    <div>
      <section style={{ marginBottom: 32 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 12 }}>
          <Warehouse size={18} color={C.amberInk} aria-hidden="true" />
          <h2 style={{ fontFamily: F.display, fontSize: 22, fontWeight: 700, margin: 0 }}>
            Your vehicles
          </h2>
          {garage.length > 0 && (
            <span style={{ fontSize: 12, color: C.dim }}>{garage.length} saved</span>
          )}
        </div>

        {!storageAvailable && (
          <div style={{
            background: C.panelAlt, border: `1px solid ${C.line}`, borderRadius: RADIUS,
            padding: "10px 12px", fontSize: 12, color: C.dim, marginBottom: 12,
          }}>
            This browser is blocking site storage, so vehicles saved here will be lost on reload.
          </div>
        )}

        {garage.length === 0 ? (
          <div style={{
            background: C.panel, border: `1px dashed ${C.line}`, borderRadius: RADIUS,
            padding: "28px 20px", textAlign: "center", color: C.dim, fontSize: 13,
          }}>
            <Car size={22} style={{ opacity: 0.5 }} aria-hidden="true" />
            <div style={{ marginTop: 8 }}>
              Nothing saved yet. Pick a vehicle on the Dragstrip tab and hit
              <strong style={{ color: C.text }}> Save to garage</strong>, or start from the
              popular list below.
            </div>
          </div>
        ) : (
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(260px, 1fr))", gap: 12 }}>
            {garage.map((b) => (
              <BuildCard
                key={b.id} build={b}
                onLoadA={onLoadA} onLoadB={onLoadB} onRemove={onRemove} onRename={onRename}
              />
            ))}
          </div>
        )}
      </section>

      <PopularVehicles popular={popular} onLoadA={onLoadPopularA} onLoadB={onLoadPopularB} />
    </div>
  );
}

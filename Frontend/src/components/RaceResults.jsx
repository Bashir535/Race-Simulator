/*
 * The race as the backend reported it.
 *
 * Every number on this panel is read straight off RaceResponse. Nothing is
 * derived beyond unit conversion for display, and nothing is inferred when a
 * field is absent — a missing milestone is simply not shown.
 */
import React from "react";
import { TrendingUp, Zap } from "lucide-react";

import { C, F, RADIUS } from "../theme.js";
import { formatMph, formatSeconds } from "../lib/units.ts";
import {
  MILESTONE_EIGHTH,
  MILESTONE_FINISH,
  MILESTONE_ZERO_TO_SIXTY,
  findMilestone,
} from "../lib/raceModel.ts";

function StatBlock({ label, value, unit, color }) {
  return (
    <div style={{ minWidth: 120 }}>
      <div style={{ fontFamily: F.mono, fontSize: 11, color: C.dim, letterSpacing: 0.3 }}>
        {label}
      </div>
      <div style={{ display: "flex", alignItems: "baseline", gap: 4 }}>
        <span style={{
          fontFamily: F.display, fontWeight: 700, fontSize: 30,
          color: color || C.text, lineHeight: 1,
        }}>
          {value}
        </span>
        {unit && <span style={{ fontSize: 13, color: C.dim }}>{unit}</span>}
      </div>
    </div>
  );
}

/* One car's column of authoritative figures. */
function VehicleColumn({ result, color }) {
  const name = result.name || "Vehicle";
  const zeroToSixty = findMilestone(result, MILESTONE_ZERO_TO_SIXTY);
  const eighth = findMilestone(result, MILESTONE_EIGHTH);
  const finish = findMilestone(result, MILESTONE_FINISH);

  return (
    <div style={{ minWidth: 240, flex: 1 }}>
      <div style={{
        display: "flex", alignItems: "center", gap: 8, marginBottom: 10,
        paddingBottom: 6, borderBottom: `1px solid ${C.lineSoft}`,
      }}>
        <span style={{ width: 4, height: 16, borderRadius: RADIUS, background: color }} />
        <span style={{ fontFamily: F.display, fontSize: 17, fontWeight: 700 }}>{name}</span>
      </div>

      <div style={{ display: "flex", flexWrap: "wrap", gap: 20 }}>
        <StatBlock
          label="FINISH TIME"
          value={formatSeconds(result.finishTimeSeconds)}
          unit="s"
          color={color}
        />
        <StatBlock
          label="FINISH SPEED"
          value={formatMph(result.finishSpeedMetersPerSecond)}
          unit="mph"
          color={color}
        />
        {zeroToSixty && (
          <StatBlock
            label="0–60 MPH"
            value={formatSeconds(zeroToSixty.elapsedSeconds)}
            unit="s"
            color={color}
          />
        )}
        {eighth && (
          <>
            <StatBlock
              label="1/8 MILE"
              value={formatSeconds(eighth.elapsedSeconds)}
              unit="s"
              color={color}
            />
            <StatBlock
              label="1/8 MILE SPEED"
              value={formatMph(eighth.speedMetersPerSecond)}
              unit="mph"
              color={color}
            />
          </>
        )}
        {finish && (
          <StatBlock
            label="FINISH TRAP"
            value={formatMph(finish.speedMetersPerSecond)}
            unit="mph"
            color={color}
          />
        )}
      </div>
    </div>
  );
}

export default function RaceResults({ response }) {
  if (!response) return null;

  const summary = response.summary;
  const leadChanges = summary?.leadChanges || [];

  return (
    <div
      style={{
        background: C.panel, border: `1px solid ${C.line}`, borderRadius: RADIUS,
        padding: 20, boxShadow: "0 1px 3px #12384a14", marginBottom: 20,
      }}
      role="region"
      aria-label="Race results"
    >
      <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 16, flexWrap: "wrap" }}>
        <Zap size={16} color={C.amberInk} aria-hidden="true" />
        <span style={{ fontFamily: F.display, fontSize: 18, fontWeight: 700 }}>
          {response.winner ? `${response.winner} wins` : "Race complete"}
        </span>
        <span style={{ color: C.dim, fontSize: 13 }}>
          by {formatSeconds(response.winningMarginSeconds, 3)}s
        </span>
        {response.simulationVersion && (
          <span style={{ fontFamily: F.mono, fontSize: 11, color: C.dim, marginLeft: "auto" }}>
            engine {response.simulationVersion}
          </span>
        )}
      </div>

      <div style={{ display: "flex", flexWrap: "wrap", gap: 28, marginBottom: 14 }}>
        <VehicleColumn result={response.vehicleA} color={C.orange} />
        <VehicleColumn result={response.vehicleB} color={C.cyan} />
      </div>

      {summary && (
        <div style={{
          borderTop: `1px solid ${C.lineSoft}`, paddingTop: 12,
          display: "flex", flexWrap: "wrap", gap: 20, fontSize: 13, color: C.dim,
        }}>
          <span style={{ display: "flex", alignItems: "center", gap: 6 }}>
            <TrendingUp size={14} aria-hidden="true" />
            {leadChanges.length === 0
              ? "No lead changes — led from the line."
              : `${leadChanges.length} lead change${leadChanges.length === 1 ? "" : "s"}, first at ${formatSeconds(leadChanges[0].timeSeconds, 1)}s to ${leadChanges[0].newLeader || "the other car"}.`}
          </span>
          {summary.largestLeadVehicle && (
            <span>
              Largest lead {summary.largestLeadMeters.toFixed(1)} m to {summary.largestLeadVehicle}.
            </span>
          )}
          <span>
            Gap at the winner&rsquo;s finish {summary.distanceGapAtWinnerFinishMeters.toFixed(1)} m.
          </span>
        </div>
      )}
    </div>
  );
}

/*
 * Post-race explanation.
 *
 * Written from the race the backend returned, in the browser, with no network
 * call and no model in the loop. The engine decides the outcome; this only
 * reads the result out in a sentence.
 *
 * The panel is mounted only while there is a result, so clearing the result
 * unmounts it and the text resets on its own.
 */
import React from "react";
import { Sparkles } from "lucide-react";

import { C, F, RADIUS } from "../theme.js";
import { formatMph, formatSeconds } from "../lib/units.ts";
import { MILESTONE_ZERO_TO_SIXTY, findMilestone } from "../lib/raceModel.ts";

/* Every clause below is guarded on the field it reads, so a missing milestone
 * drops its sentence rather than printing a blank. */
function describe(response) {
  const { vehicleA, vehicleB, summary } = response;
  const nameA = vehicleA.name || "Vehicle A";
  const nameB = vehicleB.name || "Vehicle B";
  const sentences = [];

  if (response.winner) {
    sentences.push(
      `${response.winner} took it by ${formatSeconds(response.winningMarginSeconds, 3)}s.`,
    );
  }

  sentences.push(
    `${nameA} crossed in ${formatSeconds(vehicleA.finishTimeSeconds)}s at `
    + `${formatMph(vehicleA.finishSpeedMetersPerSecond)} mph, `
    + `${nameB} in ${formatSeconds(vehicleB.finishTimeSeconds)}s at `
    + `${formatMph(vehicleB.finishSpeedMetersPerSecond)} mph.`,
  );

  const sixtyA = findMilestone(vehicleA, MILESTONE_ZERO_TO_SIXTY);
  const sixtyB = findMilestone(vehicleB, MILESTONE_ZERO_TO_SIXTY);
  if (sixtyA && sixtyB) {
    sentences.push(
      `Off the line, ${nameA} reached 60 mph in ${formatSeconds(sixtyA.elapsedSeconds)}s `
      + `against ${formatSeconds(sixtyB.elapsedSeconds)}s for ${nameB}.`,
    );
  }

  const leadChanges = summary?.leadChanges || [];
  if (leadChanges.length > 0) {
    const first = leadChanges[0];
    sentences.push(
      `The lead changed ${leadChanges.length === 1 ? "once" : `${leadChanges.length} times`}, `
      + `first at ${formatSeconds(first.timeSeconds, 1)}s`
      + `${first.newLeader ? ` to ${first.newLeader}` : ""}.`,
    );
  } else if (summary?.largestLeadVehicle) {
    sentences.push(
      `${summary.largestLeadVehicle} led throughout, by as much as `
      + `${summary.largestLeadMeters.toFixed(1)} m.`,
    );
  }

  return sentences.join(" ");
}

export default function RaceExplanation({ response }) {
  if (!response) return null;

  return (
    <div style={{
      background: C.panel, border: `1px solid ${C.line}`, borderRadius: RADIUS,
      padding: 20, boxShadow: "0 1px 3px #12384a14", marginBottom: 20,
    }}>
      <div style={{
        display: "flex", alignItems: "center", gap: 8, marginBottom: 10,
        color: C.dim, fontSize: 12, fontFamily: F.mono,
      }}>
        <Sparkles size={14} color={C.amberInk} aria-hidden="true" /> POST-RACE EXPLANATION
      </div>
      <p style={{ margin: 0, fontSize: 14, lineHeight: 1.6, color: C.text }}>
        {describe(response)}
      </p>
    </div>
  );
}

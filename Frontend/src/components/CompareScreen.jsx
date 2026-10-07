import React, { useEffect, useRef } from "react";
import { ArrowLeft, Flag } from "lucide-react";
import { C, F, RADIUS, actionStyle } from "../theme.js";
import { formatPounds, formatPoundFeet } from "../lib/units.ts";

const value = (n, suffix = "") => n == null ? "Not available" : `${n}${suffix}`;
const rpm = n => n == null ? "" : ` @ ${Math.round(n).toLocaleString()} rpm`;
const name = d => [d.identity.year, d.identity.make, d.identity.model].filter(Boolean).join(" ");

export default function CompareScreen({ a, b, onBack }) {
  const heading = useRef(null);
  useEffect(() => { heading.current?.focus(); }, []);
  if (!a || !b) return <section><p>Select two cars to compare.</p><button style={actionStyle} onClick={onBack}>Back to Dragstrip</button></section>;
  const performance = d => d.publishedPerformance?.length ? d.publishedPerformance.map((p, i) =>
    <div key={i}>
      {p.zeroToSixtySeconds != null && <div>0–60 mph: {p.zeroToSixtySeconds} s</div>}
      {p.quarterMileSeconds != null && <div>¼ mile: {p.quarterMileSeconds} s{p.quarterMileTrapSpeedMph != null ? ` @ ${p.quarterMileTrapSpeedMph} mph` : ""}</div>}
      <small style={{ color: C.dim }}>{p.source}</small>
    </div>) : "Not available";
  const rows = [
    ["Original MSRP", d => d.originalMsrpUsd == null ? "Not available" : new Intl.NumberFormat("en-US", { style: "currency", currency: "USD", maximumFractionDigits: 0 }).format(d.originalMsrpUsd)],
    ["Engine", d => value(d.engine?.name)],
    ["Horsepower", d => d.engine?.horsepower == null ? "Not available" : `${d.engine.horsepower} hp${rpm(d.engine.peakHorsepowerRpm)}`],
    ["Torque", d => d.engine?.torqueNm == null ? "Not available" : `${formatPoundFeet(d.engine.torqueNm)} lb-ft${rpm(d.engine.peakTorqueRpm)}`],
    ["Drive", d => ({ FWD: "Front-wheel drive", RWD: "Rear-wheel drive", AWD: "All-wheel drive" })[d.identity.drivetrain] || "Not available"],
    ["Transmission", d => value(d.transmission?.name)],
    ["Gearing", d => d.transmission ? <><div>Final drive: {d.transmission.finalDriveRatio}</div><div>{d.transmission.numberOfGears} forward gears</div><details><summary>All gear ratios</summary>{d.transmission.gearRatios?.map((r, i) => <div key={i}>Gear {i + 1}: {r}</div>)}</details></> : "Not available"],
    ["Curb weight", d => `${formatPounds(d.identity.massKg)} lb`],
    ["Tires", d => value(d.tireDescription)],
    ["Published performance", performance],
    ["Fuel efficiency", d => <><div>City: {value(d.fuelEfficiency?.cityMpg, " mpg")}</div><div>Highway: {value(d.fuelEfficiency?.highwayMpg, " mpg")}</div><div>Combined: {value(d.fuelEfficiency?.combinedMpg, " mpg")}</div></>],
  ];
  return <section aria-labelledby="comparison-title" style={{ background: C.panel, border: `1px solid ${C.line}`, borderRadius: RADIUS }}>
    <div style={{ display: "flex", justifyContent: "space-between", flexWrap: "wrap", alignItems: "center", gap: 16, padding: 24, background: C.panelAlt }}>
      <div><h2 id="comparison-title" ref={heading} tabIndex={-1} style={{ fontFamily: F.display, fontSize: 34, margin: 0 }}>Car Specs</h2><p style={{ color: C.dim, marginBottom: 0 }}>Compare your cars, then take them to the strip.</p></div>
      <button type="button" style={actionStyle} onClick={onBack}><ArrowLeft size={14} aria-hidden="true" /> Back to Dragstrip</button>
    </div>
    <style>{`.compare-scroll-hint { display: none; } @media (max-width: 660px) { .compare-scroll-hint { display: block; } }`}</style>
    <p className="compare-scroll-hint" style={{ padding: "0 18px", color: C.dim, fontSize: 12 }}>Swipe the table sideways to compare both cars.</p>
    <div tabIndex={0} role="region" aria-label="Vehicle specifications comparison" style={{ overflowX: "auto" }}>
      <table style={{ width: "100%", minWidth: 620, borderCollapse: "collapse", textAlign: "left", tableLayout: "fixed" }}>
        <colgroup><col style={{ width: "22%" }} /><col style={{ width: "39%" }} /><col style={{ width: "39%" }} /></colgroup>
        <thead><tr><th scope="col" style={{ padding: 18, background: C.panelAlt }}>Specification</th>{[a, b].map((d, i) => <th scope="col" key={i} style={{ padding: 18, background: i ? "#DFF3F7" : "#FFF0EC", borderTop: `3px solid ${i ? C.cyan : C.orange}`, verticalAlign: "top" }}><div style={{ fontSize: 19 }}>{name(d)}</div><div style={{ fontSize: 12, color: C.dim, marginTop: 6 }}>{d.identity.trim}</div></th>)}</tr></thead>
        <tbody>{rows.map(([label, render], i) => <tr key={label} style={{ background: i % 2 ? C.panel : C.panelAlt, borderTop: `1px solid ${C.line}` }}>
          <th scope="row" style={{ padding: "16px 18px", fontWeight: 500, color: C.dim }}>{label}</th>
          {[a, b].map((d, side) => <td key={side} style={{ padding: "16px 18px", verticalAlign: "top", overflowWrap: "anywhere", lineHeight: 1.6 }}>{render(d)}</td>)}
        </tr>)}</tbody>
      </table>
    </div>
    <div style={{ padding: 24, textAlign: "center" }}>
      <p style={{ color: C.dim, fontSize: 12 }}>Published figures are reference data, not predicted results. Timing conventions and test conditions vary. Missing specifications are shown as unavailable.</p>
      <button type="button" onClick={onBack} style={{ ...actionStyle, background: C.orange, color: C.onAccent, padding: "12px 32px" }}><Flag size={15} aria-hidden="true" /> Race these cars</button>
    </div>
  </section>;
}

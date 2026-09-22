/*
 * One lane's vehicle, chosen from the backend catalog.
 *
 * Year -> Make -> Model -> Trim, then the trim's details. Every option on
 * every select comes from the catalog endpoints; nothing here is a fixture,
 * and the value a lane carries is the database trim id.
 *
 * The card keeps the shape the build card had — accent bar, name, power to
 * weight on the right, provenance underneath — so the strip looks unchanged.
 */
import React from "react";
import { Check, Save } from "lucide-react";

import { C, F, RADIUS, inputStyle } from "../theme.js";
import Field from "./Field.jsx";
import { EmptyBlock, ErrorBlock, LoadingBlock } from "./States.jsx";
import { formatPoundFeet, formatPounds, horsepowerPerTon } from "../lib/units.ts";

/* A select whose options come from a Loadable list. Disabled until the level
 * above it has a value, which is what makes the cascade legible. */
function CascadeSelect({ label, placeholder, list, value, onChange, optionOf, disabled, waitingLabel }) {
  const busy = list.status === "loading";
  const options = list.data || [];

  return (
    <Field label={label}>
      <select
        style={{ ...inputStyle, opacity: disabled ? 0.55 : 1 }}
        disabled={disabled || busy}
        value={value ?? ""}
        onChange={(e) => onChange(e.target.value === "" ? null : Number(e.target.value))}
      >
        <option value="">
          {disabled ? waitingLabel : busy ? "Loading…" : placeholder}
        </option>
        {options.map((item) => {
          const { key, text } = optionOf(item);
          return <option key={key} value={key}>{text}</option>;
        })}
      </select>
    </Field>
  );
}

/* One figure from the trim record. */
function Spec({ label, value, unit }) {
  return (
    <div style={{ minWidth: 92 }}>
      <div style={{ fontFamily: F.mono, fontSize: 10, color: C.dim, letterSpacing: 0.3 }}>{label}</div>
      <div style={{ fontFamily: F.mono, fontSize: 13, color: C.text }}>
        {value}
        {unit && <span style={{ color: C.dim }}> {unit}</span>}
      </div>
    </div>
  );
}

/* Everything shown here is read back from GET /vehicles/{trimId}. */
function VehicleDetails({ detail }) {
  const trim = detail.identity;
  const engine = detail.engine;
  const transmission = detail.transmission;
  const published = (detail.publishedPerformance || [])[0] || null;

  return (
    <div style={{ borderTop: `1px solid ${C.lineSoft}`, paddingTop: 12, marginTop: 4 }}>
      <div style={{ display: "flex", flexWrap: "wrap", gap: 14, marginBottom: 12 }}>
        <Spec label="WEIGHT" value={formatPounds(trim.massKg)} unit="lb" />
        {trim.horsepower != null && <Spec label="POWER" value={Math.round(trim.horsepower)} unit="hp" />}
        {trim.torqueNm != null && <Spec label="TORQUE" value={formatPoundFeet(trim.torqueNm)} unit="lb-ft" />}
        {trim.drivetrain && <Spec label="DRIVETRAIN" value={trim.drivetrain} />}
        {trim.transmission && <Spec label="GEARBOX" value={trim.transmission} />}
      </div>

      {(engine || transmission) && (
        <div style={{ display: "flex", flexWrap: "wrap", gap: 14, marginBottom: 12 }}>
          {engine?.displacementLiters != null && (
            <Spec label="DISPLACEMENT" value={engine.displacementLiters.toFixed(1)} unit="L" />
          )}
          {engine?.aspiration && <Spec label="ASPIRATION" value={engine.aspiration} />}
          {engine?.redlineRpm != null && <Spec label="REDLINE" value={Math.round(engine.redlineRpm)} unit="rpm" />}
          {transmission?.numberOfGears != null && (
            <Spec label="GEARS" value={transmission.numberOfGears} />
          )}
        </div>
      )}

      {detail.tireDescription && (
        <div style={{ fontFamily: F.mono, fontSize: 11, color: C.dim, marginBottom: 8 }}>
          {detail.tireDescription}
        </div>
      )}

      {/* Manufacturer/press figures, kept visibly separate from simulated ones. */}
      {published && (published.zeroToSixtySeconds != null || published.quarterMileSeconds != null) && (
        <div style={{ fontSize: 11, color: C.dim, lineHeight: 1.6 }}>
          <span style={{ fontFamily: F.mono }}>PUBLISHED</span>{" "}
          {published.zeroToSixtySeconds != null && <>0–60 in {published.zeroToSixtySeconds.toFixed(1)}s</>}
          {published.zeroToSixtySeconds != null && published.quarterMileSeconds != null && " · "}
          {published.quarterMileSeconds != null && <>1/4 in {published.quarterMileSeconds.toFixed(1)}s</>}
          {published.source && <> · {published.source}</>}
        </div>
      )}
    </div>
  );
}

export default function VehiclePicker({
  label, color, selection, disabled, onSaveToGarage, garage, onLoadFromGarage, savedFlash,
}) {
  const { years, makes, models, trims, detail } = selection;
  const trim = detail.status === "ready" && detail.data ? detail.data.identity : null;
  /* A trim set without walking the cascade — from the popular list or the garage. */
  const pickedDirectly = selection.trimId !== null && selection.modelId === null;

  const title = trim
    ? [trim.year, trim.make, trim.model].filter(Boolean).join(" ")
    : label;

  return (
    <div style={{
      background: C.panel, border: `1px solid ${C.line}`, borderRadius: RADIUS,
      padding: 20, boxShadow: "0 1px 3px #12384a14",
    }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 14, gap: 8 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 8, minWidth: 0 }}>
          <span style={{ width: 4, height: 20, borderRadius: RADIUS, background: color, flexShrink: 0 }} />
          <span style={{
            fontFamily: F.display, fontSize: 20, fontWeight: 700, color: C.text,
            overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap",
          }}>
            {title}
          </span>
        </div>
        {trim?.horsepower != null && (
          <div style={{ fontFamily: F.mono, fontSize: 12, color: C.dim, flexShrink: 0 }}>
            {Math.round(horsepowerPerTon(trim.horsepower, trim.massKg))} hp/ton
          </div>
        )}
      </div>

      {trim && (
        <div style={{ fontFamily: F.mono, fontSize: 11, color: C.dim, marginTop: -8, marginBottom: 12 }}>
          {[trim.trim, trim.generation].filter(Boolean).join(" · ")}
        </div>
      )}

      {/* Loading a popular or saved car sets the trim directly. The catalog
        * cannot map a trim id back to its make and model ids, so the selects
        * below stay empty until the viewer starts a fresh cascade. Saying so
        * is better than showing three blank dropdowns under a named car. */}
      {pickedDirectly && (
        <div style={{
          background: C.panelAlt, border: `1px solid ${C.line}`, borderRadius: RADIUS,
          padding: "8px 10px", fontSize: 12, color: C.dim, marginBottom: 12,
          display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8,
        }}>
          <span>Chosen from the catalog list.</span>
          <button
            type="button"
            onClick={selection.clear}
            disabled={disabled}
            style={{
              background: "transparent", border: `1px solid ${C.line}`, borderRadius: RADIUS,
              color: C.text, fontSize: 11, padding: "3px 8px",
              cursor: disabled ? "default" : "pointer",
            }}
          >
            Browse instead
          </button>
        </div>
      )}

      {years.status === "error" && (
        <div style={{ marginBottom: 12 }}>
          <ErrorBlock message={years.error} offline onRetry={selection.reloadYears} />
        </div>
      )}

      <CascadeSelect
        label="Year"
        placeholder="Select a year…"
        list={years}
        value={selection.year}
        onChange={selection.setYear}
        disabled={disabled}
        optionOf={(y) => ({ key: y, text: String(y) })}
      />

      <CascadeSelect
        label="Make"
        placeholder="Select a make…"
        waitingLabel="Pick a year first"
        list={makes}
        value={selection.makeId}
        onChange={selection.setMakeId}
        disabled={disabled || selection.year === null}
        optionOf={(m) => ({ key: m.id, text: m.name })}
      />

      <CascadeSelect
        label="Model"
        placeholder="Select a model…"
        waitingLabel="Pick a make first"
        list={models}
        value={selection.modelId}
        onChange={selection.setModelId}
        disabled={disabled || selection.makeId === null}
        optionOf={(m) => ({
          key: m.id,
          text: m.generation ? `${m.name} (${m.generation})` : m.name,
        })}
      />

      <CascadeSelect
        label="Trim"
        placeholder="Select a trim…"
        waitingLabel="Pick a model first"
        list={trims}
        value={selection.trimId}
        onChange={selection.setTrimId}
        disabled={disabled || selection.modelId === null}
        optionOf={(t) => ({ key: t.id, text: t.trim || `Trim ${t.id}` })}
      />

      {/* Empty states matter here: a year can genuinely have no makes seeded. */}
      {makes.status === "empty" && <EmptyBlock label="No makes for that year." />}
      {models.status === "empty" && <EmptyBlock label="No models for that make and year." />}
      {trims.status === "empty" && <EmptyBlock label="No trims for that model and year." />}

      {detail.status === "loading" && <LoadingBlock label="Loading vehicle details…" />}
      {detail.status === "error" && <ErrorBlock message={detail.error} />}
      {detail.status === "ready" && detail.data && <VehicleDetails detail={detail.data} />}

      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: 14, gap: 8 }}>
        <button
          type="button"
          onClick={() => trim && onSaveToGarage(trim)}
          disabled={disabled || !trim}
          style={{
            display: "flex", alignItems: "center", gap: 6, background: "transparent",
            border: `1px solid ${savedFlash ? C.cyan : C.line}`, borderRadius: RADIUS,
            color: savedFlash ? C.cyan : C.dim, fontSize: 12, padding: "6px 10px",
            cursor: disabled || !trim ? "default" : "pointer", opacity: trim ? 1 : 0.5,
          }}
        >
          {savedFlash ? <Check size={13} aria-hidden="true" /> : <Save size={13} aria-hidden="true" />}
          {savedFlash ? "Saved to garage" : "Save to garage"}
        </button>

        {garage.length > 0 && (
          <select
            aria-label="Load a saved vehicle into this lane"
            style={{ ...inputStyle, width: "auto", padding: "6px 8px", fontSize: 12 }}
            disabled={disabled}
            value=""
            onChange={(e) => {
              const item = garage.find((g) => String(g.trimId) === e.target.value);
              if (item) onLoadFromGarage(item);
            }}
          >
            <option value="">{"Load from garage…"}</option>
            {garage.map((g) => (
              <option key={g.id} value={String(g.trimId)}>{g.name}</option>
            ))}
          </select>
        )}
      </div>
    </div>
  );
}

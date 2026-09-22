/*
 * The small panels every data-backed section shares: loading, empty, error.
 *
 * Having one copy of each keeps the wording and the spacing consistent across
 * the picker, the popular list and the race panel, and keeps the status
 * announcements in one place rather than scattered through the tree.
 */
import React from "react";
import { AlertTriangle, CloudOff, Inbox, Loader } from "lucide-react";

import { C, F, RADIUS } from "../theme.js";

const shell = {
  background: C.panelAlt,
  border: `1px solid ${C.line}`,
  borderRadius: RADIUS,
  padding: "12px 14px",
  fontSize: 13,
  color: C.dim,
  display: "flex",
  alignItems: "center",
  gap: 8,
};

/** Announced politely so a screen reader hears the section settle. */
export function LoadingBlock({ label = "Loading…" }) {
  return (
    <div style={shell} role="status" aria-live="polite">
      <Loader size={14} aria-hidden="true" />
      <span>{label}</span>
    </div>
  );
}

export function EmptyBlock({ label }) {
  return (
    <div style={{ ...shell, borderStyle: "dashed" }} role="status" aria-live="polite">
      <Inbox size={14} aria-hidden="true" />
      <span>{label}</span>
    </div>
  );
}

/**
 * A failure the viewer may be able to act on. `correlationId` is shown when
 * the backend supplied one, because it is the thing worth quoting in a bug
 * report; network failures have none.
 */
export function ErrorBlock({ message, correlationId, onRetry, offline }) {
  return (
    <div
      style={{ ...shell, alignItems: "flex-start", borderColor: C.orange, flexDirection: "column", gap: 6 }}
      role="alert"
    >
      <div style={{ display: "flex", alignItems: "center", gap: 8, color: C.orange, fontFamily: F.mono, fontSize: 12 }}>
        {offline ? <CloudOff size={14} aria-hidden="true" /> : <AlertTriangle size={14} aria-hidden="true" />}
        {offline ? "BACKEND UNAVAILABLE" : "REQUEST FAILED"}
      </div>
      <div style={{ color: C.text, fontSize: 13, lineHeight: 1.5 }}>{message}</div>
      {correlationId && (
        <div style={{ fontFamily: F.mono, fontSize: 11, color: C.dim }}>
          correlation id: {correlationId}
        </div>
      )}
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          style={{
            background: "transparent",
            border: `1px solid ${C.line}`,
            borderRadius: RADIUS,
            color: C.text,
            fontSize: 12,
            padding: "5px 10px",
            cursor: "pointer",
            marginTop: 2,
          }}
        >
          Try again
        </button>
      )}
    </div>
  );
}

/** Visually hidden live region for status the layout does not already show. */
export function Announcer({ message }) {
  return (
    <div
      aria-live="polite"
      style={{
        position: "absolute", width: 1, height: 1, padding: 0, margin: -1,
        overflow: "hidden", clip: "rect(0 0 0 0)", whiteSpace: "nowrap", border: 0,
      }}
    >
      {message}
    </div>
  );
}

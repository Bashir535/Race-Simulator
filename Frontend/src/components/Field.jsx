import React from "react";

import { C, F } from "../theme.js";

/* A labelled form row. The label wraps its control, so the association holds
 * without needing matching ids. */
export default function Field({ label, children, hint }) {
  return (
    <label style={{ display: "block", marginBottom: 12 }}>
      <div style={{ fontSize: 12, color: C.dim, marginBottom: 4, fontFamily: F.mono }}>{label}</div>
      {children}
      {hint && (
        <div style={{ fontSize: 11, color: C.dim, marginTop: 4 }}>{hint}</div>
      )}
    </label>
  );
}

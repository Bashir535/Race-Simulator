export const C = {
  /* Surfaces */
  bg: "#CBE7F3",
  bgDeep: "#AEDAEB",
  panel: "#FFFFFF",
  panelAlt: "#EDF6FA",
  line: "#BBD9E6",
  lineSoft: "#D9EAF2",

  /* Type */
  text: "#123A4D",
  dim: "#4A6877",
  onAccent: "#FFFFFF",

  /* Racing accents — orange is lane A, cyan is lane B. */
  orange: "#D64033",
  cyan: "#0C8FAE",
  amber: "#F2A118",
  green: "#2BA24C",

  /* Bulb colors above stay bright so the tree glows; these darker twins are for
     text and icons, where the bright versions fail contrast on light surfaces. */
  amberInk: "#895B0D",
  greenInk: "#1E7436",
  red: "#D42A3D",

  /* Track scene */
  skyTop: "#6FC2E8",
  skyBottom: "#D6EEF8",
  road: "#717E87",
  roadDark: "#5F6C75",
  roadLine: "#EDF2F4",
  grass: "#84C36B",
};

/*
 * Typography. Times carries the display voice — the wordmark, panel titles,
 * build names and result numbers — so the app reads like a printed timing
 * sheet. The mono face stays on telemetry labels and Inter on form controls,
 * which keeps the split predictable: serif names things, mono measures things.
 */
export const F = {
  display: "'Times New Roman', Times, Georgia, serif",
  mono: "'IBM Plex Mono', ui-monospace, SFMono-Regular, Menlo, monospace",
  body: "'Inter', system-ui, -apple-system, 'Segoe UI', sans-serif",
};

/* Square corners throughout — panels, inputs and buttons share this. */
export const RADIUS = 0;

export const inputStyle = {
  width: "100%",
  background: C.panelAlt,
  border: `1px solid ${C.line}`,
  borderRadius: RADIUS,
  color: C.text,
  padding: "8px 10px",
  fontSize: 14,
  fontFamily: F.body,
  outline: "none",
};

/*
 * Guards the two UI rules that are easy to lose to a stale editor buffer or a
 * hand-edit: every rectangle stays square, and no component hard-codes a font
 * family or a corner radius instead of using the theme tokens.
 *
 *   node Frontend/check-ui.mjs
 */
import { readdirSync, readFileSync, statSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = fileURLToPath(new URL("./src/", import.meta.url));

function walk(dir) {
  return readdirSync(dir).flatMap((n) => {
    const p = join(dir, n);
    return statSync(p).isDirectory()
      ? walk(p)
      : /\.(jsx?|tsx?)$/.test(p) ? [p] : [];
  });
}

const problems = [];
for (const file of walk(ROOT)) {
  const rel = file.slice(ROOT.length);
  if (rel === "theme.js") continue;
  readFileSync(file, "utf8").split("\n").forEach((line, i) => {
    const at = `${rel}:${i + 1}`;
    // A numeric radius means a rounded corner slipped back in. "50%" and 999
    // are the deliberate circles: tree bulbs, the sun, the lane dot, the car token.
    const m = line.match(/borderRadius: (\d+)\b/);
    if (m && m[1] !== "999") problems.push(`${at}  rounded corner: borderRadius: ${m[1]} (use RADIUS)`);
    if (/border-radius:\s*(?!0|50%|999px)\S/.test(line)) problems.push(`${at}  rounded corner in CSS: ${line.trim()}`);
    if (/fontFamily: "/.test(line)) problems.push(`${at}  hard-coded font (use F.display / F.mono / F.body)`);
    if (/Big Shoulders/.test(line)) problems.push(`${at}  stale display webfont`);
    if (/fontWeight: 800/.test(line)) problems.push(`${at}  Times has no 800 weight — use 700`);
  });
}

if (problems.length) {
  console.error(`UI check failed (${problems.length}):`);
  problems.forEach((p) => console.error("  " + p));
  process.exit(1);
}
console.log("UI check passed: corners square, fonts come from theme tokens.");

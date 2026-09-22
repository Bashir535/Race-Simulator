/*
 * Frontend test runner.
 *
 *   npm test
 *
 * The suites exercise the two pieces of the integration that are pure logic
 * and so can be checked without a backend: how the API client talks to a
 * server (URLs, error envelope, correlation ids, de-duplication, cancellation)
 * and how the playback model reads a race timeline.
 *
 * The stub server in client.test.mjs returns DTO-shaped payloads only. Nothing
 * here verifies race physics or catalog contents — that needs the real backend,
 * per the manual steps in Frontend/README.md.
 *
 * Sources are TypeScript, so each is bundled with esbuild before it runs.
 */
import { execFileSync } from "node:child_process";
import { mkdtempSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { fileURLToPath, URL } from "node:url";

const here = (p) => fileURLToPath(new URL(p, import.meta.url));
const root = here("../../");
const esbuild = join(root, "node_modules/.bin/esbuild");
const out = mkdtempSync(join(tmpdir(), "revmatch-test-"));

const STUB_BASE_URL = "http://localhost:18081/api/v1";

const bundles = [
  {
    entry: here("../src/lib/api/client.ts"),
    outfile: join(out, "client.bundle.mjs"),
    define: `--define:import.meta.env={"VITE_API_BASE_URL":"${STUB_BASE_URL}"}`,
  },
  { entry: here("../src/lib/raceModel.ts"), outfile: join(out, "model.bundle.mjs") },
];

for (const b of bundles) {
  const args = [b.entry, "--bundle", "--format=esm", "--platform=node", `--outfile=${b.outfile}`, "--log-level=error"];
  if (b.define) args.push(b.define);
  execFileSync(esbuild, args, { stdio: "inherit" });
}

let failed = 0;
for (const suite of ["client.test.mjs", "model.test.mjs"]) {
  console.log(`\n── ${suite} ──`);
  try {
    execFileSync(process.execPath, [here(`./${suite}`)], { stdio: "inherit", env: { ...process.env, BUNDLE_DIR: out } });
  } catch {
    failed += 1;
  }
}

if (failed) {
  console.error(`\n${failed} suite(s) failed.`);
  process.exit(1);
}
console.log("\nAll frontend suites passed.");

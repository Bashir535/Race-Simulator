import { defineConfig } from "@playwright/test";

// Run against a disposable backend/database, never a shared production server.
export default defineConfig({
  testDir: "./e2e",
  timeout: 60000,
  workers: 1,
  use: {
    baseURL: "http://localhost:5175",
    channel: "chrome",
    viewport: { width: 1440, height: 1000 },
    screenshot: "only-on-failure",
    trace: "retain-on-failure",
  },
  webServer: {
    command: "npm run dev -- --port 5175 --strictPort",
    url: "http://localhost:5175",
    cwd: "..",
    reuseExistingServer: false,
    env: { VITE_API_BASE_URL: process.env.E2E_API_URL || "http://localhost:18082/api/v1" },
  },
});

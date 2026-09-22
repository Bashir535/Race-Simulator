import { fileURLToPath, URL } from "node:url";
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

const here = (p) => fileURLToPath(new URL(p, import.meta.url));

/*
 * The API base URL comes from VITE_API_BASE_URL (see .env.example), so the
 * client calls the backend directly. No dev proxy is needed: the backend's
 * WebConfiguration allows this origin, defaulting to http://localhost:5173,
 * which is why the dev server port is pinned below.
 */
export default defineConfig({
  root: here("src"),
  envDir: here("."),
  plugins: [react()],
  server: { port: 5173, strictPort: true },
  build: { outDir: here("dist"), emptyOutDir: true },
});

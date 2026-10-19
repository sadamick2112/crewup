import path from "node:path";
import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { loadEnv } from "vite";
import { defineConfig } from "vitest/config";

// The repo root .env is shared with Docker Compose and the backend (plan §4).
const repoRoot = path.resolve(import.meta.dirname, "..");

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // Reads only this variable (not the whole .env) and never exposes it to browser code.
  const { CREWUP_BACKEND_PORT: backendPort = "8080" } = loadEnv(
    mode,
    repoRoot,
    "CREWUP_BACKEND_PORT",
  );

  return {
    plugins: [react(), tailwindcss()],
    resolve: {
      // "@/..." imports, defined once in tsconfig.app.json.
      tsconfigPaths: true,
    },
    server: {
      // Keycloak only accepts sign-in redirects to :5173, so fail instead of moving to another port.
      port: 5173,
      strictPort: true,
      // The browser sees one origin, so the backend needs no CORS (plan §5).
      proxy: {
        "/api": `http://localhost:${backendPort}`,
      },
    },
    test: {
      environment: "jsdom",
      setupFiles: ["./src/test/setup.ts"],
      coverage: {
        provider: "v8",
        include: ["src/**"],
        exclude: [
          // Tests and test helpers are not application code.
          "src/**/*.test.{ts,tsx}",
          "src/test/**",
          // shadcn/ui components are copied in from the registry, not written here (M0-24).
          "src/components/ui/**",
          // Bootstrap entry point: mounts the app into index.html (M0-24).
          "src/main.tsx",
        ],
        thresholds: {
          lines: 80,
          branches: 70,
        },
        reporter: ["text-summary", "html", "json-summary"],
      },
    },
  };
});

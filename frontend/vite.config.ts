import path from "node:path";
import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { loadEnv } from "vite";
import { defineConfig } from "vitest/config";

// The repo root .env is shared with Docker Compose and the backend (plan §4).
const repoRoot = path.resolve(import.meta.dirname, "..");

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // Reads only these variables, not the whole .env. Only the issuer reaches browser code (it's public).
  const {
    CREWUP_BACKEND_PORT: backendPort = "8080",
    CREWUP_OIDC_ISSUER_URI:
      oidcAuthority = "http://localhost:8180/realms/crewup",
  } = loadEnv(mode, repoRoot, [
    "CREWUP_BACKEND_PORT",
    "CREWUP_OIDC_ISSUER_URI",
  ]);

  return {
    plugins: [react(), tailwindcss()],
    define: {
      // The browser signs in with the same realm whose tokens the backend accepts.
      "import.meta.env.VITE_OIDC_AUTHORITY": JSON.stringify(oidcAuthority),
    },
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
          // Generated from api/openapi.json by `npm run gen:api` (M0-24).
          "src/shared/api/schema.d.ts",
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

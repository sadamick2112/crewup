import { defineConfig, devices } from "@playwright/test";

// End-to-end tests run locally against the real stack (spec decision 3; they join CI in M8). Playwright starts
// Vite; the backend and Docker Compose (Postgres, Keycloak) must already be running, which global-setup checks.
export default defineConfig({
  testDir: "./e2e",
  globalSetup: "./e2e/global-setup.ts",
  forbidOnly: true,
  reporter: [["list"], ["html", { open: "never" }]],
  use: {
    // Keycloak only accepts sign-in redirects to :5173 (plan §4).
    baseURL: "http://localhost:5173",
    trace: "retain-on-failure",
  },
  projects: [
    {
      name: "chromium",
      use: {
        ...devices["Desktop Chrome"],
        viewport: { width: 1280, height: 720 },
      },
    },
  ],
  webServer: {
    command: "npm run dev",
    url: "http://localhost:5173",
    // A dev server that's already running (e.g. `npm run dev` in another terminal) is reused.
    reuseExistingServer: true,
  },
});

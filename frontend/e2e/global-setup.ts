import path from "node:path";
import { loadEnv } from "vite";

// Same variables and defaults as vite.config.ts, from the repo root .env (plan §4).
const repoRoot = path.resolve(import.meta.dirname, "../..");
const {
  CREWUP_BACKEND_PORT: backendPort = "8080",
  CREWUP_OIDC_ISSUER_URI: issuer = "http://localhost:8180/realms/crewup",
} = loadEnv("development", repoRoot, [
  "CREWUP_BACKEND_PORT",
  "CREWUP_OIDC_ISSUER_URI",
]);

/** Fails fast with a readable message when the stack isn't running, instead of a timeout in the middle of a test. */
export default async function globalSetup(): Promise<void> {
  await expectReachable(
    `${issuer}/.well-known/openid-configuration`,
    "Keycloak: run `docker compose up -d` in the repo root",
  );
  await expectReachable(
    `http://localhost:${backendPort}/actuator/health`,
    "the backend: run `./mvnw spring-boot:run` in backend/",
  );
}

async function expectReachable(url: string, howToStart: string): Promise<void> {
  const ok = await fetch(url).then(
    (response) => response.ok,
    () => false,
  );
  if (!ok) {
    throw new Error(`${url} is not reachable. Start ${howToStart}.`);
  }
}

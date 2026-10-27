// Adds CrewUp's variable to Vite's import.meta.env type (vite/client, listed in tsconfig.app.json).
interface ImportMetaEnv {
  /** The Keycloak realm URL, from CREWUP_OIDC_ISSUER_URI in the repo root .env (set in vite.config.ts). */
  readonly VITE_OIDC_AUTHORITY: string;
}

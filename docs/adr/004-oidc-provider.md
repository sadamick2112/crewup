# ADR-004: OIDC provider and browser sign-in

**Status:** Accepted · October 23, 2026 (the hosted provider for production is chosen in M8) · Context: M0 spec §6 decisions 1, 2 and 6, plan §4–§6

## Context

CrewUp needs sign-up and sign-in with email and password now (ACC-1), and social login before launch. The web app is a single-page app (SPA); a React Native app follows later and calls the same API. The backend should depend on the OpenID Connect (OIDC) standard, not on one vendor, so the identity provider can change at launch without code changes.

Three questions follow: which provider to run during development, how the SPA gets and keeps tokens, and whether the API needs CSRF protection.

## Decision

### 1. Keycloak for development, a hosted provider at launch

Development uses **Keycloak** in Docker Compose (`start-dev --import-realm`). The realm `crewup` is committed as `infra/keycloak/crewup-realm.json` and imported on every start: self-registration on, the public client `crewup-web`, an audience mapper that adds `crewup-api` to access tokens, and the dev user `dev@crewup.local`. Every dev-only value is marked "DEV ONLY" in the realm and the README.

The backend is a plain OAuth2 resource server: it validates the token's signature (keys from the issuer's JWKS), issuer, expiry, and the `crewup-api` audience. The issuer comes from `CREWUP_OIDC_ISSUER_URI`, so switching providers is a configuration change.

**Social login is deferred to M8.** Keycloak could broker Google or GitHub today, but M8 may replace Keycloak with a hosted provider, and social login should be set up once, there. ACC-1 stays partial until then.

### 2. SPA with Authorization Code + PKCE, tokens in memory only

- `crewup-web` is a **public client** (no secret, since a browser can't keep one) using **Authorization Code with PKCE** (`S256` required by the realm). The app never sees a password; sign-in, registration and sign-out happen on the provider's hosted pages.
- Tokens live **in memory only** (`oidc-client-ts` with an in-memory user store), never in `localStorage` or `sessionStorage`, where any injected script could read them later.
- **Staying signed in across reloads:** a reload loses the in-memory tokens. At startup the app tries a **silent sign-in** (`prompt=none` in a hidden iframe), which uses the provider's own session cookie to issue fresh tokens without a visible redirect. With no session, the provider answers `login_required` and the app shows "Sign in". After startup, renewals use the refresh token.
- The API is **stateless**: no server session, every request carries `Authorization: Bearer <access token>`.

A **backend-for-frontend** (BFF: the server holds the tokens and gives the browser a session cookie) was considered. It's stronger against XSS, because no token ever reaches the browser. It was rejected for now: it adds server sessions, CSRF handling, and a second authentication path next to the bearer tokens the mobile app needs. XSS is reduced instead by in-memory tokens, React's escaping, and the Semgrep rule against `dangerouslySetInnerHTML` (TS-10).

### 3. CSRF protection is off for the stateless API

CSRF (cross-site request forgery) works by getting the victim's browser to send a request that **automatically** carries their credentials, which in practice means cookies. CrewUp's API accepts only a bearer token in the `Authorization` header. A browser never adds that header on its own, and another site's page can't read the token from CrewUp's memory, so a forged request arrives without credentials and gets `401`.

So `SecurityConfig` disables Spring Security's CSRF protection with a comment pointing here. Scanners flag `csrf.disable()`; those findings are suppressed or dismissed with this reason, not "fixed" (M0-19):

- Semgrep: a `nosemgrep` comment with the reason, on the line above (today the registry rule only matches the older `.csrf().disable()` form, so it suppresses nothing yet).
- CodeQL: alert `java/spring-disabled-csrf-protection` dismissed as "won't fix", citing this ADR (CodeQL has no inline suppression).

**If CrewUp ever uses cookie-based sessions** (for example a BFF), CSRF protection must come back on.

### 4. Production requirement: provider on the same site (M8)

Silent sign-in needs the browser to send the provider's session cookie inside the hidden iframe. Browsers increasingly block such **third-party cookies**, so it works reliably only when the app and the provider are on the **same site** (same registrable domain). In development, `localhost:5173` and `localhost:8180` are same-site.

**In M8, the hosted provider must be served from the app's own site**, for example `auth.<domain>` next to `app.<domain>` (a custom domain on the provider). Otherwise, a reload falls back to a quick full-page redirect to the provider and back: still signed in, but with a visible flash.

## Consequences

- **Good:** the backend depends only on OIDC; changing provider is a configuration change. Passwords never touch CrewUp. The mobile app can use the same flow (Authorization Code + PKCE) and the same API. No server sessions to store or scale.
- **Bad:** an XSS bug could still use tokens while the page is open (a BFF would prevent that). Silent sign-in ties production hosting to a same-site provider domain. Keycloak's login pages use its default theme until M8.
- **Follow-ups (M8):** choose the hosted provider, add social login, host it on the app's site, and decide CORS for production.

# M0 — Foundation: Spec

**Status:** Approved · October 14, 2026 (amended the same day: test quality M0-24…M0-25, logging and tracing M0-26…M0-28) · Milestone M0 in `roadmap.md`
**Requirements:** ACC-1 (partial, see §5) · NFR-2 · NFR-3 · NFR-4 · NFR-8 · NFR-9

## 1. Goal

Stand up the walking skeleton for every later milestone: a backend and a frontend that build, test, and run locally with one command, with CI and a typed API contract between them. The demo shows the whole path working: **a person signs in through the OIDC provider, and the web app greets them with data from an authenticated API call.**

M0 has almost no product features. Its value is that M1 can start with "add a table, an endpoint, and a page" instead of "set up the project."

## 2. User stories

- **US-1** As a developer, I clone the repo, run `docker compose up -d`, start the backend and frontend, and have a working app locally, without hand-configuring the identity provider or database.
- **US-2** As a visitor, I can sign up with email and password, sign in, and sign out.
- **US-3** As a signed-in user, I see a home page that greets me by name, using data the backend returned for my account.
- **US-4** As a developer, every push and pull request runs CI, which tells me whether the build, tests, lint, and module-boundary checks pass.
- **US-5** As a frontend developer, I call the backend through a TypeScript client generated from the backend's OpenAPI spec, not hand-written fetch calls.
- **US-6** As a developer, CI scans every change for common security flaws (injection, XSS, insecure configuration), committed secrets, and vulnerable dependencies, so security problems are caught before merge.
- **US-7** As a developer, code follows one written set of coding standards: formatting and mechanical rules are fixed and checked by tools, and the rest is reviewed against the standards before every pull request.
- **US-8** As a developer, CI tells me when tests leave too much code unexercised, or when a page has accessibility problems, so test quality and accessibility don't slip unnoticed.
- **US-9** As a developer, I can follow one user action from the browser through every backend log line it produced, using a single trace ID, and a user who hits an error can give me that ID.

## 3. Acceptance criteria

Test names reference the criterion ID (for example, `M0_4_unauthenticatedApiCallReturns401`) plus the requirement ID where one applies.

### Local environment (US-1, NFR-9)
- **M0-1** `docker compose up -d` starts PostgreSQL and Keycloak. Keycloak imports a committed `crewup` realm on startup: the web client is registered, self-registration is on, and there is at least one ready-to-use development user.
- **M0-2** The backend starts against the Compose services with no manual setup beyond copying an example env/config file. Flyway runs on startup (an empty baseline migration is enough).
- **M0-3** The backend exposes a health endpoint that reports the status of the application and its database, reachable without signing in.

### Authentication (US-2, US-3, ACC-1)
- **M0-4** Every `/api/**` endpoint, except explicitly public ones, rejects requests without a valid access token with `401` and an RFC 9457 Problem Details body.
- **M0-5** A visitor can register with email and password, and sign in and sign out through the provider's hosted login page. The app never handles passwords. *(ACC-1, email/password part)*
- **M0-6** `GET /api/me` returns the caller's user profile (id, display name, email). The first authenticated call creates a User record linked to the token's subject; later calls return the same record. *(ACC-1)*
- **M0-7** Signed in, the home page shows "Hello, {display name}" using data from `GET /api/me`. Signed out, it shows a sign-in button. Reloading the page keeps the user signed in until the session ends.

### Backend structure (NFR-8)
- **M0-8** The backend is a Spring Modulith application under `app.crewup`, with at least the `identity` module (owning User) and a `shared` package. A Modulith `verify()` test passes and runs in CI.
- **M0-9** The backend publishes an OpenAPI document. `npm run gen:api` regenerates the frontend client from it, and the frontend uses that client for `GET /api/me`.

### Frontend (NFR-3)
- **M0-10** A React + TypeScript + Vite app with routing, server state through TanStack Query, and Tailwind + shadcn/ui is set up, with at least one shadcn component in use.
- **M0-11** The home page is usable at a 360 px width.

### CI (US-4, NFR-8)
- **M0-12** A GitHub Actions workflow runs on pushes and pull requests to `main`: backend build and tests (including Testcontainers integration tests and Modulith verification), and frontend lint, type check, tests, and production build. A failure in any step fails the workflow.
- **M0-13** There is at least one automated test per layer: backend unit, backend integration (Testcontainers), frontend component (RTL + MSW), and one end-to-end sign-in test (Playwright) that runs locally.

### Security hygiene (US-6, NFR-2)
- **M0-14** No secrets are committed. Local settings come from gitignored files (`.env`, `application-local.yml`), with committed examples. Development-only Keycloak users and client settings in the realm export are clearly marked dev-only.
- **M0-15** **Static analysis.** CI runs Semgrep (Java, Spring, TypeScript, React, and OWASP Top 10 rulesets, plus the project rules from M0-16) and CodeQL (Java and JavaScript/TypeScript, `security-extended` queries) on every push and pull request to `main`. Results appear in GitHub code scanning. A high-severity or error-level finding fails the check. Semgrep also runs locally with one documented command.
- **M0-16** **Project Semgrep rules** live in `.semgrep/`. At minimum: (a) no SQL or JPQL built by string concatenation or formatting and passed to `@Query`, `createQuery`, `createNativeQuery`, or `JdbcTemplate`; (b) no `dangerouslySetInnerHTML` in the frontend; (c) the Semgrep-enforced rules in `docs/coding-standards.md` (JAVA-3 constructor injection, JAVA-9 no legacy date types, JAVA-12 parameterized logging, JAVA-14 no `UUID.randomUUID()` for entity IDs). Every rule has test fixtures with matching and non-matching examples, and `semgrep --test` runs in CI.
- **M0-17** **Secret scanning.** gitleaks runs in CI on every push and pull request and fails on any finding. The README documents an optional local pre-commit hook.
- **M0-18** **Dependency scanning.** Dependabot security alerts are on, and Dependabot version updates are configured for Maven, npm, GitHub Actions, and Docker Compose images.
- **M0-19** **Suppressions are justified.** Every suppressed or dismissed finding gives a reason next to it (an inline `nosemgrep` comment, or a CodeQL dismissal note). The CSRF exemption points to ADR-004 (see §6, decision 6).

### Coding standards (US-7)
- **M0-20** **Java formatting.** Spotless with Palantir Java Format is configured in the Maven build. `./mvnw spotless:apply` formats the code, and `spotless:check` runs during `./mvnw verify`, so unformatted code fails CI.
- **M0-21** **Frontend linting and formatting.** ESLint (typescript-eslint, react-hooks, jsx-a11y, and testing-library rules) and Prettier are configured, with `eslint-config-prettier` so the two never conflict. `npm run lint`, `npm run format` (write), `npm run format:check` and `npm run typecheck` (`tsc --noEmit`, `strict` on) exist, and CI runs `lint`, `format:check` and `typecheck`.
- **M0-22** **Shared editor settings.** An `.editorconfig` sets line endings (LF), final newlines, charset and indentation per file type, matching the formatters.
- **M0-23** **Standards in the workflow.** `docs/coding-standards.md`, the `/review-standards` Claude Code skill, and the pull request template (with the review checklist) are committed. The first M0 pull request is reviewed with `/review-standards`.

### Test quality (US-8, NFR-4)
- **M0-24** **Coverage gate.** Backend coverage is measured with JaCoCo over all backend tests (unit and Testcontainers integration), and frontend coverage with Vitest's V8 coverage over the component tests. Each side fails the build below **80% line and 70% branch** coverage. Exclusions are listed in the build config with a reason each: generated code, copied shadcn/ui components, and bootstrap entry points (`CrewUpApplication.main`, `main.tsx`). CI shows both coverage summaries on the workflow run, and keeps the HTML reports as artifacts.
- **M0-25** **Automated accessibility checks.** Component tests check each page-level component with axe-core against the WCAG 2.2 A and AA rules, and fail on any violation. The Playwright test runs `@axe-core/playwright` on the home page, signed out and signed in, at desktop and 360 px widths, with zero violations allowed. Keycloak's login pages are out of scope until its theme is customized (M8).

### Logging and tracing (US-9, NFR-9)
- **M0-26** **Structured logging.** The backend logs through SLF4J. Locally, logs are human-readable; outside local development they're JSON in Elastic Common Schema (ECS) format (Spring Boot's built-in structured logging, switched on by one setting). Every log line carries the request's `traceId` and `spanId`. Each API request produces exactly one request log line (method, route, status, duration, and the caller's token subject when signed in). Logs never contain tokens, passwords, email addresses, names, or request bodies (GEN-7).
- **M0-27** **Request tracing.** Every request runs in a W3C Trace Context trace. If the request has a valid `traceparent` header, the backend continues that trace; otherwise it starts a new one. Every Problem Details error response (including 401 and 403) includes the `traceId`. No traces are exported in M0; the IDs exist for log correlation.
- **M0-28** **Browser-to-backend correlation.** The web app's API client sends a fresh `traceparent` header on every request, so the backend's logs for that request share the browser's trace ID. When an API call fails, the UI shows a short message with "Reference: {traceId}" and writes the same ID to the browser console.

## 4. Documentation deliverables

- README: what CrewUp is, prerequisites, and how to run it locally (matching US-1).
- `CLAUDE.md` commands section matches the real commands.
- ADR-004 (Keycloak for development) written in `docs/adr/`, including why CSRF protection is off for the stateless API.
- README: a security badge or section listing the scans CI runs.

## 5. Out of scope

- **Social login** (the rest of ACC-1). Deferred to M8, see §6.
- Groups, Members, roles, and any group-scoped endpoint (M1, M2).
- Group switcher (ACC-2, M1).
- Hosting and deployment (M8). PWA installability (later).
- MinIO and photos (P2). AI and Ollama (M7).
- The endpoint authorization guard test (M2) and the OWASP ZAP baseline scan of the running app (M8). See §6, decision 7.
- Profile editing. The display name comes from the identity provider.

## 6. Decisions

Resolved October 13, 2026.

1. **Social login is deferred to M8.** Keycloak could broker Google or GitHub now, but the hosted provider chosen in M8 may replace Keycloak, so social login is set up once, there. ACC-1 stays partial until M8.
2. **SPA + PKCE.** The SPA is a public OIDC client using Authorization Code + PKCE, keeps tokens in memory only, and calls a stateless resource server. A backend-for-frontend was considered (stronger against XSS) and rejected for now: it adds sessions, CSRF handling, and a second auth path for the mobile app. Recorded in ADR-004.
3. **The Playwright end-to-end test runs locally in M0** and joins CI in M8. CI covers everything else (M0-12).
4. **M0 persists the User.** The `identity` module owns a User table, provisioned just in time by `GET /api/me` (M0-6). This proves the Flyway + JPA + Testcontainers path end to end.
5. **Versions:** The plan pins exact versions, confirmed as the current stable releases at scaffold time (Java 25, Spring Boot 4.x, Spring Modulith, Node LTS, Keycloak).
6. **CSRF protection is off for `/api/**`, on purpose.** The API is a stateless resource server that only accepts bearer tokens, which the browser never attaches automatically, so a forged cross-site request has no credentials. Scanners will flag `csrf.disable()`. That finding is suppressed with a reference to ADR-004 instead of being "fixed". If CrewUp ever moves to cookie sessions (for example, a backend-for-frontend), CSRF protection comes back on.
7. **Security scanning is layered across milestones.** Static analysis can't reliably catch broken access control, which is the main risk in a group-scoped app. That risk is covered by the per-role authorization tests (architecture §6) and, from M2 on, a guard test that fails if any `/api/groups/{groupId}/**` handler lacks `@PreAuthorize`. A live scan of the running app (OWASP ZAP baseline) joins in M8, once there is a deployed app to scan. Tool choice: Semgrep for readable custom rules that run locally; CodeQL for data-flow analysis across methods (free for public repos); gitleaks for secrets; Dependabot for dependencies. All of them are CI tools, not application dependencies.
8. **Coding standards: tools for the mechanical rules, review for the rest.** Formatting is automatic (Palantir Java Format through Spotless for Java, chosen for its 4-space, 120-column style familiar to Spring developers; Prettier for the frontend), so it's never debated in review. Rules that need judgment are checked by the `/review-standards` skill before each pull request, as a checklist item in the PR template. It isn't enforced by CI, which avoids API costs. A Claude review in GitHub Actions was considered and can be added later if the local step gets skipped. New build-time dependencies: the Spotless Maven plugin, ESLint plugins (typescript-eslint, react-hooks, jsx-a11y, testing-library), Prettier and `eslint-config-prettier`. None of them ship in the application.
9. **Test quality gates (added after approval, same day).** Coverage limits start at 80% line / 70% branch, are raised as the code settles, and are lowered (or exclusions added) only with the user's approval (CLAUDE.md principle 7). Tests are never written just to raise coverage. Backend coverage includes integration tests, because security, provisioning and migrations are deliberately tested against real Postgres (TEST-4); a unit-only number would push toward mocking the database. Coverage shows that code ran, not that it was checked. Requirement-named tests remain the main proof, and PIT mutation testing is a candidate after M0. Coverage summaries go to the workflow run's summary page instead of a PR comment, which avoids giving third-party actions write access. Accessibility: `jsx-a11y` lints the source, but only axe on rendered output catches problems such as missing names and invalid ARIA. jsdom can't compute colors, so color contrast is checked only by the Playwright run, which is local until M8. New build-time dependencies: `jacoco-maven-plugin`, `@vitest/coverage-v8`, `axe-core`, `@axe-core/playwright`. Commit-message linting (commitlint) was considered and declined; GIT-1 stays review-only.
10. **Logging and tracing (added after approval, same day).** Scope is request tracing and browser-to-backend correlation, viewed as console logs. A log/trace viewer (for example a local Grafana stack) and trace export were considered and deferred; adding an OpenTelemetry exporter later needs no code changes. An **audit trail** of data changes was also considered: it's NFR-7 and starts in M1, when groups and events exist. **Browser errors that don't involve an API call** (such as a rendering crash) aren't sent to the backend in M0. That would need a public error-collection endpoint with abuse protection; the UI shows the reference ID and logs to the console instead. The browser generates `traceparent` with a few lines of its own code rather than the OpenTelemetry JavaScript SDK, which would be a large dependency for one header. A client-supplied `traceparent` is accepted as is: it only affects log correlation, never authorization. New dependency: Micrometer Tracing with the OpenTelemetry bridge (chosen over Boot 4's OpenTelemetry starter in T1, see plan §2); none on the frontend.

## 7. Open questions

None.

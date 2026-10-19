# M0 — Foundation: Tasks

**Status:** Approved · October 14, 2026 · From `plan.md` §11

One task at a time (CLAUDE.md principles 5–7). Check a task off in the commit that completes it. "Done when" is what must be true before the final commit; plan sections are in brackets.

- **Test-first tasks** (marked 🔴→🟢): T4, T5, T6, T7, T10, T11, T14. **Red:** write the listed tests, run them, confirm they fail on assertions (stubs allowed so they compile), and commit. **Green:** implement until they pass, and commit. Never change a red-commit test to make it pass; ask instead (TEST-11).
- **Exempt tasks** (setup, configuration, styling, test-only): T1–T3, T8, T9, T12, T13, T15–T17. Implement, test, and commit once.
- **Coverage at every final commit:** from T3 (backend) and T9 (frontend) on, "Done when" includes the coverage check: `./mvnw verify` and/or `npm run test:coverage`, whichever side the task touched. Gaps show up in the task that created them. If the check fails, follow TEST-12: one pass at testing untested criteria and deleting dead code, then stop and report.

---

### T1 · Pin versions and repo basics [plan §1, §2]
- [x] Confirm the current stable version of every component in plan §2 from official sources, and replace the "Expected line" column with a pinned-versions table in `plan.md`.
- [x] Add `.editorconfig` (M0-22) and `.nvmrc`.
- [x] Check `.gitignore` covers `target/`, `node_modules/`, `dist/`, `.env`, `application-local.yml`, `playwright-report/`, `test-results/`.
- [x] Technology guide: `compose.yaml` → `docker-compose.yml`.

**Done when:** the versions table has exact versions with the date checked. **Commit:** `chore: pin M0 versions and add editor config [M0-22]`

### T2 · Local services [plan §4]
- [x] `docker-compose.yml`: Postgres and Keycloak (`start-dev --import-realm`), ports from `.env`, Postgres health check, named volume.
- [x] `.env.example` with dev-only values and a comment on each.
- [x] `infra/keycloak/crewup-realm.json`: realm `crewup`, client `crewup-web` (public, PKCE S256, redirect and web origin for :5173), `crewup-api` audience mapper, self-registration on, dev user `dev@crewup.local`, "DEV ONLY" descriptions.

**Done when:** `docker compose up -d` brings both services up healthy, the dev user can sign in to the Keycloak account console, and a token from the `crewup-web` client contains `aud: crewup-api` (checked by hand, noted in the commit body). **Commit:** `chore(infra): add Postgres and Keycloak with dev realm [M0-1, M0-14]`

### T3 · Backend scaffold [plan §3, §5]
- [x] Generate the project into `backend/` (Maven, Java 25, `app.crewup`, the plan §3 dependencies), with the Maven Wrapper.
- [x] `application.yml`: imports `../.env`, datasource, Flyway location `db/migration/*`, actuator (health and info only, components shown), `spring.mvc.problemdetails.enabled`.
- [x] Spotless with Palantir Java Format, `check` bound to `verify` (M0-20).
- [x] JaCoCo: `prepare-agent`, `report` and `check` (line ≥ 80%, branch ≥ 70%) in `verify`; exclude `CrewUpApplication` with a reason (M0-24).
- [x] Testcontainers base: a `@ServiceConnection` Postgres configuration shared by integration tests.
- [x] `ModularityTests` with `M0_8_moduleBoundariesAreRespected`.
- [x] Tests: `M0_2_applicationStartsAndMigratesSchema`, `M0_3_healthIsPublicAndReportsDatabase` (health is temporarily open while security is wide open; T5 locks the rest down).

**Done when:** `./mvnw verify` passes with coverage above the limits, a temporarily untested class makes it fail (then remove it), and `./mvnw spring-boot:run` starts against Compose. **Commit:** `feat(backend): scaffold Spring Boot Modulith app [M0-2, M0-3, M0-8, M0-20, M0-24]`

### T4 · UUID v7 IDs 🔴→🟢 [plan §5]
**Red**
- [x] `IdsTest`: version 7 and the RFC variant bits, the embedded timestamp matches the clock, IDs from later milliseconds sort after earlier ones, no duplicates in a large batch. Stub `Ids.newId()` throws.
- [x] Run: all fail on assertions. **Commit:** `test(shared): add failing tests for UUID v7 ids [JAVA-14]`

**Green**
- [x] Implement `app.crewup.shared.Ids.newId()`, and `shared/package-info.java` with `@ApplicationModule(type = OPEN)`.

**Done when:** tests pass without changes. **Commit:** `feat(shared): add UUID v7 id generator [JAVA-14]`

### T5 · Security 🔴→🟢 [plan §5]
**Red**
- [x] Tests: `M0_4_unauthenticatedApiCallReturns401WithProblemDetails`, `M0_4_tokenWithWrongAudienceIsRejected`, `M0_4_validTokenIsAccepted` (against a test-only `/api` probe endpoint), `M0_4_deniedRequestReturns403WithProblemDetails` (an authenticated call outside `/api/**`; API-4), plus health still public.
- [x] Run: the 401/Problem Details and audience tests fail. **Commit:** `test(security): add failing tests for API authentication [M0-4]`

**Green**
- [x] `SecurityConfig`: stateless, JWT resource server (issuer from `.env`), `crewup-api` audience validator, public health and OpenAPI endpoints, `/api/**` authenticated, everything else denied, CSRF off with a `nosemgrep` reason citing ADR-004.
- [x] Problem Details `AuthenticationEntryPoint` and `AccessDeniedHandler`. No `@RestControllerAdvice` yet; it arrives with the first domain exception (GEN-10).

**Done when:** tests pass without changes. **Commit:** `feat(security): secure API as OIDC resource server [M0-4]`

### T6 · Users and `GET /api/me` 🔴→🟢 [plan §5]
**Red**
- [x] Tests: `M0_6_firstCallProvisionsUser`, `M0_6_laterCallsReturnSameUser`, `M0_6_profileRefreshesChangedClaims`, `M0_6_concurrentFirstCallsCreateOneUser`, unit tests for the display-name fallback. Stubs: the `UserProfile` record and a `MeController` / `UserProvisioningService` whose methods throw.
- [x] Run: all fail on assertions (500s, not compile errors). **Commit:** `test(identity): add failing tests for GET /api/me [M0-6]`

**Green**
- [x] Migration `db/migration/identity/V20261014120000__identity_create_users.sql`.
- [x] `User` entity, `UserRepository`, `UserProvisioningService` (create, refresh changed claims, survive concurrent first calls), `MeController`.

**Done when:** tests pass without changes, and a manual `curl` with a real Keycloak token returns the profile (`TOKEN=$(python3 infra/keycloak/get-dev-token.py)`). **Commit:** `feat(identity): provision user and add GET /api/me [M0-6, ACC-1]`

### T7 · Logging and request tracing 🔴→🟢 [plan §5]
**Red**
- [x] Add the tracing dependency (setup, in this commit). Tests, capturing log output with `OutputCaptureExtension`: `M0_26_apiRequestIsLoggedOnceWithRouteStatusAndDuration`, `M0_26_requestLogContainsNoPersonalDataOrToken`, `M0_26_logsAreEcsJsonWhenStructuredFormatIsOn`, `M0_27_incomingTraceparentIsContinued`, `M0_27_newTraceStartsWithoutTraceparent`, `M0_27_problemDetailsIncludeTraceId` (401 and 403). Stubs: an empty `RequestLoggingFilter` and a `ProblemDetailsSupport` that throws.
- [x] Run: all fail on assertions. **Commit:** `test(shared): add failing tests for request logging and tracing [M0-26, M0-27]`

**Green**
- [x] `RequestLoggingFilter` (one INFO line, key-values only, `/api/**` only), `ProblemDetailsSupport` used by the security handlers, and a `ResponseBodyAdvice<ProblemDetail>`.
- [x] `application.yml`: log levels, W3C propagation, sampling 1.0; ECS JSON via `LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs` (documented in `.env.example`).

**Done when:** tests pass without changes, `./mvnw verify` passes with coverage, and a manual `curl -H "Authorization: Bearer $TOKEN" -H 'traceparent: 00-<id>-<span>-01' localhost:8080/api/me` (token from `infra/keycloak/get-dev-token.py`) shows that trace ID in the backend log line. **Commit:** `feat(shared): add request logging and W3C trace correlation [M0-26, M0-27, NFR-9]`

### T8 · OpenAPI contract [plan §5]
- [x] springdoc configured (API title, bearer-auth security scheme). `/api/me` documented with responses 200 and 401.
- [x] Fix: `/swagger-ui.html` (springdoc's redirect to `/swagger-ui/index.html`) returns 401 because `SecurityConfig` doesn't list it as public (found after T6). A bug fix, so test first (TEST-2): a failing test that it redirects without a token, its own commit, then the one-line fix.
- [x] `OpenApiContractTest` with `M0_9_openApiContractIsUpToDate`, and the `-Dopenapi.update=true` mode.
- [x] Generate and commit `api/openapi.json`.

**Done when:** the test passes, and fails after a deliberate change to `UserProfile` (then revert). **Commit:** `feat(api): publish OpenAPI contract with drift test [M0-9]`

### T9 · Frontend scaffold [plan §6, §8]
- [x] Vite `react-ts` in `frontend/`, strict TS, `@/` alias, `engines` from `.nvmrc`, and a `/api` proxy to :8080.
- [x] Backend port overridable in `.env` (plan §4; gap left by T3): `server.port: ${CREWUP_BACKEND_PORT:8080}` in `application.yml`, `CREWUP_BACKEND_PORT` with a comment in `.env.example`, and the Vite proxy target reads the same variable.
- [x] Tailwind 4, then `shadcn init` and `shadcn add button card` (into shadcn's default `src/components/ui/`, decided in T9).
- [x] ESLint flat config (including `no-console` allowing only `warn`/`error`, LOG-7), Prettier (with the Tailwind plugin), and scripts `lint`, `format`, `format:check`, `typecheck`, `test` (M0-21).
- [x] Vitest + jsdom + RTL + MSW setup, with one smoke test rendering the shadcn Button.
- [x] Vitest V8 coverage with thresholds (lines 80, branches 70), exclusions with reasons, and the `test:coverage` script (M0-24).
- [x] `src/test/axe.ts` with `expectNoAxeViolations()`, used in the smoke test (M0-25).

**Done when:** `npm run lint && npm run format:check && npm run typecheck && npm run test:coverage && npm run build` passes, and a deliberately inaccessible element (an unlabeled input) makes the axe helper fail (then remove it). **Commit:** `feat(frontend): scaffold React app with Tailwind, shadcn/ui and tooling [M0-10, M0-21, M0-24, M0-25]`

### T10 · Generated API client 🔴→🟢 [plan §6]
**Red**
- [ ] `gen:api` script and the generated `schema.d.ts` (generation is setup, so it goes in this commit).
- [ ] Tests, with an MSW handler for `GET /api/me`: `useMe` returns the profile; the client sends `Authorization: Bearer <token>`; `useMe` stays idle when signed out; `M0_28_traceparentHasValidFormatAndIsUnique`; `M0_28_apiRequestsCarryTraceparent`; a failed request throws an `ApiError` carrying the `traceId`. Stubs: `client.ts`, `traceparent.ts` and `useMe()` that throw.
- [ ] Run: all fail on assertions. **Commit:** `test(frontend): add failing tests for API client, traceparent and useMe [M0-9, M0-28]`

**Green**
- [ ] `src/shared/api/traceparent.ts`, `src/shared/api/client.ts` (`openapi-fetch` plus bearer-token and `traceparent` middleware, `ApiError`), and `src/core/me/` with `meKeys` and `useMe()`.

**Done when:** tests pass without changes, and `npm run gen:api` leaves no diff. **Commit:** `feat(frontend): add generated API client with trace correlation and useMe query [M0-9, M0-28]`

### T11 · Sign-in and home page 🔴→🟢 [plan §6]
**Red**
- [ ] Tests (auth state supplied by a test wrapper, API by MSW): `M0_7_showsGreetingWhenSignedIn`, `M0_7_showsSignInWhenSignedOut`, `M0_7_signInButtonStartsSignIn`, `M0_7_signOutButtonSignsOut`, `M0_25_homeSignedOutHasNoAxeViolations`, `M0_25_homeSignedInHasNoAxeViolations`; for `restoreSession` with a fake `UserManager`: `M0_7_restoreSessionReturnsUserWhenSsoSessionExists`, `M0_7_restoreSessionTreatsLoginRequiredAsSignedOut`, `M0_7_restoreSessionTreatsOtherErrorsAsSignedOut`; `M0_28_failedRequestShowsReferenceId` (MSW returns 500; the page shows "Reference: {traceId}" and `console.error` gets the same ID). Stubs: an empty `HomePage`, an empty `ErrorMessage`, and a `restoreSession` that throws.
- [ ] Run: all fail on assertions. **Commit:** `test(frontend): add failing tests for home page sign-in states [M0-7, M0-25]`

**Green**
- [ ] `restoreSession()` (the tested decisions). `oidcConfig.ts` as a plain settings object, and `react-oidc-context` wiring: in-memory user store, PKCE code flow, `/auth/callback` route, `restoreSession` at startup, automatic silent renew, sign-out redirect (configuration, exempt from test-first; keep logic out of it, plan §6).
- [ ] Home page: signed out, sign-in Button; signed in, "Hello, {displayName}" Card and sign-out; if `/api/me` fails, `ErrorMessage` with the reference ID. Mobile-first (styling is exempt; behavior is covered by the red tests).

**Done when:** tests pass without changes, and the full flow works by hand with the stack running (sign in, greeting, reload stays signed in, sign out). **Commit:** `feat(frontend): sign in with OIDC + PKCE and greet the user [M0-5, M0-7, M0-11, M0-25, M0-28, ACC-1]`

### T12 · End-to-end test [plan §10]
- [ ] Playwright config (Chromium, Vite as `webServer`, expects backend and Compose running), and the `e2e` script.
- [ ] `e2e/m0-1-5-7-sign-in.spec.ts`: dev user signs in, sees the greeting, reloads and stays signed in, signs out; a 360×740 viewport has no horizontal scroll.
- [ ] `@axe-core/playwright` checks of the home page, signed out and signed in, at 1280 px and 360 px, with zero violations (M0-25).

**Done when:** `npm run e2e` passes locally. **Commit:** `test(e2e): add sign-in journey test with accessibility checks [M0-1, M0-5, M0-7, M0-11, M0-13, M0-25]`

### T13 · CI workflow [plan §9]
- [ ] `.github/workflows/ci.yml` with jobs `backend`, `frontend` (runs `test:coverage`), `api-client`, actions pinned by SHA.
- [ ] Coverage tables in the job summaries, and HTML coverage reports uploaded as artifacts (M0-24).

**Done when:** all three jobs pass on GitHub, and each fails once on a throwaway branch with a deliberate error. **Commit:** `ci: add build, test, lint and coverage workflow [M0-12, M0-24]`

### T14 · Semgrep and gitleaks 🔴→🟢 [plan §7]
**Red**
- [ ] `.semgrep/` fixtures (`// ruleid:` and `// ok:` examples) for `sql-string-concat`, `no-dangerously-set-inner-html`, `no-field-injection`, `no-legacy-date-types`, `slf4j-no-concat`, `no-random-uuid-for-entity-ids`, with rule files that match nothing yet.
- [ ] Run `semgrep --test .semgrep`: every fixture fails. **Commit:** `test(security): add failing Semgrep rule fixtures [M0-16]`

**Green**
- [ ] Write the rule patterns until `semgrep --test` passes without fixture changes.
- [ ] `.github/workflows/security.yml`: a Semgrep scan with SARIF upload and error gate, `semgrep --test`, and gitleaks; `.gitleaks.toml` only if needed, with reasons.
- [ ] Resolve or justify every finding on the existing code (M0-19).

**Done when:** `semgrep --test .semgrep` passes locally (Docker), and the workflow is green on GitHub. **Commit:** `ci(security): add Semgrep rules and gitleaks scanning [M0-15, M0-16, M0-17, M0-19]`

### T15 · CodeQL and Dependabot [plan §7]
- [ ] `.github/workflows/codeql.yml` (java-kotlin, javascript-typescript, `security-extended`).
- [ ] `.github/dependabot.yml` (maven, npm, github-actions, docker-compose; weekly, grouped).
- [ ] Triage CodeQL findings; dismissals carry a reason (M0-19).

**Done when:** CodeQL results show in the Security tab, and Dependabot config is accepted (Insights → Dependency graph → Dependabot). **Commit:** `ci(security): add CodeQL analysis and Dependabot [M0-15, M0-18]`

### T16 · Documentation [spec §4]
- [ ] README: what CrewUp is, prerequisites, run locally (`cp .env.example .env` …), dev user (DEV ONLY), the security scans CI runs, and how to run Semgrep locally.
- [ ] `docs/adr/004-oidc-provider.md`: Keycloak for development, SPA + PKCE with in-memory tokens, CSRF off for the stateless API, and the same-site requirement for silent sign-in in M8.
- [ ] CLAUDE.md commands (npm in `frontend/`, `openapi.update`), `coding-standards.md` DB-1 (timestamp versions) and the local Semgrep command, `requirements.md` statuses (ACC-1 partial).

**Done when:** a fresh clone, following only the README, reaches the greeting. **Commit:** `docs: add README setup, ADR-004 and M0 doc updates [M0-23]`

### T17 · Repository settings and pull request [plan §9]
- [ ] On GitHub: enable Dependabot alerts and security updates; set the code scanning thresholds; protect `main` (require a PR and the checks `backend`, `frontend`, `api-client`, `semgrep`, `gitleaks`, `CodeQL`). *(Done by you in the GitHub UI; I'll give step-by-step instructions.)*
- [ ] Run `/review-standards`, then fix or justify the findings.
- [ ] Open the PR to `main` using the template, and run the plan's Verification steps.

**Done when:** the PR is green and merged, and M0 is marked done in `roadmap.md` / `requirements.md`.

# CrewUp — Project Log

Decisions and context from planning sessions, so any new Claude Code session can pick up where the last one left off. Newest entries go at the top.

---

## 2026-10-29 · M1 start: Surefire delay gone in CI, spec approved, "group" renamed "crew"

**Decided**
1. **M1 spec approved** (`docs/specs/m1-crews/spec.md`), with the decisions in its §7: pictures deferred, real access checks for M1's own rules, no restore and no leave/remove in M1, the app shell and eight screens. 
2. **"Group" is renamed "crew" everywhere** (spec decision 19): screens, API paths (`/api/crews/{crewId}`), the module (`app.crewup.crews`), tables (`crews`, `crew_id`), and the living docs (CLAUDE.md, README, requirements, architecture, roadmap, coding standards, technology guide). Done before any code used the name, so it changed only docs. Requirement IDs keep GRP-*. **Kept as "group":** the "long-running friend groups" tagline, "Friend groups that have met for years" (requirements §1), "Group chat" (a non-goal), "radio group", and shadcn's `input-group`, `radio-group` and `RadioGroup`. **Not changed:** past log entries and the M0 spec, plan and tasks, which record history. The spec folder is now `docs/specs/m1-crews/` and the branch `feat/m1-crews`. The living-docs part merged first as PR #17, on its own because `coding-standards.md` changes go in their own PR.
3. **M1 plan and tasks approved**, including plan §5.6: the M0 home page tests render through the app's routes, get a `GET /api/crews` handler, and open the user menu before "Sign out" (TEST-11, names and assertions unchanged, own `test:` commit before T9).
4. **All eight Dependabot major-version holds stay** (milestone re-check, T17 entry). Last week's npm downloads by major: `typescript` 5: 59%, 6: 20%, 7: 14% (`typescript-eslint` 8.72.1 still allows `<6.1.0`, `openapi-typescript` 7.13.0 needs `^5.x`); `eslint` 9: 53%, 10: 23% (`eslint-plugin-jsx-a11y` 6.10.2 still declares ESLint ≤ 9, issue #1075 open); `@eslint/js` follows ESLint; `@types/node` follows Node 24 (Node 26 became LTS on 2026-10-28; Node 24 is in maintenance until 2028-04-30, so no move yet); `vitest` 4: 47% vs 5: 28%; `msw` 2: 86% vs 3: 8%; `react-router` 7: 45% vs 8: 12%. Re-check at the start of M2.
5. **The README's gitleaks command is fixed in M1's T16**, not in a separate PR.

**Learned**
- **The Surefire 30 s "kill self fork JVM" delay is gone in CI too.** PR #16's `backend` job log (run 40943543169) has no "kill self fork" line and the Maven build took 29 s; the first `main` run after the Boot 4.1.2 merge (40946111321) also has none, 35 s. Before the fix, `main` run 40546676813 (Oct 27) printed the message and took 1:07 min. The one shared test context (entry below) was the fix; no thread hunt needed.
- **The README's branch-only gitleaks command scans nothing** when run through Docker: `--log-opts="origin/main..HEAD"` reported "0 commits scanned" (checked on PR #17's branch). With the two commit IDs (`<base>..<head>`) it scanned the commit. Fix the README command when convenient.

## 2026-10-29 · Fix: one JSON-logging context for all integration tests

**Decided**
1. **`@IntegrationTest` turns on JSON logs (`logging.structured.format.console=ecs`) for every integration test**, instead of `RequestLoggingTest` adding it with `@TestPropertySource`. Changing the committed test was approved (TEST-11); no assertion changed.

**Learned**
- **Why:** Dependabot PR #14 (Spring Boot 4.1.1 → 4.1.2) failed all 6 `RequestLoggingTest` tests: their context logged plain text, so no JSON request line was found. The extra property gave that class its own (second) context. Logback is set up once per JVM, and up to 4.1.1 the test framework's context *pausing* reset it by accident, so the second context set up JSON afresh. Boot 4.1.2 stopped that reset ([spring-boot#52088](https://github.com/spring-projects/spring-boot/issues/52088)), and the second context kept the first one's plain-text setup. Production runs one context and isn't affected. It passed alone and failed only in the full suite.
- **Logging settings belong in `@IntegrationTest`, not on one test class**, and any per-class property creates a second context.
- With one context, Surefire's 30 s "kill self fork JVM" delay (T17 entry, 2026-10-26) no longer appears locally; confirm in CI.

## 2026-10-29 · T17 done: M0 merged, Dependabot tuned (M1 next)

**Decided**
1. **M0 merged** as PR #1 (merge commit `1ca5b58`). The standards wording from the T17 review merged as PR #4 (GIT-1 "where one applies", GIT-2 "in the task's last commit").
2. **Dependabot ignores every major the plan §2 version rule holds back**, not only the ones with a technical blocker: `typescript`, `eslint`, `@eslint/js` (ships with ESLint), `@types/node` (follows Node 24 in `.nvmrc`), `vitest`, `@vitest/*`, `msw` and `react-router` (PR #9 and this PR). This reverses T15's decision 3: those major PRs failed CI and would return with every release. **Re-check the holds by hand, with fresh npm download numbers, at the start of each milestone.**
3. **`vitest` and `@vitest/*` are one Dependabot group**, listed before `minor-and-patch`, because they must share a version.
4. Merged: #3 (`fast-uri` 3.1.9, a security update fixing 4 alerts) and #11 (`lucide-react` 1.60, the first `minor-and-patch` group PR). Closed with the reason on each: #5–#8, #10 and #12 (Vitest 5, React Router 8, `@types/node` 26, MSW 3).

**Learned**
- **Dependabot's first run** on `main` succeeded for all four ecosystems. Each Dependabot PR's CI runs like ours and is held by the same ruleset.
- **Default limit of 5 open version-update PRs per ecosystem.** The first npm run handed over 6 PRs, but only 4 appeared; the rest arrived once others were closed.
- **Closing a Dependabot PR skips that release of each package in it, also inside a group.** Closing #5 (`@vitest/coverage-v8` 5.0.5) made the later `vitest` group PR (#10) leave it out, so the group couldn't pass CI.
- **`@dependabot close` did nothing within two minutes**, even on its own line. Closing the PR through the API worked, and Dependabot answered "I won't notify you again about this release".
- **The yellow warning on `docker-compose.yml` is harmless:** Keycloak's image is on quay.io, where Dependabot can't read publish dates, so the cooldown doesn't apply to it. The `UNAUTHORIZED` line for `library/postgres` in the log is Docker Hub's normal anonymous token step.
- `gh pr edit` fails with a "Projects (classic) is being deprecated" GraphQL error; update or create PRs through `gh api` (REST) instead.

## 2026-10-26 · T17: repository settings, review, verification (merge next)

**Decided**
1. **`main` is protected by one branch ruleset, `protect-main`**, not classic branch protection: GitHub now sets code scanning thresholds only in rulesets ("Require code scanning results"). No bypass; deletion and force pushes blocked; a PR with **0 approvals** (GitHub doesn't let you approve your own PR); required checks `backend`, `frontend`, `api-client`, `semgrep`, `gitleaks`, `CodeQL (java-kotlin)`, `CodeQL (javascript-typescript)`; CodeQL results gated at security "High or higher", alerts "Errors". Semgrep isn't in the code scanning rule, because its job already gates on `ERROR`. Plan §9 updated. **Every change to `main` now goes through a PR**, docs included.
2. **`/review-standards` verdict: Ready for PR** (0 must-fix). Fixed: TEST-10, the `/auth/callback` page had no axe component test (`AuthCallbackPage.test.tsx`, 3 tests, shown to fail with the page's loading check inverted). Accepted as they are, with no history rewrite on the pushed branch: 12 housekeeping commits without IDs (GIT-1) and the follow-up "record and check off" commits of T13–T15 (GIT-2).
3. **Standards PR after the merge:** GIT-1 becomes "with requirement IDs where one applies"; GIT-2 says the checkbox and log entry go in the task's last commit. It's a separate PR from `main` because `coding-standards.md` isn't on `main` until M0 merges (listed in T17).
4. **Merge PR #1 with a merge commit, not squash**, so `main` keeps the red→green commit pairs that TEST-2 and `/review-standards` check.
5. M0 is marked done in `roadmap.md` (M0 row) and `requirements.md` §11 on the branch, since `main` takes changes only through a PR.

**Learned**
- **Required-check names:** `Semgrep OSS` and `CodeQL` (without a suffix) are GitHub's code scanning *result* checks, not our jobs. Picking `Semgrep OSS` by mistake would have left the `semgrep` job (rule tests, error gate) not required; it was swapped for `semgrep` (source GitHub Actions, `integration_id` 15368).
- The ruleset's `pull_request` rule shows `require_extra_approval_for_unattributed_changes: true`, which GitHub's REST docs don't list. It didn't block: PR #1's merge state was `CLEAN`. If a merge ever asks for an approval despite 0 required, look there first.
- Check with `gh api repos/<owner>/<repo>/rules/branches/main` (the rules in effect) and `…/vulnerability-alerts` (204 = alerts on).
- **Verification (plan):** 1, fresh clone (T16); 2, by hand: register a new account, sign out, sign in as the dev user, reload stays signed in, sign out; 3, `/api/me` → 401 Problem Details, health `UP` with `db`; 4, `./mvnw verify` 35 tests (JaCoCo 99.3% lines, 88.9% branches), frontend checks and 24 tests (94.2% / 87.5%), `npm run e2e` 4/4; 5, all 9 PR checks green.
- **Follow-up:** after the backend tests, Surefire waits 30 s and kills the forked JVM ("going to kill self fork JVM … after System.exit(0)", printed as `[ERROR]`, build still passes; also in CI). Something keeps a non-daemon thread alive, probably the fake Keycloak's `HttpServer` or Testcontainers. Worth fixing early in M1: it costs 30 s per build.
- Docker Desktop can stop on its own between sessions; WSL then reports "The command 'docker' could not be found". Start Docker Desktop on Windows.

## 2026-10-23 · T16 done: documentation (T17 next)

**Decided**
1. **`requirements.md` got a status table (§11)**, since it had no status field: unlisted requirements are not started, "Partial" names the milestone that finishes the rest. M0 rows: ACC-1 (social login in M8), NFR-2, NFR-3, NFR-4, NFR-8, NFR-9. T17 updates it when M0 is marked done.
2. **ADR-004 is Accepted** (`docs/adr/004-oidc-provider.md`, the first ADR file), with the hosted provider still chosen in M8. `architecture.md` §10 links it. It covers Keycloak for development, SPA + PKCE with in-memory tokens (BFF rejected for now), CSRF off for the bearer-token API (Semgrep `nosemgrep` and the CodeQL dismissal), and the same-site provider requirement for silent sign-in in M8.
3. **The README's optional gitleaks pre-commit hook runs through Docker** (`git /repo --staged --redact`, M0-17), like the other local scans. No Python or Go install needed.
4. **DB-1** now states the timestamp versions and `<module>_` prefix (`V<yyyyMMddHHmmss>__<module>_<what>.sql`). `coding-standards.md`, CLAUDE.md and `/review-standards` use the Docker Semgrep command, so the review no longer skips the project rules.

**Learned**
- **Fresh-clone check passed:** a scratch clone, following only the README (`cp .env.example .env`, `docker compose up -d`, `./mvnw spring-boot:run`, `npm ci && npm run dev`), passed `npm run e2e` 4/4, i.e. sign-in and greeting. Health showed `db: UP`, and `/api/me` without a token returned 401 `application/problem+json`. Not exercised: the one-time `npx playwright install` steps, because Chromium was already cached on this machine.
- Compose's fixed project name (`name: crewup`) means a second checkout's `docker compose up` replaces the running containers instead of starting new ones. The check ran `docker compose down` first and restored the stack from the repo afterwards.
- From `docker compose up -d`, both services were healthy in about 30 seconds.
- The staged-secret hook command was tried in a scratch clone: a fake `ghp_` token staged → "leaks found: 1", exit 1.

## 2026-10-23 · T15 done: CodeQL and Dependabot (T16 next)

**Decided**
1. **CodeQL skips `.semgrep/`** (`paths-ignore` in the `init` step's inline config, with the reason in a comment): the rule fixtures are insecure on purpose.
2. **The CSRF alert is dismissed, not suppressed in code.** CodeQL (`java/spring-disabled-csrf-protection`, high, `SecurityConfig.java:26`) has no inline suppression comment, so alert #5 is dismissed as "won't fix" with the ADR-004 reason (M0-19). Unlike Semgrep's registry rule, CodeQL does match the lambda form `csrf -> csrf.disable()`.
3. **Dependabot ignores major TypeScript and ESLint updates**, the two plan §2 holds with a technical blocker (a comment on each says when to remove it). Vitest, MSW and React Router are held only because adoption is low, so their major PRs still arrive for a re-check.
4. **A 7-day `cooldown` on every ecosystem**, after Semgrep's `dependabot-missing-cooldown` warning on the first scan of `dependabot.yml` (4 warnings, one per ecosystem). Fixed rather than suppressed: a new release waits a week, so a hijacked one has time to be pulled. GitHub's docs say `default-days` works for all four ecosystems, and GitHub applies 3 days when nothing is set.
5. **`github/codeql-action` v4.39.1**, the same commit `security.yml` pins. Its `releases/latest` is a CodeQL *bundle* release (`codeql-bundle-v2.27.3`), so list the `v4.*` tags to find the action's own latest version.

**Learned**
- First run on PR #1: java-kotlin 1 min 50 s, javascript-typescript 1 min, both green; 120 and 103 rules; one alert (the CSRF one above). Code scanning's default setup is "not configured", so the workflow doesn't conflict with it.
- `paths-ignore` with Java `build-mode: none`: the log doesn't list the extracted files, but the CLI announces `informsAboutUnsupportedPathFilters` and printed no such warning, and no alert points into `.semgrep/`. Strong, but indirect, evidence that the exclusion holds.
- **Dependabot reads `dependabot.yml` from the default branch only**, so pushing it to the feature branch started no update runs. The "config accepted" check moved to T17, after the merge. (The docs don't say this outright; the behavior here matches it.)
- Semgrep's registry packs also scan YAML such as `dependabot.yml`; warnings reach code scanning without failing the gate.
- Dependabot alerts are still off (the API answers 404); T17 turns them on.
- `jq` isn't installed locally (CI has it); use `python3 -c 'import json…'` for local JSON checks.

## 2026-10-23 · T14 done: Semgrep and gitleaks (T15 next)

**Decided**
1. **Fixtures were tried against real patterns in a scratch folder before the red commit**, so no committed fixture would turn out impossible to satisfy (TEST-11 forbids changing it later). The red commit then had stub rules; 0/6 passed, with every `ruleid` line missed.
2. **`r/java.spring` replaces plan §7's `p/spring`**, which doesn't exist (registry 404). `r/java.spring` is Semgrep's Spring rule folder (15 rules, including `spring-csrf-disabled`).
3. **Test code is scanned.** Semgrep's built-in ignore list skips `test/` and `tests/`. A root `.semgrepignore` replaces that list, so it repeats the other defaults and adds `.semgrep/` (the fixtures), keeping "never `UUID.randomUUID()`, even in tests" (T4–T6 entry) enforceable.
4. **One scan, gated on its JSON.** The scan writes SARIF (every severity, uploaded to code scanning) and JSON. `jq` counts `ERROR` findings in the JSON and fails the job if any exist. The JSON leaves out `nosemgrep`-suppressed findings, and puts the severity on each finding (SARIF only has it on the rule). Rejected: a second `--severity ERROR --error` pass (plan §7's wording). The scan is cheap (about 4 s locally, 44 s for the CI job), but one pass is simpler.
5. **No `Ids.java` exclusion** in `no-random-uuid-for-entity-ids`: `Ids` builds UUIDs from `SecureRandom` and never calls `randomUUID()`.
6. **No `.gitleaks.toml`:** the dev-only realm and `.env.example` values aren't flagged.
7. **Images pinned by tag and digest** in `security.yml`'s `env`: `semgrep/semgrep:1.182.0` (latest; 1.x is the only major, released weekly) and `ghcr.io/gitleaks/gitleaks:v8.30.1` (latest of 8.x). `github/codeql-action/upload-sarif` is pinned at v4.39.1 (an annotated tag, dereferenced to its commit).

**Learned**
- **`semgrep --test` finds no tests under a path with a hidden segment** (`.semgrep`, `./.semgrep/`, `/src/.semgrep` all report "No unit tests found"). Mounting the folder as `/rules` works; `semgrep scan --config .semgrep` is not affected.
- `ruleid:` annotations must be `//` comments on the line above the finding. A JSX comment (`{/* ruleid: … */}`) isn't recognized, and a JSX finding is reported where its enclosing expression starts, so `no-dangerously-set-inner-html` uses `focus-metavariable` to report the attribute's line.
- A `metavariable-regex` on a metavariable that an alternative never binds silently rejects that alternative. `slf4j-no-concat` keeps the classic and fluent forms as separate `patterns` blocks for this reason.
- `sql-string-concat` uses taint mode so a query built in a variable and passed on later is caught.
- Project rules are reported as `semgrep.<rule-id>`; `nosemgrep: <rule-id>` with the bare ID still suppresses.
- **The CSRF `nosemgrep` in `SecurityConfig` suppresses nothing today:** the registry rule matches `.csrf().disable()`, not the lambda `csrf -> csrf.disable()`. Kept, as plan §5 asks, with its ADR-004 reason.
- The flag to show suppressed findings is `--disable-nosem` (not `--disable-nosemgrep`). With it, still 0 findings.
- Both images trust their mount point for git (`safe.directory`: `/src` for Semgrep, `*` for gitleaks), so mount the repo at `/src` for Semgrep. Semgrep scans only files git tracks.
- **Dependabot (T15) won't update the image lines in `security.yml`**; bump them by hand.
- Failure checks: a temporary `UUID.randomUUID()` in `IdsTest` made the gate count 1 (a second one under `nosemgrep` was left out); a fake GitHub token committed in a scratch clone failed gitleaks (`github-pat`, exit 1, redacted).
- First run on PR #1: `semgrep` (44 s: 6/6 rule tests, 326 rules on 115 files, 0 findings) and `gitleaks` (9 s, 41 commits, no leaks) green; code scanning shows a "Semgrep OSS" analysis.
- **Local commands** are in plan §7. T16 puts them in the README and `coding-standards.md`, and fixes `/review-standards`, which runs Semgrep only "if installed" (added to T16's checklist).

## 2026-10-22 · T13 done: CI workflow (T14 next)

**Decided**
1. **Triggers stay as plan §9 says: pull requests to `main` and pushes to `main`.** The developer asked what the industry standard is. Their previous job's "CI" was a deployed Kubernetes namespace; here CI means checks on each change before it merges, and deploying to an environment is M8's CD. Feature branches get CI through a PR, so **draft PR [#1](https://github.com/sadamick2112/crewup/pull/1) (`feat/m0-foundation` → `main`) is open now**. T17 marks it ready instead of opening a new one. Rejected: running on pushes to every branch (each push to an open PR would run twice).
2. **Concurrency:** a newer push to a PR cancels the outdated run; runs on `main` are never cancelled. Jobs get `contents: read` only, and timeouts of 20/15/10 minutes.
3. **Actions pinned by commit SHA** with a version comment: `actions/checkout` v7.0.1, `actions/setup-java` v6.1.0, `actions/setup-node` v7.1.0, `actions/upload-artifact` v7.0.2. Each is the latest release of its current major, checked with `gh api repos/<action>/releases/latest`. The pinned SHA is the commit the tag points to (annotated tags dereferenced).
4. **Coverage tables** use inline scripts: awk over JaCoCo's CSV, Node over Vitest's `coverage-summary.json`. The summary and upload steps run `if: !cancelled()` when the report exists, so a run that fails the coverage gate still shows its numbers.

**Learned**
- First run on PR #1: all three jobs green (backend about 1.5 min, frontend 45 s, api-client 23 s); artifacts `backend-coverage` and `frontend-coverage` uploaded.
- Failure check on a throwaway draft PR (#2, closed, branch deleted): `backend` failed in `IdsTest` (an assertion changed to version 4), `frontend` at `lint` (`no-console`), and `api-client` at the diff check (a hand edit to `schema.d.ts`).
- Run locally first: the jobs only call project scripts, so the same commands give the same answer. [`act`](https://github.com/nektos/act) can run `ci.yml` itself in Docker; not installed, and worth considering in M8 when the workflow grows.
- `actionlint` (Docker image `rhysd/actionlint`) checks workflow files, including shellcheck on `run:` scripts. A Node script inside shell single quotes trips SC2016 because of JS template strings; a quoted heredoc (`node - <<'EOF'`) avoids it.
- Watching CI from the terminal: `gh pr checks`, `gh run watch`, `gh run view --log-failed`.

## 2026-10-22 · T12 done: end-to-end sign-in test (T13 next)

**Decided**
1. **Four tests instead of one long journey** (TEST-3, one behavior per test): sign in and out, reload stays signed in, and the layout + axe checks at 1280 and 360 px signed out and signed in. Each test gets a new browser context (no tokens, no Keycloak session) and signs in itself; a sign-in takes about a second.
2. **Sign-out also proves the Keycloak session ended:** after "Sign out", a reload (which tries a silent sign-in) still shows "Sign in".
3. **`e2e/global-setup.ts` checks Keycloak and the backend before any test** and fails with how to start them, instead of a timeout mid-test. It reads `CREWUP_BACKEND_PORT` and `CREWUP_OIDC_ISSUER_URI` like `vite.config.ts`. Vite is started by Playwright (`webServer`), or reused if `npm run dev` is already running.
4. **Wiring:** `tsconfig.e2e.json` (DOM lib, because `page.evaluate` callbacks run in the browser) is referenced from `tsconfig.json`, so `typecheck` and ESLint cover `e2e/` and `playwright.config.ts`. Vitest now includes only `src/**/*.test.{ts,tsx}` (its default pattern would pick up `e2e/*.spec.ts`). ESLint and Prettier ignore `playwright-report/` and `test-results/`.
5. Versions as pinned in plan §2: `@playwright/test` 1.64.1 (latest; 1.x is the only major) and `@axe-core/playwright` 4.14.0 (latest, matches `axe-core`), both re-checked on npm (not deprecated).

**Learned**
- **One-time machine setup:** Playwright's Chromium needs system libraries that minimal WSL Ubuntu lacks (first missing: `libnspr4.so`). Install with `sudo env "PATH=$PATH" npx playwright install-deps chromium` in `frontend/` (`env PATH` because `sudo` resets `PATH` and Node comes from nvm), after `npx playwright install chromium`. For T16's README.
- The first try failed on the apt lock: Ubuntu's `apt.systemd.daily` update had hung for over a day. `sudo systemctl stop apt-daily.service` cleared it.
- The checks were tried against a broken page once: a 500 px element at 360 px fails the scroll check (156 px too wide), and `#eee` text fails axe's `color-contrast`. A first attempt "passed" because it changed the page before React had rendered; checks must wait for the page (e.g. the Sign in button) first.
- With the backend unreachable, the run stops in global setup with "…/actuator/health is not reachable. Start the backend: …".
- Coverage unchanged: 86.5% lines, 79.2% branches (e2e code isn't measured by Vitest).

## 2026-10-22 · T11 done: sign-in and home page (T12 next)

**Decided**
1. **HomePage reads sign-in state with react-oidc-context's `useAuth()`** (chosen from three options with previews). Tests supply the library's `AuthContext` with a fake value (`isAuthenticated`, `signinRedirect`/`signoutRedirect` as `vi.fn()`, one cast in the helper). Rejected: props plus a thin container (untested glue), and a project-owned `Session` interface with an adapter (an extra layer).
2. **`restoreSession(userManager)` returns the user or null.** `login_required` (no SSO session, the normal case) is silent; any other silent-sign-in error also means signed out, but calls `console.warn` so a misconfiguration isn't invisible.
3. **`restoreSession` runs before the first render** (top-level `await` in `main.tsx`), skipped when the URL has sign-in parameters (`hasAuthParams()`): that's Keycloak answering, either the redirect back or the silent-sign-in iframe, and `AuthProvider` handles it. So a reload never flashes "Sign in" before the greeting. The routes and providers live in `main.tsx` (the excluded bootstrap file, as plan §6 says).
4. **One callback page for both flows:** `redirect_uri` and `silent_redirect_uri` are both `/auth/callback`. `signinCallback()` reads the request type from the stored state and handles a redirect or the iframe. `AuthCallbackPage` waits for `isLoading` to end, then navigates to `/` (a failed sign-in lands on "Sign in").
5. **The OIDC authority comes from `CREWUP_OIDC_ISSUER_URI`** in the repo root `.env` (the backend's issuer), exposed by `vite.config.ts` as `import.meta.env.VITE_OIDC_AUTHORITY` and typed in `src/vite-env.d.ts`. One value for both sides, like the backend port in T9.
6. **`ErrorMessage` uses shadcn's `Alert`** (TS-8; `role="alert"`, added with `shadcn add alert`, `package.json` unchanged). It shows the reference only for an `ApiError`; a network `TypeError` (dev server down) shows the message without one.

**Learned**
- Keycloak answers `prompt=none` without an SSO session with a 302 to the callback carrying `error=login_required`, and no frame-blocking header, so the silent iframe works (checked with curl, then in the browser).
- `signinSilent()` uses the refresh token when the stored user has one, so renewals after startup don't need the iframe; only the first silent sign-in after a reload does.
- Vite's `define` for an `import.meta.env.*` key shows up in dev in the injected `import.meta.env` object, and in the build as a literal (the minifier writes it with backticks, so grep for the URL, not `"…"`).
- `spring-boot:run` runs two JVMs: Maven's, and the forked app. `kill <app pid>` stops it gracefully; Maven then reports "exit code: 143" (128 + SIGTERM) as an `[ERROR]`, which is expected.
- Each TanStack Query retry is a new request with a fresh `traceparent`, so the reference shown is the last attempt's (the bottom `me` row in DevTools → Network). Default retries (3) delay the error message a few seconds; left at the default.
- Coverage: 86.5% lines, 79.2% branches. Uncovered: `oidcConfig.ts`, `AuthCallbackPage`, `ErrorMessage`'s no-trace-ID branch, and T10's `useApiClient()` guard.
- Checked by hand with the stack running: sign in, greeting, reload stays signed in (three `/api/me` 200s in the backend log, each with its own browser trace ID), sign out; and with the backend stopped, the error with a reference matching the console.
- **Tag `m0-sign-in-works` → `0cf42bb`** (replaces the planned `m0-frontend-done`: the UI keeps growing every milestone, so "frontend done" would mislead; the tag names the first full journey, sign in and greeting). Tags name what works at that point, not a layer being finished.

## 2026-10-22 · T10 done: generated API client and useMe (T11 next)

**Decided**
1. **How the token and signed-in state reach the API code** (chosen from three options with previews): `createApiClient(getAccessToken)` builds the client once and asks for the token on every request, so a silently renewed token is used right away. `ApiClientContext` (`src/shared/api/apiClientContext.ts`) hands the client to hooks. `useMe(isSignedIn)` takes the signed-in state as an argument. T10 has no OIDC code; T11 wires `getAccessToken` to `userManager.getUser()` and passes `auth.isAuthenticated`. Rejected: `useMe()` calling `useAuth()` itself (pulls `react-oidc-context` into T10), and a module-level client with a token setter (global state that every test resets).
2. **`ApiError` carries the trace ID the browser sent**, not the one in the response body. Both match for backend errors, but an error from the Vite proxy (backend down) has no body (tested: 502 with no body). `ApiError` has `status` and `traceId` only (GEN-10).
3. **A fetch wrapper instead of openapi-fetch middleware** (plan §6 says middleware). With middleware, `onResponse` would have to parse the trace ID back out of the request headers. In the wrapper it stays a local variable. Same behavior, one function.
4. **Extra tests beyond the task list:** `M0_9_clientSendsNoAuthorizationWhenSignedOut`, `M0_28_traceparentIdsAreNeverAllZeros` (plan §6's "never all zeros"), and `M0_28_failedRequestWithoutProblemDetailsStillCarriesTraceId`.

**Learned**
- openapi-fetch passes on errors thrown from `fetch` (and from `onResponse`) to the caller, so `client.GET` rejects with the `ApiError`. TanStack Query then exposes it as the query's `error`.
- The client sets `baseUrl: window.location.origin`: Node's `Request` (used in tests) rejects relative URLs, while browsers resolve them.
- `erasableSyntaxOnly` rules out constructor parameter properties (`constructor(readonly status: number)`), so `ApiError` declares its fields.
- Prettier wraps at 80 columns (its default) even though comments are written to 120; let `npm run format` decide.
- Coverage: 96.8% lines, 83.3% branches. The only uncovered line is `useApiClient()`'s "no context provided" guard (a wiring mistake, not spec behavior), left untested on purpose.
- `useMe`'s query function casts `data` to `UserProfile`: openapi-fetch types it as possibly undefined (the error branch), but the wrapper throws before that branch can happen.

## 2026-10-19 · T9 done: frontend scaffold and a version rule (T10 next)

**Decided**
1. **Version rule** (plan §2, replacing "newest of everything"): versions must be industry standard, modern, not deprecated, and compatible without workarounds. That means the most used major (npm per-version weekly downloads) at its latest non-deprecated release. The developer wants the app to show how modern applications are built, and "modern" doesn't mean "latest". Measure with data (`api.npmjs.org/versions/<pkg>/last-week`, `npm view <pkg>@<v> deprecated`), not memory or another assistant's list. A list from Copilot (Next.js 15.1, TS 5.6, Tailwind 3.4) matched early 2025, not the download data.
2. **Frontend versions changed** under that rule: Vitest 5 → 4.1.12, MSW 3 → 2.15.0, React Router 8 → 7.18.4 (T11), plus latest patches. jest-dom stays 7.0.1 because 6.10.0 is deprecated. TypeScript 5.9.3 and ESLint 9.39.5 stay; plan §2 gives the measured reasons.
3. **TypeScript 7 was tried and declined.** TS 7 has no JS API until 7.1, so `typescript-eslint` refuses to start and `openapi-typescript` crashes. The TypeScript team's side-by-side install (TS 7 `tsc` plus `@typescript/typescript6` aliased as `typescript`) worked in a scratch copy, but it needs aliases and overrides.
4. **ESLint 9 is the one known exception to "not deprecated"** (end of life since 2026-08-06). jsx-a11y doesn't support ESLint 10 yet ([#1075](https://github.com/jsx-eslint/eslint-plugin-jsx-a11y/issues/1075)). The `jsx-a11y-x` fork (niche, 0.x) and an npm override (a workaround) were declined. Revisit when #1075 ships.
5. **shadcn defaults:** components in `src/components/ui`, helper in `src/lib/utils.ts` (not plan §1's `src/shared/ui`), Radix base, Nova preset. shadcn's own `cn` package stays, because registry components import it directly, so swapping in `clsx` + `tailwind-merge` would mean editing every added component.
6. **Prettier uses its defaults** (double quotes, semicolons; TS-1), plus the Tailwind plugin.

**Learned**
- create-vite 9.3's `react-ts` template now ships oxlint and TypeScript 7 (where `strict` is the default). We replaced both per the plan and set `"strict": true` explicitly.
- `shadcn add` re-adds `cn` to `package.json` and bumps `shadcn` to `^latest`. After running it, check `package.json` and pin exact versions again.
- Vite 8's `resolve.tsconfigPaths: true` reads the `@/` alias from `tsconfig.app.json`; the shadcn CLI also needs `paths` in the root `tsconfig.json`.
- Vitest globals are off, so Testing Library can't clean up automatically. `src/test/setup.ts` calls `cleanup()` (checked: without it a render leaks into the next test), with the lint rule disabled on that line and the reason.
- Coverage currently measures 0 of 0 lines (everything in `src/` is an approved exclusion), and an untested file made the gate fail as expected. T10 adds the first measured code and the `schema.d.ts` exclusion.
- `npm audit` reports 7 high findings, all one `braces` advisory reached through the `shadcn` CLI (dev only, no fixed version; `audit fix --force` would downgrade shadcn to 1.0). For T15's triage.
- npm 11 warns that MSW's postinstall script isn't in `allowScripts`. It only copies the browser worker, which the Node tests don't use.
- The Vite proxy reads `CREWUP_BACKEND_PORT` from the repo root `.env` (or the environment), checked by hand against a stub server. `.env` files copied before T9 don't have the variable, which is fine because both sides default to 8080.

## 2026-10-19 · Handoff: backend done (T1–T8), frontend starts with T9

**Where things stand**
- Branch `feat/m0-foundation`. T1–T8 are done and checked off in `tasks.md`; the backend skeleton is tagged `m0-backend-done` (see the entry below). No CI yet (T13).
- **Next: T9** (frontend scaffold, exempt from test-first). Start a new session with: *"Read CLAUDE.md and docs/project-log.md, then implement T9 from docs/specs/m0-foundation/tasks.md."*
- T9 includes a backend item deferred from T3: `server.port: ${CREWUP_BACKEND_PORT:8080}` in `application.yml`, `CREWUP_BACKEND_PORT` in `.env.example`, and the Vite `/api` proxy reading the same variable.

**What the frontend needs to know about the backend**
- Run it: `docker compose up -d` (repo root), then `cd backend && ./mvnw spring-boot:run` (port 8080; Ctrl+C stops it gracefully). Before starting it, check nothing else holds 8080 (`ss -ltn | grep :8080`); if something does, ask instead of killing it.
- No CORS: the Vite dev server proxies `/api` to the backend, so the browser sees one origin (plan §6).
- **API contract:** `api/openapi.json` (generated by the backend; T10's `gen:api` reads it). `GET /api/me` → `UserProfile { id: uuid, displayName: string, email: string | null }` (all three required). Errors are `application/problem+json` with a top-level **`traceId`** (`ProblemDetail` schema), which T10's `ApiError` and T11's "Reference: {traceId}" use.
- Error `type`s are URNs: `urn:crewup:problem:unauthenticated` (401), `urn:crewup:problem:access-denied` (403). Spring's own errors (e.g. 404) have no `type`.
- **Tracing:** the backend continues an incoming W3C `traceparent` (`00-<32 hex>-<16 hex>-01`). The backend log's request line shows that trace ID in brackets (`[<traceId>-<spanId>] … Request completed http.method="GET" http.route="/api/me" … subject="…"`), so T10/T11's correlation can be checked by hand there. Read full log lines; the subject is at the end.
- **Manual API calls:** `TOKEN=$(python3 infra/keycloak/get-dev-token.py)` (tokens last 5 minutes). Swagger UI at http://localhost:8080/swagger-ui.html has an **Authorize** button for the token.
- Keycloak client `crewup-web` allows redirects to `http://localhost:5173/*` (dev user `dev@crewup.local` / `dev`).

**How the developer likes to work (from T3–T8)**
- One task at a time; explain unfamiliar concepts when asked (frontend is new to the developer, so expect more questions here than in the backend).
- Ask before decisions the spec or plan doesn't settle, especially anything a committed test will assert, using concrete options and previews. Record decisions in this log.
- Verify against real files and output (jars, the npm registry, actual tool output) instead of assuming; say when something was a guess, and own mistakes plainly.
- Versions are pinned in plan §2 (note the deliberate holds: TypeScript 5.9.3, ESLint 9). Node via nvm: in non-interactive shells run `. ~/.nvm/nvm.sh` first.
- Commit messages follow the task's suggested message, with a body that says what was verified. The developer pushes; tags are created only when asked.

## 2026-10-19 · T8 done; milestone tags (T9 next)

**Decided**
1. **Milestone tags keep snapshots to look back on** (this is also a learning project: how an app grows step by step with Claude Code). Annotated tags, never moved or reused: `git tag -a <name> <commit> -m "<what it marks>"`, pushed separately with `git push origin <name>`.
   - `m0-backend-done` → `521f895`: backend skeleton (T1–T8).
   - Planned: `m0-frontend-done` after T11 (created as `m0-sign-in-works`, see the T11 entry), `m0-done` on the merge to `main`, then one per milestone (or notable stage) from M1.
   - Browse with `git checkout <tag>` (detached HEAD; `git switch -` to return) or under *Tags* on GitHub. Commit history and this log fill in the steps between tags.
2. **No CI until T13, by plan order.** GitHub Actions is enabled on the repo, but no workflow exists yet; the developer chose not to pull a backend job forward.

**Learned (T8)**
- `shared.ProblemDetailSchema` documents the error body for OpenAPI, because Spring's `ProblemDetail` class would show a nested `properties` map instead of the top-level `traceId`. It's documentation only, so its one line is uncovered.
- The OpenAPI server URL is `/`, so the contract doesn't depend on host or port. `ApiProbeController` is `@Hidden` to keep it out of the contract.
- Regenerate the contract: `cd backend && ./mvnw test -Dtest=OpenApiContractTest -Dopenapi.update=true`.

## 2026-10-16 · T7 done (T8 next)

**Decided**
1. **Readable local logs show key-values:** `logging.pattern.console` is Boot's default pattern plus `%kvp` (e.g. `… : Request completed http.method="GET" http.route="/api/me" …`). The developer compared real output of Boot's default, default + `%kvp`, and ECS JSON before choosing. JSON (`LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs`) writes key-values as fields.
2. **Requests with no matched route log `http.route="UNMATCHED"`** (401s, unknown paths), never the raw URL. Every `/api` request is logged, so `RequestLoggingFilter` wraps the security filters (plan §5 had it after them, which would skip 401s) and reads the subject from the request-attribute security context.
3. **Request-line field names stay as in the plan** (`http.method`, `http.route`, `http.status`, `durationMs`, `subject`), not ECS's `http.request.method` etc.; revisit when a log platform arrives.
4. **Static resource mappings are off** (`spring.web.resources.add-mappings: false`): otherwise the catch-all `/**` handler matched unknown paths (route `/**`). springdoc serves Swagger UI with its own handler.

**Learned**
- Boot 4 needs `spring-boot-micrometer-tracing-opentelemetry` next to the bridge (plan §3 updated). Boot 4 tests turn off only trace *export*, not tracing, so no test annotation is needed.
- Boot's ECS output puts trace IDs in `traceId`/`spanId` (not ECS's `trace.id`) and key-values as top-level fields.
- Logging tests run in their own context with ECS output and parse fields (`JsonLogLines`); the Postgres container is now one per test run, so that second context doesn't start another database.
- When checking logs by hand, don't truncate lines (`cut -c…`): the request line's `subject` is at the end, and a truncated line looked like a bug.

## 2026-10-15 · T4–T6 done (T7 next)

**Decided**
1. **Problem Details `type` URIs are URNs:** `urn:crewup:problem:<kind>` (API-4). No domain is needed; RFC 9457 allows non-resolvable URIs. So far: `unauthenticated` (401), `access-denied` (403). Spring's own MVC errors (e.g. 404) still use the default `about:blank`.
2. **Backend tests use a fake Keycloak** (`TestOidcProvider`: JDK `HttpServer`, RSA key generated per run, serves OIDC discovery and the JWKS). `@IntegrationTest` points `issuer-uri` at it, so tests run the production decoder: signature, issuer, expiry and the `crewup-api` audience. A public-key file was rejected because Boot then builds a different decoder (only when `issuer-uri` is unset) that skips the issuer check. Tests that only need *a* signed-in user use spring-security-test's `jwt()`; `ApiSecurityTest` stays the one place that tests token validation.
3. **Display name with no name claims** (no `name`, `preferred_username` or `email`): the placeholder "CrewUp user"; the next token with a name replaces it.
4. **First-call provisioning uses `INSERT … ON CONFLICT (subject) DO NOTHING`** then re-reads, instead of catching the unique-constraint error as plan §5 described. In PostgreSQL a failed insert aborts the transaction, so that re-read would fail. The concurrency test covers the lost-insert branch reliably.
5. **`UUID.randomUUID()` is never used, even in tests:** test-unique values use `Ids.newId()` (anticipating T14's `no-random-uuid-for-entity-ids` rule).

**Learned**
- `/error` is public in `SecurityConfig`: Spring Boot renders unhandled exceptions there, and denying it would turn a 500 into a 403. MockMvc doesn't exercise this.
- A 401 carries `WWW-Authenticate: Bearer` (adds `error="invalid_token"` for a rejected token, never the reason).
- Spotless wraps long comments. `nosemgrep` must be on the line directly above the finding, so keep it short and put the reason on its own line.
- Security and Problem Details classes live in the root package `app.crewup` for now. T7 moves the Problem Details writing into `shared.ProblemDetailsSupport`.
- `./mvnw spring-boot:run` fails with "Port 8080 was already in use" if an earlier run is still going; stop it with Ctrl+C (graceful), or `kill <pid>` (also graceful).
- Backend port override via `.env` was deferred to T9 (added to its checklist).

## 2026-10-14 · T3 backend scaffold done (T4 next)

**Learned while scaffolding** (Boot 4.1.1, generated from start.spring.io and trimmed to plan §3)
- **Boot 4 renamed and split starters.** Plan §3's names map to `spring-boot-starter-webmvc` (was `-web`), `spring-boot-starter-flyway` (needed for Flyway auto-configuration; `flyway-core` alone no longer triggers it), `spring-boot-starter-security-oauth2-resource-server`, and per-module test starters (`spring-boot-starter-webmvc-test` brings `spring-boot-starter-test` and MockMvc; `spring-boot-starter-security-test` brings `spring-security-test`). Testcontainers 2 uses `org.testcontainers.postgresql.PostgreSQLContainer` and the `testcontainers-postgresql` / `testcontainers-junit-jupiter` artifacts.
- start.spring.io's "modulith" adds observability, actuator and runtime modules; they were left out (not in plan §3; observability belongs with T7's tracing choice). The tracing bridge itself is added in T7, as that task says.
- **First migration is Modulith's event registry, not an empty baseline.** `spring-modulith-starter-jpa` maps `event_publication` and `event_publication_archive`, and `ddl-auto: validate` fails without them. So `db/migration/shared/V20261014110000__shared_create_event_publication.sql` creates both (columns from Modulith 2.1's JPA mapping). This satisfies M0-2's "Flyway runs on startup" with a real migration.
- `classpath:db/migration/*` (wildcard) works with Flyway as planned.
- **Temporary `app.crewup.SecurityConfig` permits everything**, because the security starter otherwise puts basic auth on every endpoint, including health. **T5 replaces it.**
- Integration tests use the `@IntegrationTest` meta-annotation (`@SpringBootTest` + MockMvc + Testcontainers Postgres `postgres:18.6`), so the whole run shares one context and one container. Tests use constructor injection (JAVA-3).
- Without a `.env` (as in CI), tests still pass: `@ServiceConnection` replaces the datasource settings.
- Checked by hand: an untested class drops coverage to 25% line / 0% branch and fails `verify`; `./mvnw spring-boot:run` starts against Compose and `/actuator/health` shows `db: UP` without details.

## 2026-10-14 · Handoff: M0 implementation started (T1–T2 done, T3 next)

**Where things stand**
- Branch `feat/m0-foundation`, not pushed yet. Spec, plan and tasks for M0 are **approved**; work proceeds one task at a time from `docs/specs/m0-foundation/tasks.md` (check boxes show progress).
- **T1 done:** versions pinned in `plan.md` §2 (note the deliberate holds: TypeScript 5.9.3, ESLint 9). **T2 done:** `docker-compose.yml`, `.env.example`, `infra/keycloak/crewup-realm.json`.
- **Next: T3** (backend scaffold). Start a new session with: *"Read CLAUDE.md and docs/project-log.md, then implement T3 from docs/specs/m0-foundation/tasks.md."*

**Developer machine (WSL Ubuntu)**
- Java: Temurin **25.0.3** via SDKMAN (one patch behind Adoptium's 25.0.4.1; fine). Node **24.22.0** via nvm (`.nvmrc`). Docker Desktop with WSL integration (Docker 29.7.2, Compose v5.5.1).
- Non-interactive shells may not load SDKMAN/nvm. If `java`/`node` aren't found, run `source ~/.sdkman/bin/sdkman-init.sh` and `. ~/.nvm/nvm.sh` first.
- `.env` already exists locally (copied from `.env.example`). Postgres and Keycloak may still be running: `docker compose ps`.

**Local environment facts learned in T2**
- Keycloak: admin console http://localhost:8180/admin (`admin` / `admin-dev-only`). No data volume, so `docker compose down` resets the realm (and any registered accounts) from the committed export. Postgres data persists in the `postgres-data` volume.
- Access tokens: issuer `http://localhost:8180/realms/crewup`, audience `crewup-api`, `azp` `crewup-web`, with `sub`, `email`, `name`, `preferred_username` claims. **Lifetime is 5 minutes** (Keycloak default).
- **Getting a real token for manual API calls:** `TOKEN=$(python3 infra/keycloak/get-dev-token.py)`. The password grant is disabled on purpose, so the script runs the PKCE flow. `--claims` shows the claims; `--check` re-verifies the realm (PKCE required, foreign redirects refused, registration on, `crewup-api` audience). Used by T6's and T7's manual checks.
- Keycloak's cookies are `Secure`. Browsers (and Playwright) send them to `http://localhost` because it's a secure context; plain HTTP clients such as Python's cookie jar don't, which is why the script clears the flag. Keep this in mind for any scripted sign-in.

## 2026-10-14 · Logging and request tracing

**Decided** (spec M0-26…M0-28 and §6 decision 10; `coding-standards.md` LOG-1…LOG-7; NFR-9 updated)
1. **Scope:** request tracing (W3C Trace Context via Micrometer Tracing + OpenTelemetry bridge) and browser-to-backend correlation (the web app sends `traceparent`; errors show "Reference: {traceId}"). Logs are viewed in the console: readable locally, ECS JSON elsewhere.
2. **Deferred:** a log/trace viewer and trace export (no code change needed later); audit trail of data changes (NFR-7, from M1); sending non-API browser errors to the backend (needs an abuse-protected endpoint).
3. New task **T7** (logging and tracing, test-first); later tasks renumbered T8–T17.

## 2026-10-14 · Test-driven development

**Decided** (CLAUDE.md principles 5–6, `coding-standards.md` TEST-2, TEST-11, GIT-2)
1. **Test first** for domain rules, endpoints, and bug fixes: tests from the acceptance criteria, run to confirm they fail on assertions, committed, then implemented. Setup, configuration, and visual styling are exempt.
2. **Committed tests are never changed to make them pass.** A wrong test means stopping to ask; an approved correction gets its own commit.
3. Stubs that throw are allowed in the red commit so tests compile. Push after the green commit, so CI doesn't run red.
4. `/review-standards` checks the commit order and that the implementation commit left the tests unchanged. M0 tasks T4, T5, T6, T7, T10, T11, T14 are test-first (numbers after the logging task was added).
5. **Coverage is a floor, not a goal** (CLAUDE.md principle 7, TEST-9): no tests written only to raise coverage; thresholds lowered or exclusions added only with the user's approval (replacing the earlier "never lowered").
6. **Not getting stuck on coverage:** TEST-12 defines what to do when the gate fails (one pass at testing untested criteria and deleting dead code, then stop and report). GEN-10 bans speculative code, so the M0 `@RestControllerAdvice` moved to M1. Coverage runs at every task's final commit. Sign-in logic sits in a small tested `restoreSession()`, keeping the untested OIDC wiring to one config file.

## 2026-10-14 · Test quality gates

**Decided** (spec M0-24, M0-25 and §6 decision 9; plan §8)
1. **Coverage gate:** JaCoCo (backend, unit + integration tests) and Vitest V8 coverage (frontend) fail the build below 80% line / 70% branch. Limits only go up. Summaries go to the CI run page, not PR comments.
2. **Accessibility:** axe-core in component tests and `@axe-core/playwright` end to end, with zero violations allowed (NFR-4). Color contrast is checked only in Playwright.
3. Commitlint was declined; GIT-1 stays review-only. PIT mutation testing is a candidate after M0. Performance budgets (NFR-5) and container image scanning belong to later milestones.
4. M0 `plan.md` and `tasks.md` drafted; plan approved.

## 2026-10-14 · Coding standards

**Decided** (details in `coding-standards.md` and `specs/m0-foundation/spec.md` M0-20…M0-23, §6 decision 8)
1. `docs/coding-standards.md` holds the rules, each with an ID (JAVA-3, TS-5…) and how it's enforced. `CLAUDE.md` points Claude Code at it for all generated code.
2. **Java formatting: Palantir Java Format** through Spotless (4-space indent, 120 columns), checked in `./mvnw verify`. Frontend: ESLint + Prettier + `tsc` strict, checked in CI.
3. **PR review is local, not in CI:** run the `/review-standards` skill (`.claude/skills/review-standards/`) before every PR; the PR template has it as a checklist item. A Claude review in GitHub Actions was considered and deferred to avoid API costs. The M2 `code-reviewer` subagent should reuse the same standards document.

## 2026-10-14 · Security scanning built in

**Decided** (details in `specs/m0-foundation/spec.md` M0-15…M0-19 and §6)
1. Security scanning starts in M0 CI: Semgrep (community + project rules in `.semgrep/`), CodeQL (free because the repo is public), gitleaks, and Dependabot. High-severity findings fail the build.
2. CSRF stays off for the stateless bearer-token API. Scanner findings about it are suppressed with a reference to ADR-004, not "fixed".
3. Broken access control is the main risk and static analysis can't catch it reliably, so per-role authorization tests and an `@PreAuthorize` guard test (M2) cover it. An OWASP ZAP baseline scan of the deployed app joins in M8.

## 2026-10-13 · Open questions resolved

**Decided** (details in `requirements.md` §9)
1. New Guys can add their own ratings, dishes, and notes on Activities they attended.
2. Friends can send invites; invitees always join as New Guy.
3. Catalogs are per group (ADR-007 now Accepted).
4. New Guys can't see members' contact info until promoted (MEM-4, NFR-1 updated).
5. Track board game ownership: new **BG-8 [P2]**, built alongside AI-7.
6. No history import. Added to non-goals and removed from the Phase 2 roadmap.
7. No commercial intent yet. Treat third-party APIs as non-commercial use; revisit if that changes.
8. Activity Type table prefixes are the module name (`dining_`, `boardgames_`, `movies_`), replacing the short `bg_` / `mov_`. Core module tables stay unprefixed.
9. **Build tool: Maven** (with the Maven Wrapper, `./mvnw`) instead of Gradle. The developer prefers it; there are no other changes to the stack.
10. **Local LLM option (AI-0e):** The LLM provider is chosen per deployment by `crewup.ai.provider` (`anthropic` default, or `ollama`). Feature code uses only Spring AI's `ChatClient`. The eval set runs against both. Where Ollama runs (Windows native with GPU vs. Docker Compose) and which local model to use are decided in the M7 plan.

**Git and repo**
- Housekeeping commit done (`.gitignore` restored from GitHub's Java template plus project entries, `.gitattributes` added). No Zone.Identifier files were present.
- Git identity in WSL uses the GitHub noreply address.
- Branching: commit docs directly to `main` for now. From Milestone 0 on, use one branch per feature spec (e.g. `feat/m0-foundation`) with a PR to `main`.

## 2026-10-13 · Environment setup

**Decided**
- **Develop in Ubuntu on WSL**, not on Windows. The repo lives at `~/workspace/crewup` (Linux file system, not `/mnt/d`), for fast Gradle/npm, working Vite hot reload, and smoother Docker and Testcontainers.
- **Editor:** VS Code on Windows, opened in WSL via `code .` (the window shows "WSL: Ubuntu"), with the Claude Code extension installed on the WSL side. The extension shares CLAUDE.md, skills, settings and history with the `claude` command-line tool.
- The repo was created on GitHub with a README and LICENSE, then cloned. The spec files were copied in afterwards.

**Housekeeping done or to do**
- `.gitignore` must include `CrewUp App.docx` (private source notes) and `*:Zone.Identifier` (Windows download leftovers created when copying files from Windows into WSL). If GitHub generated a template `.gitignore`, keep its content and add these two lines.
- Delete stray files: `find . -name '*:Zone.Identifier' -delete`
- Add `.gitattributes` containing `* text=auto eol=lf` to keep Linux line endings.
- An old copy of the specs remains in `D:\Workspace\Portfolio\CrewUp`. **The repo copy is the source of truth.** Delete or rename the old one.
- Snapshot copies of the specs also exist in the CrewUp Project on claude.ai (dated October 6, 2026). They will go stale; the repo wins.

## 2026-10-06 · Planning and spec

**Context about the developer**
- Principal software engineer with a Java and Spring Boot backend background. Prior AI coding experience: Devin IDE with Claude models (vibe coding).
- Goals: learn Claude Code properly, learn frontend development, learn applied AI techniques, and build a portfolio app for the resume. Possibly sell it in an app store later.
- Subscription: **Claude Pro**. Claude Code usage shares one pool with Claude chat (a 5-hour rolling window plus a weekly cap). Keep sessions focused on one task.

**Decided**
1. **Workflow: native Claude Code files, not SpecKit (for now).** Use spec-driven discipline with `docs/specs/<feature>/spec.md`, `plan.md` and `tasks.md`, plus CLAUDE.md as the "constitution". Build our own skills and subagents gradually. Try SpecKit later as an experiment on the Movies feature. Full reasoning is in `docs/approach.md`.
2. **Client:** React + TypeScript web app (responsive, installable as a PWA) first. React Native/Expo mobile app later, reusing the same API.
3. **Backend:** Spring Boot **modular monolith** with Spring Modulith. Each Activity Type (Dining Out, Board Gaming, Movies) is an isolated module, extractable to a microservice later. This replaces the original idea of a microservice per Activity Type.
4. **First release (MVP) scope:** core logging (groups, members, roles, QR invites, Events, Dining Out, Board Gaming, timeline and search) plus **one AI feature: board game lookup** from BoardGameGeek.

**Changes from the original "CrewUp App" document** (reasons are in `requirements.md` §8)
- User (login) is separate from Member (profile in a group). Roles live on Member. This allows historical members without accounts.
- CSV strings became structured lists. Dates and times are real types. Board game results record a placement per player. Movie (catalog) is split from Movie Activity (a viewing). Restaurant ratings are derived from per-visit reviews. Every Activity belongs to an Event. Movie data comes from TMDB/OMDb instead of IMDb scraping.

**Important notes**
- CrewUp's own AI features call the Claude API, which is **billed separately from Pro** through an Anthropic Console account. Set a spending limit there.
- BoardGameGeek requires registering an application for XML API access. Check its terms before Milestone 7.

## Next steps

1. Build M1 task by task on `feat/m1-crews` (`docs/specs/m1-crews/tasks.md`), starting with T1.
2. At the start of M2: re-check the Dependabot-ignored majors against the version rule (T17 entry, 2026-10-29).

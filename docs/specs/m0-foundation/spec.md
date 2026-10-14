# M0 — Foundation: Spec

**Status:** Approved · October 14, 2026 · Milestone M0 in `roadmap.md`
**Requirements:** ACC-1 (partial, see §5) · NFR-2 · NFR-3 · NFR-8 · NFR-9

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

## 7. Open questions

None.

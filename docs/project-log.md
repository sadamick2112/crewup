# CrewUp — Project Log

Decisions and context from planning sessions, so any new Claude Code session can pick up where the last one left off. Newest entries go at the top.

---

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

1. Push `main` to GitHub. Delete or rename the old specs copy in `D:\Workspace\Portfolio\CrewUp`.
2. Write the first feature spec: `docs/specs/m0-foundation/` (see `docs/roadmap.md`, Milestone 0), then plan and build it with Claude Code on a `feat/m0-foundation` branch.

# CrewUp — Project Log

Decisions and context from planning sessions, so any new Claude Code session can pick up where the last one left off. Newest entries go at the top.

---

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

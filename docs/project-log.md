# CrewUp — Project Log

Decisions and context from planning sessions, so any new Claude Code session can pick up where the last one left off. Newest entries go at the top.

---

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

1. Finish the housekeeping above, commit, and push.
2. Answer the **open questions** at the end of `docs/requirements.md` and update the file.
3. Write the first feature spec: `docs/specs/m0-foundation/` (see `docs/roadmap.md`, Milestone 0), then plan and build it with Claude Code.

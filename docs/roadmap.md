# CrewUp — Roadmap

**Status:** Draft v0.1 · October 6, 2026

Each milestone is a vertical slice (database → API → UI → tests) that ends in a demoable, merged state. Each one gets a folder in `docs/specs/` and runs through the spec → plan → tasks → implement → review loop described in `approach.md`.

## MVP (Phase 1)

| # | Milestone | Requirements | What you'll learn |
|---|---|---|---|
| **M0** | **Foundation:** repository, `CLAUDE.md`, Docker Compose (Postgres, Keycloak), Spring Boot + Modulith skeleton, React + Vite skeleton, CI pipeline, generated API client, "hello authenticated user" end to end | ACC-1 | Claude Code basics, plan mode, CLAUDE.md, the frontend toolchain |
| **M1** | **Groups and members:** create/edit/delete group, members (including historical), profile editing, group switcher | GRP-1…4, MEM-1…5, ACC-2 | React forms, TanStack Query, JPA modules |
| **M2** | **Roles and invites:** permission matrix, method security, QR invite and link, claim a historical profile | ROL-1…2, INV-1…3 | Spring Security, authorization tests, QR generation |
| **M3** | **Events core:** Event/Activity CRUD, Activity Type SPI and frontend registry with a "Generic" type (log only), attendee picker, copy event | EVT-1…7, PICK-1…4 | The plug-in architecture, the mobile-first create flow |
| **M4** | **Dining Out:** restaurants, reviews, dishes, restaurant history page | DIN-1…5, MEM-T3 | First real Activity Type plug-in. **Write the `new-activity-type` skill after this.** |
| **M5** | **Board Gaming:** hosts, games, sessions, placements, win chart | BG-1…6 | Second plug-in (built with the skill), charts |
| **M6** | **Timeline and search:** filters, full-text search | MEM-T1…2 | Postgres full-text search, list UX |
| **M7** | **AI-1 board game lookup:** Spring AI, tool calling, structured output, caching, budget, eval set | AI-0a…d, AI-1, NFR-10 | Applied AI engineering |
| **M8** | **Ship it:** hosting, seed demo group, Playwright smoke tests, README with screenshots, architecture diagram, and a "How I built this with Claude Code" section | NFR-* | Deployment, telling the portfolio story |

**Suggested order of adding Claude Code features:** `CLAUDE.md` (M0) → plan mode on every milestone → `code-reviewer` subagent (M2) → `spec-feature` skill (M3) → `new-activity-type` skill (after M4) → hooks for formatting and tests (M5) → `test-writer` subagent (M6).

## Phase 2

Photos (PHO-1) · alerts (SOC-1…3) · Movies (MOV-1…3) · AI-2 restaurant lookup · AI-3 recommendations · AI-4 movie lookup · AI-7 game suggestions · email invites (INV-4) · "On this day" (MEM-T4) · structured food preferences (MEM-6) · per-type permissions (ROL-3) · account deletion (ACC-3) · history import (if needed) · **SpecKit experiment on the Movies feature**

## Phase 3

AI-5 showtimes · AI-6 event timing · SMS invites · cloud photo links · data export (MEM-T5) · React Native/Expo mobile app · app store release preparation (privacy policy, terms, data licensing review)

## Definition of done (every milestone)

- Acceptance criteria covered by tests that reference requirement IDs
- CI is green, including the Modulith `verify()` check
- Reviewed by you (and by the reviewer subagent once it exists)
- `requirements.md` statuses and any ADRs updated
- Usable at a 360 px phone width

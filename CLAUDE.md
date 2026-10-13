# CrewUp

A shared memory log for long-running friend groups: plan get-togethers, log activities (dining, board games, movies), and look back on them. It's a portfolio project and a learning project, so favor clarity and tests over cleverness.

## Read first
- `docs/project-log.md` — decisions and context from earlier sessions; read before starting work.
- `docs/requirements.md` contains requirement IDs (such as EVT-5). Every feature traces to IDs.
- `docs/architecture.md` explains modules, the Activity Type plug-in contract, and security.
- `docs/roadmap.md` lists milestones and the definition of done.
- Before implementing a feature, read its `docs/specs/<feature>/spec.md`, `plan.md`, and `tasks.md`. If they don't exist, stop and propose creating them.

## Principles (the project's constitution)

1. **Spec first.** Don't implement anything without a spec and an approved plan. If the spec is ambiguous, ask instead of guessing.
2. **Module boundaries are law.** Activity Type modules (`dining`, `boardgames`, `movies`) never import each other. They use only the published APIs of `events` and `groups`, plus application events.
3. **Group-scoped everything.** Every group resource is under `/api/groups/{groupId}`, and every endpoint checks group permissions.
4. **AI proposes, the user confirms.** AI never writes data without confirmation, and every AI feature has a manual fallback.
5. **Tests prove requirements.** Test names reference requirement IDs. Write or update tests in the same change as the code.
6. **Small steps.** One task from `tasks.md` at a time: implement, test, commit.

## Stack

Backend: Java 25, Spring Boot, Spring Modulith, Spring Data JPA, Flyway, PostgreSQL, Spring Security (OIDC resource server), Spring AI (Claude by default; local LLM via Ollama as a configurable alternative).
Frontend: React, TypeScript, Vite, React Router, TanStack Query, Tailwind CSS + shadcn/ui, React Hook Form + Zod, Recharts.
Testing: JUnit 5, Testcontainers, Modulith tests, Vitest, React Testing Library, MSW, Playwright.

## Commands

<!-- Fill these in during Milestone 0 -->
- Start dependencies: `docker compose up -d`
- Backend (Maven, via the wrapper): `./mvnw spring-boot:run` · test: `./mvnw test` · full build with integration tests: `./mvnw verify`
- Frontend: `npm run dev` · test: `npm test` · lint: `npm run lint`
- Regenerate API client: `npm run gen:api`
- End-to-end tests: `npm run e2e`

## Conventions

- Java packages: `app.crewup.<module>`. Module internals go in `internal` subpackages. Expose only what other modules need.
- Activity Type tables are prefixed with the module name (`dining_`, `boardgames_`, `movies_`). One Flyway folder per module.
- IDs are UUID v7. Store instants as `timestamptz`, with the IANA zone on Event.
- Errors use RFC 9457 Problem Details.
- Frontend: new Activity Types go in `src/activity-types/<type>/` and register in `registry.ts`. Server state uses TanStack Query only.
- Commits: Conventional Commits (`feat(dining): add restaurant reviews [DIN-3]`).

## Don't

- Don't add dependencies without saying why in the plan.
- Don't edit generated code (API client, migrations already applied).
- Don't commit secrets. Use `.env` files (gitignored) and `application-local.yml`.

# CrewUp

A shared memory log for long-running friend groups: plan get-togethers, log activities (dining, board games, movies), and look back on them. It's a portfolio project and a learning project, so favor clarity and tests over cleverness.

## Read first
- `docs/project-log.md` — decisions and context from earlier sessions; read before starting work.
- `docs/requirements.md` contains requirement IDs (such as EVT-5). Every feature traces to IDs.
- `docs/architecture.md` explains modules, the Activity Type plug-in contract, and security.
- `docs/roadmap.md` lists milestones and the definition of done.
- `docs/coding-standards.md` — the coding rules, with IDs such as JAVA-3. Follow them in all code you write, and cite the rule ID when you deliberately break one.
- Before implementing a feature, read its `docs/specs/<feature>/spec.md`, `plan.md`, and `tasks.md`. If they don't exist, stop and propose creating them.

## Principles (the project's constitution)

1. **Spec first.** Don't implement anything without a spec and an approved plan. If the spec is ambiguous, ask instead of guessing.
2. **Module boundaries are law.** Activity Type modules (`dining`, `boardgames`, `movies`) never import each other. They use only the published APIs of `events` and `groups`, plus application events.
3. **Group-scoped everything.** Every group resource is under `/api/groups/{groupId}`, and every endpoint checks group permissions.
4. **AI proposes, the user confirms.** AI never writes data without confirmation, and every AI feature has a manual fallback.
5. **Test first for logic and APIs.** For domain rules, endpoints, and bug fixes: write tests from the acceptance criteria, run them to confirm they fail, commit, then implement. Never modify a committed test to make it pass; ask instead. Setup, configuration, and visual styling are exempt. Test names reference requirement IDs.
6. **Small steps.** One task from `tasks.md` at a time. Test-first tasks: failing tests → commit → implement until green → commit. Exempt tasks: implement, test, commit.
7. **Coverage is a floor, not a goal.** CI enforces thresholds (JaCoCo, Vitest). Never write a test just to raise coverage: every test asserts behavior from the acceptance criteria. Don't lower thresholds or add exclusions without asking. If the check fails, follow TEST-12 in `docs/coding-standards.md`: one pass at testing untested criteria and deleting dead code, then stop and report.
8. **Standards are checked, not hoped for.** Code follows `docs/coding-standards.md`. Run the formatters before committing (`./mvnw spotless:apply`, `npm run format`), and run `/review-standards` before opening a pull request.

## Stack

Backend: Java 25, Spring Boot, Spring Modulith, Spring Data JPA, Flyway, PostgreSQL, Spring Security (OIDC resource server), Spring AI (Claude by default; local LLM via Ollama as a configurable alternative).
Frontend: React, TypeScript, Vite, React Router, TanStack Query, Tailwind CSS + shadcn/ui, React Hook Form + Zod, Recharts.
Testing: JUnit 5, Testcontainers, Modulith tests, Vitest, React Testing Library, MSW, Playwright.

## Commands

<!-- Fill these in during Milestone 0 -->
- Start dependencies (repo root): `docker compose up -d` (first time: `cp .env.example .env`)
- Backend (in `backend/`, Maven via the wrapper): `./mvnw spring-boot:run` · test: `./mvnw test` · full build with integration tests, coverage and formatting check: `./mvnw verify`
- Backend formatting (in `backend/`): `./mvnw spotless:apply` (fix) · `./mvnw spotless:check` (check; also runs in `verify`)
- Update the API contract `api/openapi.json` after an API change (in `backend/`): `./mvnw test -Dtest=OpenApiContractTest -Dopenapi.update=true`
- Frontend (all npm commands in `frontend/`): `npm run dev` · test: `npm test` · lint: `npm run lint` · format: `npm run format` (check: `npm run format:check`) · type check: `npm run typecheck` · coverage: `npm run test:coverage`
- Regenerate API client (in `frontend/`): `npm run gen:api` (reads `../api/openapi.json`, so update the contract first)
- End-to-end tests (in `frontend/`): `npm run e2e` (needs Compose and the backend running)
- Semgrep (repo root, Docker): `docker run --rm -v "$PWD:/src" -w /src semgrep/semgrep:1.182.0 semgrep scan --metrics=off --config .semgrep --error` · rule tests: `docker run --rm -v "$PWD/.semgrep:/rules:ro" semgrep/semgrep:1.182.0 semgrep --test --metrics=off /rules`
- Standards review before a PR: `/review-standards`

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

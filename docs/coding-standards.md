# CrewUp — Coding Standards

**Status:** v1 · October 14, 2026

These are the rules for all code in CrewUp, whether a person or Claude Code writes it. Each rule has an ID (such as `JAVA-3`) so reviews can point to it.

## How the rules are applied

- **When writing code:** `CLAUDE.md` tells Claude Code to follow this document. People follow it too.
- **Automatically:** Rules marked with a tool are checked by that tool locally and in CI. A violation fails the build. Formatting is never argued about in review; the formatter decides.
- **In review:** Before opening a pull request, run the `/review-standards` skill in Claude Code. It reviews the branch's changes against the rules marked *Review* and reports each finding with its rule ID. The pull request template asks for confirmation that it was run and its findings were resolved.

| Enforced by | What it checks | Run locally |
|---|---|---|
| **Spotless** (Palantir Java Format) | Java formatting and import order | `./mvnw spotless:apply` fixes, `./mvnw spotless:check` checks |
| **ESLint** | TypeScript and React code rules, React hooks rules, accessibility | `npm run lint` |
| **Prettier** | TypeScript, CSS, JSON and Markdown formatting | `npm run format` |
| **TypeScript compiler** (`strict`) | Type errors | `npm run typecheck` |
| **Semgrep** (project rules in `.semgrep/`) | Security and project-specific patterns | `semgrep --config .semgrep` |
| **Spring Modulith** `verify()` | Module boundaries | `./mvnw test` |
| **Review** | Everything that needs judgment | `/review-standards` in Claude Code |

**Exceptions.** If a rule is wrong for a specific case, break it on purpose and say why in a comment next to the code, citing the rule ID. If a rule is wrong in general, change this document in its own pull request.

---

## General (GEN)

- **GEN-1** Names say what something is or does, in the language of the domain: `Member`, `Activity`, `copyEvent`, not `data`, `item`, `handle`. No abbreviations except well-known ones (`id`, `url`, `dto`). *Review*
- **GEN-2** A function does one thing. If it needs a comment to separate its sections, split it. *Review*
- **GEN-3** Comments explain **why**, not what. Don't restate the code. Public APIs of a module get a short doc comment. *Review*
- **GEN-4** No dead code, commented-out code, or unused parameters. Git history keeps old versions. *ESLint, Review*
- **GEN-5** No `TODO` without a linked GitHub issue: `// TODO(#42): …`. *Review*
- **GEN-6** No secrets, tokens or passwords in code, tests or config. Use `.env` / `application-local.yml`. *gitleaks*
- **GEN-7** Never log tokens, passwords, or personal data (email, phone, food preferences). Log IDs instead. *Review*
- **GEN-8** Prefer clear code over clever code. This is a learning and portfolio project: a reader should follow it without a debugger. *Review*
- **GEN-9** Don't add a dependency without saying why in the plan (`CLAUDE.md`). *Review*

## Java and Spring (JAVA)

- **JAVA-1** Formatting is Palantir Java Format: 4-space indent, 120-column lines. Run `./mvnw spotless:apply` before committing. *Spotless*
- **JAVA-2** Packages are `app.crewup.<module>`. A module's internals live in `app.crewup.<module>.internal`. Only what other modules need is public in the module's top-level package. *Modulith*
- **JAVA-3** Use constructor injection with `final` fields. No `@Autowired` on fields or setters. *Semgrep*
- **JAVA-4** Use `record`s for DTOs, commands, events and value objects. Don't use Lombok; records and the IDE cover what it would do.
- **JAVA-5** JPA entities never leave their module and never appear in API responses. Controllers return DTO records, mapped explicitly (for example, `UserProfile.from(user)`). *Review*
- **JAVA-6** Controllers are thin: they validate input, check access, call one service method, and map the result. Business logic lives in services or the domain model. *Review*
- **JAVA-7** `@Transactional` goes on service methods, not controllers or repositories. Read-only methods use `@Transactional(readOnly = true)`. *Review*
- **JAVA-8** Use `Optional` only as a return type, never for fields or parameters. Never return `null` for a collection; return an empty one. *Review*
- **JAVA-9** Use `java.time` (`Instant`, `ZonedDateTime`, `ZoneId`). Never `java.util.Date`, `Calendar` or `java.sql.Timestamp` in application code. *Semgrep*
- **JAVA-10** Validate request DTOs with Jakarta Bean Validation (`@NotBlank`, `@Size`, `@Valid`). The backend validates everything, even if the frontend already did. *Review*
- **JAVA-11** Don't catch exceptions just to log and rethrow them. Domain errors are specific exceptions, turned into Problem Details in one `@RestControllerAdvice`. *Review*
- **JAVA-12** Log with SLF4J placeholders (`log.info("Created event {}", eventId)`), not string concatenation. *Semgrep*
- **JAVA-13** Queries use bound parameters: Spring Data method names, `@Query` with `:named` parameters, or the Criteria API. Never build SQL or JPQL by concatenation or formatting. *Semgrep (M0-16)*
- **JAVA-14** IDs are UUID v7, created through the shared ID generator, never `UUID.randomUUID()` for entity IDs. *Semgrep*

## Modules (MOD)

- **MOD-1** Activity Type modules (`dining`, `boardgames`, `movies`) never import each other. *Modulith*
- **MOD-2** Activity Type modules use only the published APIs of `events` and `groups`, plus application events. *Modulith*
- **MOD-3** When one module needs to react to something in another, publish an application event (a `record` named in the past tense, such as `ActivityCopied`) instead of calling it directly. *Review*
- **MOD-4** Each module owns its tables. No module reads or writes another module's tables, even with native SQL. *Review*

## REST API (API)

- **API-1** Every group resource lives under `/api/groups/{groupId}/…`. *Review*
- **API-2** Every group endpoint checks permissions with `@PreAuthorize` (from M2, also checked by the guard test). *Review, test*
- **API-3** Paths use plural nouns in kebab-case (`/board-game-sessions`). JSON fields use camelCase. *Review*
- **API-4** Errors use RFC 9457 Problem Details, with a stable `type` URI per error kind. *Review*
- **API-5** Status codes: `201` with a `Location` header on create, `204` on delete, `400` invalid input, `401` not signed in, `403` not allowed, `404` not found (also for resources in groups the caller can't see, so they don't leak that it exists), `409`/`412` for edit conflicts. *Review*
- **API-6** Instants are ISO-8601 in UTC (`2026-10-14T18:30:00Z`). An event's time zone is sent separately as an IANA name. *Review*
- **API-7** Lists use cursor pagination. Updates use optimistic locking (`ETag` / `If-Match`). *Review*
- **API-8** Every endpoint and DTO is documented in the OpenAPI output (summary, response codes), because the frontend client is generated from it. *Review*

## Database and migrations (DB)

- **DB-1** Each module has its own Flyway folder. Scripts are named `V<number>__<what_it_does>.sql`. *Review*
- **DB-2** Never edit a migration that has been merged to `main`. Fix it with a new migration. *Review*
- **DB-3** Table and column names are `snake_case`. Activity Type tables are prefixed with the module name (`dining_restaurant`). *Review*
- **DB-4** Primary keys are `uuid` (v7). Instants are `timestamptz`. *Review*
- **DB-5** Every foreign key has an index. Add `NOT NULL`, unique and check constraints wherever the domain allows; the database is the last line of defense. *Review*

## TypeScript and React (TS)

- **TS-1** Formatting is Prettier with project defaults. *Prettier*
- **TS-2** TypeScript `strict` mode is on. No `any`; use `unknown` and narrow it. No `!` non-null assertions outside tests. *TypeScript, ESLint*
- **TS-3** Components are function components. File names: `PascalCase.tsx` for components, `useCamelCase.ts` for hooks, `camelCase.ts` for everything else. *Review*
- **TS-4** Follow the rules of hooks, with complete dependency arrays. *ESLint (react-hooks)*
- **TS-5** Server state goes through TanStack Query only, never `useEffect` + `fetch`, and never copied into `useState`. Each feature defines its query keys in one place. *Review*
- **TS-6** API calls use the generated client only. Never edit generated code. *Review*
- **TS-7** Forms use React Hook Form with a Zod schema. Types come from the schema (`z.infer`), not a separate interface. *Review*
- **TS-8** Use shadcn/ui components before writing custom ones. Style with Tailwind classes; no inline `style` and no separate CSS files except global styles. *Review*
- **TS-9** Accessibility (WCAG 2.2 AA): every input has a label, every icon-only button has an accessible name, everything works by keyboard, and color is never the only signal. *ESLint (jsx-a11y), Review*
- **TS-10** No `dangerouslySetInnerHTML`. *Semgrep (M0-16)*
- **TS-11** Activity Types live in `src/activity-types/<type>/` and register in `registry.ts`. Core screens never import from a specific Activity Type. *Review*
- **TS-12** Layouts are mobile-first: design for 360 px first, then add `md:` / `lg:` styles. *Review*

## Tests (TEST)

- **TEST-1** Test names reference the criterion or requirement they prove: `M0_4_unauthenticatedApiCallReturns401`, `EVT5_copyShiftsAllActivityTimes`. *Review*
- **TEST-2** Code and its tests change in the same commit. A bug fix includes a test that failed before the fix. *Review*
- **TEST-3** One behavior per test, laid out as arrange, act, assert. *Review*
- **TEST-4** Backend integration tests use Testcontainers PostgreSQL, never H2 or another in-memory stand-in. *Review*
- **TEST-5** React tests find elements the way a user would: `getByRole`, `getByLabelText`, then `getByText`. `getByTestId` only as a last resort. *ESLint (testing-library), Review*
- **TEST-6** Frontend tests mock the network with MSW, not by mocking the API client module. *Review*
- **TEST-7** No `Thread.sleep` or fixed timeouts. Wait for a condition (Awaitility, `findBy…`, Playwright auto-waiting). *Review*
- **TEST-8** Tests don't depend on each other or on run order, and they clean up their own data. *Review*

## AI features (AI)

- **AI-1** Feature code uses Spring AI's `ChatClient` only, never a provider-specific class. *Review*
- **AI-2** AI never writes data. It returns a proposal the user confirms. Every AI feature has a manual alternative. *Review*
- **AI-3** Prompts live in resource files (`src/main/resources/prompts/`), not inline strings. *Review*
- **AI-4** Send the model only the data it needs. No contact details or other personal data. *Review*

## Git and pull requests (GIT)

- **GIT-1** Commits follow Conventional Commits with requirement IDs: `feat(dining): add restaurant reviews [DIN-3]`. *Review*
- **GIT-2** One task from `tasks.md` per commit: implement, test, commit. *Review*
- **GIT-3** A pull request covers one spec (or part of one), passes CI, and has had `/review-standards` run with no unresolved findings. *PR template*

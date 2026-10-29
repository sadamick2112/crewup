# M1 — Crews and members: Tasks

**Status:** Approved · October 29, 2026 · From `plan.md` §7

One task at a time (CLAUDE.md principles 5–7). Check a task off in its last commit, together with its `project-log.md` entry (GIT-2). "Done when" is what must be true before the final commit; plan sections are in brackets.

- **Test-first tasks** (marked 🔴→🟢): T1–T14. **Red:** write the listed tests, run them, confirm they fail on assertions (stubs allowed so they compile), and commit. **Green:** implement until they pass, and commit. Never change a red-commit test to make it pass; ask instead (TEST-11).
- **Exempt tasks** (test-only, docs, review): T15–T17. Implement, test, and commit once.
- **Every final commit:** `./mvnw verify` and/or `npm run test:coverage`, whichever side the task touched, at or above the coverage limits (TEST-12 if not). Backend tasks also update `api/openapi.json` and regenerate the client (plan §4.7), and run `./mvnw spotless:apply`; frontend tasks run `npm run format`, `lint` and `typecheck`.

---

### T1 · Shared API plumbing 🔴→🟢 [plan §4.4]
**Red**
- [ ] Test-only probe endpoints (like M0's `ApiProbeController`) that validate a request record, throw each `ApiProblemException` subclass, and return a `CursorPage`.
- [ ] Tests: `M1_1_invalidRequestReturns400WithFieldPaths` (nested and list fields), `M1_5_missingIfMatchReturns428`, `M1_5_staleVersionReturns412`, `M1_5_concurrentSaveReturns412` (from `OptimisticLockingFailureException`), `notFoundReturns404WithTypeAndTraceId`; unit tests for `ETags` (format, missing, unparsable) and `Cursors` (round trip, tampered cursor → `400`, `limit` above 100 → `400`). Stubs that throw.
- [ ] Run: all fail on assertions. **Commit:** `test(shared): add failing tests for API errors, ETags and cursors [M1-1, M1-5, M1-11]`

**Green**
- [ ] `ApiProblemException` and its subclasses, `ApiExceptionHandler`, `ETags`, `CursorPage`, `Cursors`.

**Done when:** the tests pass and `./mvnw verify` is green. **Commit:** `feat(shared): add API error handling, ETags and cursor pages [M1-1, M1-5, M1-11]`

### T2 · The caller's User id 🔴→🟢 [plan §4.1]
**Red**
- [ ] Tests: `M1_2_currentUserIdProvisionsOnFirstUse`, `M1_2_currentUserIdMatchesGetMe`. Stub: the `Users` interface, with an implementation that throws.
- [ ] **Commit:** `test(identity): add failing tests for the Users API [M1-2]`

**Green**
- [ ] `app.crewup.identity.Users`, implemented by `UserProvisioningService`.

**Done when:** tests pass, `M0_8_moduleBoundariesAreRespected` still passes. **Commit:** `feat(identity): publish the caller's user id to other modules [M1-2]`

### T3 · Create, list and view crews 🔴→🟢 [plan §3, §4.2, §4.3, §4.5, §4.6]
**Red**
- [ ] `CrewFixtures` test helper (users, crews and members per test, removed afterwards).
- [ ] Tests from the plan §6 rows M1-1 (API and validator units), M1-2, M1-3, M1-4, M1-16 (`isMember` part: non-member, unknown and deleted crew all `404` with the same body; `M1_16_everyCrewEndpointHasPreAuthorize`), M1-17. Stubs: the DTOs and controller methods that throw. The migration `V20261030100000__crews_create_crews_and_members.sql` is setup and goes in this red commit, so the tests fail on assertions rather than on a missing table.
- [ ] **Commit:** `test(crews): add failing tests for creating and viewing crews [GRP-1, ACC-2, M1-16, M1-17]`

**Green**
- [ ] `Crew`, `Member` (core fields), repositories, `YearMonthConverter`, `CrewIcon`, `MemberRole`, `MemberStatus`, `CrewService` (create, my crews, view), `CrewController`, the validators, `CrewAccess.isMember`, `CallerMemberships`, `@EnableMethodSecurity`.
- [ ] If the 404 from `CrewAccess` arrives wrapped by Spring's expression evaluation, unwrap it in `ApiExceptionHandler` and note it in the log entry (plan §4.2).

**Done when:** tests pass; contract and client updated. **Commit:** `feat(crews): create, list and view crews [GRP-1, ACC-2]`

### T4 · Edit and delete a crew 🔴→🟢 [plan §4.2, §4.3, §4.6]
**Red**
- [ ] Tests from plan §6 rows M1-5 (API) and M1-6 (API), and the 403 cases of M1-16 for these endpoints.
- [ ] **Commit:** `test(crews): add failing tests for editing and deleting crews [GRP-2, GRP-3]`

**Green**
- [ ] `CrewAccess.isAdministrator`, `PUT` with `If-Match`, `DELETE` with `confirmName`, `ConfirmationMismatchException`.

**Done when:** tests pass; contract and client updated. **Commit:** `feat(crews): edit and delete crews [GRP-2, GRP-3]`

### T5 · Permanent deletion after 30 days 🔴→🟢 [plan §4.6]
**Red**
- [ ] Tests: `M1_7_GRP3_purgeDeletesCrewsOlderThan30DaysWithMembers`, `M1_7_GRP3_purgeKeepsRecentlyDeletedCrews` (calling the job's method directly, with `deleted_at` set through JDBC). Stub job that does nothing.
- [ ] **Commit:** `test(crews): add failing tests for purging deleted crews [GRP-3]`

**Green**
- [ ] `CrewPurgeJob`, scheduling config, `Deleted crews purged` log line.

**Done when:** tests pass. **Commit:** `feat(crews): purge crews 30 days after deletion [GRP-3]`

### T6 · Add, list and view members 🔴→🟢 [plan §3, §4.3, §4.5]
**Red**
- [ ] Tests from plan §6 rows M1-9 (API), M1-10 (API and validator units), M1-11 (API), M1-12 (API), M1-14, M1-15 (viewing).
- [ ] **Commit:** `test(crews): add failing tests for adding and viewing members [MEM-1, MEM-2, MEM-4, MEM-5]`

**Green**
- [ ] Migration `V20261030110000__crews_create_member_lists.sql`; the rest of the `Member` fields, `SocialLink`, `Favorite`, `Birthday`; `MemberService`, `MemberController`; contact hiding in the mapper; keyset query for the list.

**Done when:** tests pass; contract and client updated. **Commit:** `feat(crews): add, list and view members [MEM-1, MEM-2, MEM-4, MEM-5]`

### T7 · Edit a member 🔴→🟢 [plan §4.2, §4.3]
**Red**
- [ ] Tests from plan §6 rows M1-8, M1-13 (API), M1-15 (editing an inactive member).
- [ ] **Commit:** `test(crews): add failing tests for editing members [MEM-3, GRP-4]`

**Green**
- [ ] `CrewAccess.canEditMember`, `PUT` with `If-Match`.

**Done when:** tests pass; contract and client updated. **Commit:** `feat(crews): edit member profiles [MEM-3, GRP-4]`

### T8 · Frontend plumbing 🔴→🟢 [plan §2, §5.4, §5.5]
**Red**
- [ ] Add `react-hook-form`, `zod` and `@hookform/resolvers` at the plan §2 versions (setup).
- [ ] `src/test/renderWithProviders.tsx` (test helper).
- [ ] Tests: `M1_22_apiErrorCarriesProblemDetails` (and none for a non-JSON error), `applyServerErrors` unit tests (nested and list paths, unknown field, `412`). Stubs that throw.
- [ ] **Commit:** `test(frontend): add failing tests for server field errors [M1-22]`

**Green**
- [ ] `ApiError.problem`, `applyServerErrors`.

**Done when:** tests pass, `npm run test:coverage` green. **Commit:** `feat(frontend): carry Problem Details to forms [M1-22]`

### T9 · Routes, My crews, not found 🔴→🟢 [plan §5.1, §5.6]
**M0 test change** (approved with the plan, §5.6)
- [ ] Render the M0 `HomePage` tests through `renderWithProviders` at `/`, with a `GET /api/crews` handler; open the user menu before "Sign out" (unit and e2e). Names and assertions unchanged. **Commit:** `test(frontend): render M0 home tests through the app routes [M0-7, M0-25, M0-28]`

**Red**
- [ ] Tests from plan §6 rows M1-3 (component), M1-24, and `M1_21_pageShowsReferenceOnApiError` and axe for My crews and Not found. Stubs: page components that render nothing, and `routes.tsx`.
- [ ] **Commit:** `test(frontend): add failing tests for My crews and not found [ACC-2, M1-24]`

**Green**
- [ ] `routes.tsx`, `WelcomePage`, `MyCrewsPage` (with the greeting), `NotFoundPage`, `crewKeys`, `useMyCrews`; `main.tsx` uses the routes. shadcn `badge`.

**Done when:** tests pass, including the M0 ones. **Commit:** `feat(frontend): add routes, My crews and not-found pages [ACC-2, M1-24]`

### T10 · App shell and crew home 🔴→🟢 [plan §5.2]
**Red**
- [ ] Tests from plan §6 rows M1-19, M1-20, M1-23 (crew home), M1-25 (keyboard; axe for the crew home), and the M0 sign-out test now passing through the user menu.
- [ ] **Commit:** `test(frontend): add failing tests for the app shell [ACC-2, M1-19, M1-20]`

**Green**
- [ ] `AppShell`, `Header`, `CrewSwitcher`, `UserMenu`, `CrewNav`, `CrewLayout`, `CrewHomePage`, `useCrew`; shadcn `dropdown-menu`, `avatar`, `separator`.

**Done when:** tests pass; checked by hand at 360 px and 1280 px. **Commit:** `feat(frontend): add the app shell and crew home [ACC-2, M1-19, M1-20]`

### T11 · Create crew and crew settings 🔴→🟢 [plan §5.4]
**Red**
- [ ] Add `cmdk` through shadcn `command` (setup), plus `field`, `label`, `input`, `textarea`, `select`, `radio-group`, `popover`, `input-group`.
- [ ] Tests from plan §6 rows M1-1 (component), M1-5 (component), `crewSchema` unit tests, axe for both pages.
- [ ] **Commit:** `test(frontend): add failing tests for the crew forms [GRP-1, GRP-2, M1-22]`

**Green**
- [ ] `CrewForm`, `crewSchema`, `crewIcons`, `MonthYearField`, `TimeZoneField`, `CreateCrewPage`, `CrewSettingsPage`, mutations.

**Done when:** tests pass. **Commit:** `feat(frontend): create and edit crews [GRP-1, GRP-2]`

### T12 · Delete crew dialog 🔴→🟢 [spec §4.3]
**Red**
- [ ] Add shadcn `dialog` (setup). Tests from plan §6 row M1-6 (component), keyboard (Escape closes, focus returns), axe with the dialog open.
- [ ] **Commit:** `test(frontend): add failing tests for deleting a crew [GRP-3]`

**Green**
- [ ] `DeleteCrewDialog`, delete mutation, message on My crews.

**Done when:** tests pass. **Commit:** `feat(frontend): delete a crew with name confirmation [GRP-3]`

### T13 · Members list and profile 🔴→🟢 [spec §4.3]
**Red**
- [ ] Tests from plan §6 rows M1-11, M1-12, M1-14, M1-23 (component), axe for both pages.
- [ ] **Commit:** `test(frontend): add failing tests for the members list and profile [MEM-2, MEM-4, MEM-5]`

**Green**
- [ ] `memberKeys`, `useMembers` (infinite query), `useMember`, `MembersPage`, `MemberProfilePage`, the initials avatar.

**Done when:** tests pass. **Commit:** `feat(frontend): show members and profiles [MEM-2, MEM-4, MEM-5]`

### T14 · Add and edit member 🔴→🟢 [plan §5.4]
**Red**
- [ ] Tests from plan §6 rows M1-9, M1-13 (component), `memberSchema` unit tests, `ListField` tests, axe for the form.
- [ ] **Commit:** `test(frontend): add failing tests for the member form [MEM-1, MEM-2, MEM-3, M1-22]`

**Green**
- [ ] `MemberFormPage` (add and edit), `memberSchema`, `ListField`, mutations.

**Done when:** tests pass. **Commit:** `feat(frontend): add and edit members [MEM-1, MEM-2, MEM-3]`

### T15 · End-to-end tests [plan §6]
- [ ] `e2e/m1-26-golden-path.spec.ts`: the M1-26 journey with a unique crew name per run; deletes every crew it created, even on failure (TEST-8).
- [ ] Playwright axe at 360 px and desktop for My crews, Create crew, Crew home, Members, Member profile and Edit member (M1-25).

**Done when:** `npm run e2e` passes with the stack running. **Commit:** `test(e2e): add the M1 golden path and axe checks [M1-25, M1-26]`

### T16 · Documentation [spec §5]
- [ ] `requirements.md` §11: GRP-1 and MEM-2 partial (pictures), GRP-4 partial (M2), MEM-5 partial (M3, P2), NFR-7 partial; GRP-2, GRP-3, MEM-1, MEM-3, MEM-4, ACC-2 done. NFR-2 notes crew-scoped checks.
- [ ] `docs/adr/003-user-member-split.md` (Accepted); `architecture.md` §10 links it, and §6 describes `CrewAccess` as built.
- [ ] README: fix the branch-only gitleaks command, which scans 0 commits through Docker with `origin/main..HEAD` (project log, 2026-10-29); give a form that works, such as the two commit IDs.

**Commit:** `docs: record M1 statuses and ADR-003 [GRP-1, MEM-2]`

### T17 · Review and pull request [plan, Verification]
- [ ] Run `/review-standards`, then fix or justify the findings.
- [ ] Run the plan's Verification steps; open the PR to `main` from `feat/m1-crews` (you push with VS Code's Publish Branch).
- [ ] After the merge: M1 marked done in `roadmap.md`.

**Done when:** the PR is green and merged.

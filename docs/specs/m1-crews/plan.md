# M1 — Crews and members: Plan

**Status:** Approved · October 29, 2026 · Implements `spec.md` (Approved)

## Context

M0 left a walking skeleton: an `identity` module that provisions the User from the token, Problem Details for 401/403, request logging with trace IDs, a contract-tested OpenAPI document, and a one-page React app. M1 adds the first domain module, `crews` (Crew and Member), its crew-scoped endpoints with access checks, and the app shell with eight new screens.

This plan fixes the *how*: modules and their public APIs, tables, endpoints, access checks, the shared API plumbing that later milestones reuse (field errors, ETags, cursors), the frontend's routes and folders, new dependencies with reasons, and which test proves each criterion.

---

## 1. Layout (new and changed files)

```
backend/src/main/java/app/crewup/
├── identity/
│   ├── Users.java                  NEW public API: the caller's User id (provisions on first use)
│   └── internal/UserProvisioningService.java   implements Users
├── crews/                         NEW module (nothing public yet: no other module needs it in M1)
│   └── internal/
│       ├── Crew, CrewRepository, CrewService, CrewController, CrewIcon
│       ├── CrewPurgeJob           daily permanent deletion (M1-7)
│       ├── Member, MemberRepository, MemberService, MemberController
│       ├── MemberRole, MemberStatus, SocialLink, Favorite, Birthday
│       ├── CrewAccess             the @PreAuthorize bean "crewAccess" (M1-16)
│       ├── CallerMemberships       request-scoped cache of the caller's Member per crew
│       ├── YearMonthConverter      YearMonth <-> date (first of the month)
│       └── *Request / *Response    DTO records (JAVA-4, JAVA-5)
├── shared/
│   ├── ApiExceptionHandler.java    NEW the one @RestControllerAdvice (JAVA-11)
│   ├── ApiProblemException.java    NEW base for domain errors: status + stable type URI
│   ├── ETags.java                  NEW ETag / If-Match helpers (API-7)
│   └── CursorPage.java, Cursors.java  NEW cursor pagination (API-7)
└── SecurityConfig.java             + @EnableMethodSecurity
backend/src/main/resources/db/migration/crews/
├── V20261030100000__crews_create_crews_and_members.sql
└── V20261030110000__crews_create_member_lists.sql

frontend/src/
├── app/
│   ├── routes.tsx                  NEW all routes in one place
│   └── shell/                      NEW AppShell, Header, CrewSwitcher, UserMenu, CrewNav
├── core/
│   ├── home/                       WelcomePage (signed out) and MyCrewsPage (signed in, keeps the greeting)
│   ├── crews/                     crewKeys + hooks, CreateCrewPage, CrewHomePage, CrewSettingsPage,
│   │                               DeleteCrewDialog, CrewForm, crewSchema.ts, crewIcons.ts
│   └── members/                    memberKeys + hooks, MembersPage, MemberProfilePage, MemberFormPage,
│                                   memberSchema.ts
├── shared/
│   ├── api/client.ts               ApiError also carries the Problem Details body
│   ├── forms/                      NEW MonthYearField, TimeZoneField, ListField, applyServerErrors.ts
│   └── errors/NotFoundPage.tsx     NEW
└── test/renderWithProviders.tsx    NEW router + query cache + API client + auth, for page tests
```

## 2. Versions and new dependencies (GEN-9)

Checked October 29, 2026 with npm's per-version downloads for the last week (version rule, M0 plan §2).

**Backend: none.** Method security and scheduling are in the starters M0 already has.

**Frontend runtime:**

| Package | Version | Share of downloads | Why |
|---|---|---|---|
| `react-hook-form` | 7.91.0 | 7.x: 99% | Form state and validation timing (TS-7, already in the planned stack). It keeps typing fast by not re-rendering the whole form on each key. |
| `zod` | 4.6.7 | 4.x: 66%, 3.x: 34% | One schema gives both the form's validation and its TypeScript type (`z.infer`, TS-7). |
| `@hookform/resolvers` | 5.9.2 | 5.x: 66%, 6.x: 3% | The adapter that lets React Hook Form validate with a Zod schema. 6.0 is 10 days old, so 5.9.2 (supports `zod` ^4 and `react-hook-form` ^7.55). |
| `cmdk` | 1.1.1 | 1.x: 98% | Brought in by shadcn's `command` component, for the searchable time-zone list (418 zones). Built on the same Radix packages as the rest of shadcn. shadcn's newer `combobox` was considered: it brings a second primitives library (`@base-ui/react`). |

None of these are deprecated. **shadcn/ui components added** (copied source, as in M0; their only package is `cn`, already installed): `dropdown-menu` (switcher and user menu), `avatar`, `separator`, `badge`, `dialog`, `field`, `label`, `input`, `textarea`, `select`, `radio-group`, `popover`, `command`, `input-group`.

**Crew icons** (`CrewIcon`, 12, all in `lucide-react`): `USERS` (Users, the default), `DICE` (Dices), `DINING` (UtensilsCrossed), `FILM`, `BEER`, `GAMEPAD` (Gamepad2), `TROPHY`, `MUSIC`, `MOUNTAIN`, `TENT`, `PIZZA`, `COFFEE`.

## 3. Database (DB-1…DB-5)

Text limits are checked by Bean Validation (§4.5), not the database, so a limit change needs no migration. **Audit authors** (`created_by`, `updated_by`) hold the User id (spec decision 13) and have **no foreign key**: they must survive account deletion (ACC-3, P2), while `members.user_id` is a real link and has one.

### `V20261030100000__crews_create_crews_and_members.sql` (T3)

**`crews`**

| Column | Type | Null | Notes |
|---|---|---|---|
| `id` | `uuid` | no | Primary key, UUID v7 from `Ids.newId()` |
| `name` | `text` | no | |
| `established_month` | `date` | no | Always the first of the month; check `extract(day from established_month) = 1` |
| `motto` | `text` | yes | |
| `description` | `text` | yes | |
| `icon_key` | `text` | no | Check: one of the 12 `CrewIcon` keys (§2) |
| `time_zone` | `text` | no | IANA name, validated by `@ValidTimeZone` |
| `version` | `bigint` | no | JPA `@Version`; the ETag (§4.4) |
| `created_at` | `timestamptz` | no | |
| `created_by` | `uuid` | no | User id, no foreign key |
| `updated_at` | `timestamptz` | no | |
| `updated_by` | `uuid` | no | User id, no foreign key |
| `deleted_at` | `timestamptz` | yes | Set by delete (M1-6); indexed for the purge job (M1-7) |

**`members`**

| Column | Type | Null | Notes |
|---|---|---|---|
| `id` | `uuid` | no | Primary key, UUID v7 |
| `crew_id` | `uuid` | no | References `crews` **on delete cascade** (the purge removes members with their crew); indexed |
| `user_id` | `uuid` | yes | References `users`; null for historical members; indexed |
| `role` | `text` | no | Check `ADMIN`, `FRIEND`, `NEW_GUY` |
| `first_name` | `text` | no | |
| `last_name` | `text` | yes | |
| `nickname` | `text` | yes | |
| `pronouns` | `text` | yes | |
| `joined_month` | `date` | yes | First of the month (check as on `established_month`) |
| `birth_month` | `smallint` | yes | 1–12 |
| `birth_day` | `smallint` | yes | 1–31; set together with `birth_month` |
| `birth_year` | `smallint` | yes | Only when month and day are set |
| `phone` | `text` | yes | Contact field (M1-14) |
| `email` | `text` | yes | Contact field (M1-14) |
| `food_preferences` | `text` | yes | Free text (MEM-6 structures it in P2) |
| `status` | `text` | no | Check `ACTIVE`, `INACTIVE`; default `ACTIVE` |
| `sort_name` | `text` | no | Generated: `lower(coalesce(nullif(nickname, ''), first_name))` (M1-11) |
| `sort_last_name` | `text` | no | Generated: `lower(coalesce(last_name, ''))` (M1-11) |
| `version` | `bigint` | no | JPA `@Version`; the ETag |
| `created_at` | `timestamptz` | no | |
| `created_by` | `uuid` | no | User id, no foreign key |
| `updated_at` | `timestamptz` | no | |
| `updated_by` | `uuid` | no | User id, no foreign key |

Table-level rules on `members`:

| Rule | Definition | Why |
|---|---|---|
| Check | `birth_month` and `birth_day` both null or both set | MEM-2: month and day required together |
| Check | `birth_year` null unless `birth_month` is set | A year alone isn't a birthday |
| Unique | `(crew_id, user_id)` where `user_id is not null` | One Member per User per crew |
| Index | `(crew_id, status, sort_name, sort_last_name, id)` | Keyset paging of the members list in its sort order (M1-11; `ACTIVE` sorts before `INACTIVE`) |

### `V20261030110000__crews_create_member_lists.sql` (T6)

**`member_social_links`** (primary key `(member_id, position)`)

| Column | Type | Null | Notes |
|---|---|---|---|
| `member_id` | `uuid` | no | References `members` on delete cascade |
| `position` | `int` | no | Order in the list, from 0 |
| `url` | `text` | no | `http` or `https` (`@HttpUrl`) |
| `label` | `text` | yes | |

**`member_favorites`** (primary key `(member_id, kind, position)`; the data model's `board_game_id` arrives with M5)

| Column | Type | Null | Notes |
|---|---|---|---|
| `member_id` | `uuid` | no | References `members` on delete cascade |
| `kind` | `text` | no | Check `MUSIC`, `VIDEO_GAME`, `BOARD_GAME` |
| `position` | `int` | no | Order within that kind's list, from 0 |
| `value` | `text` | no | |

## 4. Backend design

### 4.1 The caller's User (`identity` public API)

Every crew endpoint needs the caller's User id, and a crew request may come before any `GET /api/me`. `identity` publishes one interface in its base package:

```java
/** The signed-in caller's CrewUp User, created on first use (M0-6). */
public interface Users {
    UUID currentUserId(Jwt token);
}
```

`UserProvisioningService` implements it with the existing `currentUser(jwt)`, so the provisioning rules stay in one place. `crews` depends only on this interface (Modulith `verify()` checks it).

### 4.2 Access checks (M1-16)

- `SecurityConfig` gets `@EnableMethodSecurity`.
- **`CrewAccess`** (bean name `crewAccess`) is what `@PreAuthorize` calls:
  - `isMember(crewId)`: true for a member. **Throws `CrewNotFoundException` (404)** for a non-member, a deleted crew or an unknown id, so all three look the same (API-5).
  - `isAdministrator(crewId)`: the same 404 for non-members, otherwise true only for the Administrator role (false → Spring Security's `403`, through M0's `ProblemDetailsAccessDeniedHandler`).
  - `canEditMember(crewId, memberId)`: an Administrator, or the member whose `user_id` is the caller (MEM-3).
- **Why throw instead of returning false:** `false` always becomes `403`, which would tell a stranger the crew exists. T3's tests prove the exception reaches the client as `404`. If Spring's expression evaluation wraps it, T3 unwraps it in `ApiExceptionHandler` and records that in the log.
- **`CallerMemberships`** is request-scoped (one instance per HTTP request): it looks up the caller's Member (`id`, `role`) per crew once and reuses it for the access check and the service call.
- Services take the caller's membership as an argument; they never read the security context themselves.

### 4.3 Endpoints

All `/api/crews/**` handlers carry `@PreAuthorize`. Tags `Crews` and `Members` in OpenAPI.

| Method and path | Access | Result |
|---|---|---|
| `GET /api/crews?cursor=&limit=` | signed in | `CursorPage<CrewSummary>` (M1-3) |
| `POST /api/crews` | signed in | `201`, `Location`, `CrewDetails` (M1-1, M1-2) |
| `GET /api/crews/{crewId}` | `isMember` | `CrewDetails` + `ETag` (M1-4) |
| `PUT /api/crews/{crewId}` + `If-Match` | `isAdministrator` | `CrewDetails` + new `ETag` (M1-5) |
| `DELETE /api/crews/{crewId}?confirmName=` | `isAdministrator` | `204` (M1-6) |
| `GET /api/crews/{crewId}/members?cursor=&limit=` | `isMember` | `CursorPage<MemberSummary>` (M1-11) |
| `POST /api/crews/{crewId}/members` | `isAdministrator` | `201`, `Location`, `MemberProfile` (M1-9) |
| `GET /api/crews/{crewId}/members/{memberId}` | `isMember` | `MemberProfile` + `ETag` (M1-12) |
| `PUT /api/crews/{crewId}/members/{memberId}` + `If-Match` | `canEditMember` | `MemberProfile` + new `ETag` (M1-13) |

- **Delete confirmation travels as a query parameter** (spec decision 14). A `DELETE` body has no defined meaning in HTTP (RFC 9110) and some proxies drop it. The name isn't secret, and the request log records only the route pattern, never the query string (M0-26).
- **DTOs** (records, mapped with `from(...)`, JAVA-5): `CreateCrewRequest` (crew fields + `creatorFirstName`), `UpdateCrewRequest`, `CrewSummary` (id, name, iconKey, motto, establishedMonth, myRole), `CrewDetails` (all fields + `myMemberId`, `myRole`), `MemberRequest` (all M1-10 fields; no role, no user link, M1-8), `MemberSummary`, `MemberProfile`.
- **Month values** (`establishedMonth`, `joinedMonth`) are `"YYYY-MM"` strings (`java.time.YearMonth`); the birthday is `{ month, day, year? }`. Stored as dates on the first of the month through `YearMonthConverter`.
- **Contact fields (M1-14):** `MemberSummary` and `MemberProfile` mark `phone`, `email` and `birthday.year` `@JsonInclude(NON_NULL)` and optional in OpenAPI. The mapper sets them to null when the caller is a New Guy looking at someone else.
- **Sorting:** crews by `lower(name), id`; members by status (Active first), `sort_name`, `sort_last_name`, `id` (§3).

### 4.4 Shared API plumbing (`shared`, reused by every later module)

- **`ApiProblemException`** (abstract): status + type URI + detail. Subclasses: `NotFoundException` (404, `urn:crewup:problem:not-found`), `PreconditionFailedException` (412, `…:precondition-failed`), `PreconditionRequiredException` (428, `…:precondition-required`), and in `crews`, `ConfirmationMismatchException` (400, `…:confirmation-mismatch`). URIs follow M0's `urn:crewup:problem:*` form (API-4).
- **`ApiExceptionHandler`** (`@RestControllerAdvice`): turns those into Problem Details, plus `MethodArgumentNotValidException` and handler-method validation errors into `400` `urn:crewup:problem:invalid-request` with an `errors` array of `{ field, message }`. `field` is the JSON path of the request property (`socialLinks[0].url`). JPA's `OptimisticLockingFailureException` (two saves at the same moment) → `412`. The trace ID is already added by M0's `ProblemDetailsTraceIdAdvice`.
- **`ETags`**: `of(version)` gives `"<version>"` (a strong ETag, quotes included); `expectedVersion(ifMatchHeader)` throws `PreconditionRequiredException` when the header is missing, and returns `-1` (never matches) when it is unparsable. Services compare it with the entity's `@Version` and throw `PreconditionFailedException` when they differ.
- **`CursorPage<T>(List<T> items, String nextCursor)`** and **`Cursors`**: the cursor is the last item's sort key, Base64url-encoded JSON, opaque to clients. `limit` defaults to 50, max 100 (`400` above). Queries are keyset queries ("rows after this key"), so a page stays correct while rows are added. They use bound parameters only (JAVA-13).

### 4.5 Validation (JAVA-10, spec §3 *Field rules*)

Bean Validation on the request records: `@NotBlank`, `@Size`, `@Email`, `@Pattern` (phone `^[0-9 +()\-]{1,30}$`), `@Valid` on list elements, `@Size(max = 10)` on links and `max = 20` on each favorites list. Custom constraints, each with unit tests:
- `@MonthInRange` (`YearMonth` from 1900-01 to the current month),
- `@ValidTimeZone` (in `ZoneId.getAvailableZoneIds()`: region IDs only, so `+02:00` is refused),
- `@ValidBirthday` (class-level: month and day together, the day exists in that month, February 29 only without a year or in a leap year, year 1900 to now),
- `@HttpUrl` (parses as a URI with an `http`/`https` scheme and a host).

Strings are trimmed when the request is mapped, and blank optional strings become null.

### 4.6 Services and logging

- `CrewService.create` runs in one transaction: insert the Crew, then the creator's Member (Administrator, linked User, Active).
- `CrewService.delete` checks `confirmName` against the stored name (trimmed, otherwise exact), sets `deleted_at` and `updated_*`. Every query that loads a crew adds `deleted_at is null`.
- `CrewPurgeJob` (`@Scheduled(cron = "0 30 3 * * *", zone = "UTC")`, `@EnableScheduling` on a small config class) deletes crews with `deleted_at` older than 30 days in one statement; cascades remove their members. It is idempotent, so a second app instance (M8) running it too is harmless.
- One INFO line per change, IDs as key-values only (LOG-1, LOG-3): `Crew created`, `Crew updated`, `Crew deleted`, `Member added`, `Member updated` (`crewId`, `memberId`), `Deleted crews purged` (`count`).

### 4.7 OpenAPI (M1-18)

Each operation gets a summary and every response code (`200/201/204/400/401/403/404/412/428`) with the Problem Details schema (API-8). `If-Match` is a documented header parameter and `ETag` a documented response header, so the generated client types them. Every backend task ends by updating `api/openapi.json` (`-Dopenapi.update=true`) and running `npm run gen:api`, because `OpenApiContractTest` and the `api-client` CI job fail otherwise.

## 5. Frontend design

### 5.1 Routes

`src/app/routes.tsx` declares them all. React Router's **layout routes** are routes with no page of their own: they render the shell, and the matched child page appears inside it, where the layout puts an `<Outlet />` (like a servlet filter wrapping the request).

```
/                              HomeRoute: signed out → WelcomePage; signed in → AppShell > MyCrewsPage
/auth/callback                 AuthCallbackPage (M0)
AppShell (signed-in layout)
├── /crews/new                CreateCrewPage
└── /crews/:crewId           CrewLayout (loads the crew; 404 → NotFoundPage; adds CrewNav)
    ├── (index)                CrewHomePage
    ├── settings               CrewSettingsPage (+ DeleteCrewDialog)
    ├── members                MembersPage
    ├── members/new            MemberFormPage (add)
    ├── members/:memberId      MemberProfilePage
    └── members/:memberId/edit MemberFormPage (edit)
*                              NotFoundPage
```

A signed-out user who opens a deep link sees the Welcome page; after sign-in they land on `/`. Returning to the original URL arrives with invite links in M2, which need it.

### 5.2 The shell (spec §4.2)

- `AppShell`: header + `<Outlet />`. `CrewLayout` adds `CrewNav`: a fixed bottom bar below `md` (768 px), a left column from `md:` on (TS-12). Links use React Router's `NavLink`, which sets `aria-current="page"` on the current one, styled bold as well as colored (TS-9).
- `CrewSwitcher` and `UserMenu` are shadcn `DropdownMenu`s (Radix underneath), which give the keyboard behavior of M1-25 (arrow keys, Escape, focus returned to the button) and the screen-reader roles. The switcher reads the first page of `useMyCrews()`; "All crews" covers anyone in more than 50 crews.
- Avatar initials: the first letters of first and last name (or the first two letters of a single name), uppercase.

### 5.3 Server state (TS-5)

- `crewKeys` (`all`, `mine`, `detail(crewId)`) and `memberKeys` (`list(crewId)`, `detail(crewId, memberId)`), one file per feature.
- Lists use TanStack Query's **infinite query** (`useInfiniteQuery`: a query that holds several pages and fetches the next on request) for "Show more".
- `useCrew` and `useMember` return `{ data, etag }`, reading the `ETag` header from `openapi-fetch`'s response. The save sends it back in `If-Match`.
- Saves are **mutations** (`useMutation`: TanStack's wrapper for a call that changes data). On success they **invalidate** the affected keys, which marks the cached copies stale so they're fetched again. Create and delete also invalidate `crewKeys.mine`, so the switcher and My crews update.

### 5.4 Forms (TS-7, M1-22)

- One Zod schema per form (`crewSchema.ts`, `memberSchema.ts`) mirrors the field rules. The form's type comes from `z.infer`. Mode `onTouched`: an error appears when the user leaves a field, then updates as they type.
- **Server errors:** `ApiError` gains `problem` (the parsed Problem Details body, when the response is `application/problem+json`). `applyServerErrors(error, setError)` maps each `errors[].field` (`socialLinks[0].url`) onto the form's field name (`socialLinks.0.url`). Errors with no matching field, and a `412`, show above the Save button; the `412` one has a "Load latest" button that refetches.
- Shared field components in `src/shared/forms/`: `MonthYearField` (month `select` + year input; established and joined), `TimeZoneField` (shadcn **combobox pattern**, a button that opens a type-to-filter list: `Popover` + `Command`; default `Intl.DateTimeFormat().resolvedOptions().timeZone`), `ListField` (rows with a remove button and "Add", for links and favorites). The icon picker is a shadcn `RadioGroup` laid out as a 6×2 grid, each option labelled with the icon's name (TS-9).
- The creator's first name is prefilled with the first word of the display name from `useMe()`.

### 5.5 Testing helpers

`src/test/renderWithProviders.tsx` renders a page at a given URL inside a `MemoryRouter` (a router that keeps its URL in memory instead of the address bar), with a fresh query cache, the real API client and a fake auth context. Page tests use it with MSW handlers (TEST-6).

### 5.6 Changes to M0 tests (TEST-11; approved with this plan)

Spec decision 8 approved moving "Sign out" into the user menu. Two more consequences surfaced while planning:

1. Signed in, `/` now shows My crews, which calls `GET /api/crews` and renders links. The M0 tests in `HomePage.test.tsx` render `HomePage` with no router and only a `/api/me` handler, so they would fail on the missing router and on the unhandled request.
2. `HomePage` splits into `WelcomePage` and `MyCrewsPage`.

**Proposal:** one commit before T9's red commit, `test(frontend): render M0 home tests through the app routes [M0-7, M0-25, M0-28]`. It renders the same tests through `renderWithProviders` at `/`, adds a `GET /api/crews` handler returning an empty page, and opens the user menu before "Sign out". Test names and assertions stay as they are. The e2e sign-out step gets the same menu step.

## 6. Test map (criterion → test)

Backend tests are Testcontainers integration tests through MockMvc with `jwt()` (as in M0) unless marked unit. Each role test creates its users, crew and members through a `CrewFixtures` helper, which inserts the test's own rows and deletes them afterwards (TEST-8).

| Criterion | Tests | Level |
|---|---|---|
| M1-1 | `M1_1_GRP1_createsCrewAndReturnsLocation`, `M1_1_GRP1_invalidFieldsReturn400WithFieldErrors`; unit tests for `@MonthInRange`, `@ValidTimeZone`; `CreateCrewPage`: `M1_1_GRP1_createsCrewAndOpensItsHome`, `M1_1_GRP1_showsFieldErrorsBeforeSubmitting` | API + unit + component |
| M1-2 | `M1_2_GRP1_creatorBecomesActiveAdministrator` | API |
| M1-3 | `M1_3_ACC2_listsOnlyCallersCrewsWithRole`, `M1_3_ACC2_leavesOutDeletedCrews`, `M1_3_ACC2_pagesWithCursorInNameOrder`; `MyCrewsPage`: `M1_3_ACC2_showsCrewCards`, `M1_3_ACC2_showsEmptyState` | API + component |
| M1-4 | `M1_4_memberGetsCrewWithOwnRoleAndETag` | API |
| M1-5 | `M1_5_GRP2_administratorUpdatesCrew`, `M1_5_GRP2_staleIfMatchReturns412`, `M1_5_GRP2_missingIfMatchReturns428`, `M1_5_GRP2_nonAdministratorGets403`; `CrewSettingsPage`: `M1_5_GRP2_savesChanges`, `M1_5_GRP2_conflictOffersToLoadLatest` | API + component |
| M1-6 | `M1_6_GRP3_deleteHidesCrewFromEveryone`, `M1_6_GRP3_wrongConfirmationDeletesNothing`, `M1_6_GRP3_nonAdministratorGets403`; `DeleteCrewDialog`: `M1_6_GRP3_deleteEnabledOnlyWhenNameMatches`, `M1_6_GRP3_deletingOpensMyCrewsWithMessage` | API + component |
| M1-7 | `M1_7_GRP3_purgeDeletesCrewsOlderThan30DaysWithMembers`, `M1_7_GRP3_purgeKeepsRecentlyDeletedCrews` | Integration |
| M1-8 | `M1_8_GRP4_memberUpdateCannotChangeRoleOrAccount` | API |
| M1-9 | `M1_9_MEM1_administratorAddsHistoricalMember`, `M1_9_MEM1_nonAdministratorGets403`; `MemberFormPage`: `M1_9_MEM1_addsMemberAndOpensProfile` | API + component |
| M1-10 | `M1_10_MEM2_storesAndReturnsEveryProfileField`, `M1_10_MEM2_invalidFieldsReturn400WithFieldErrors`; unit tests for `@ValidBirthday`, `@HttpUrl`, phone pattern; `memberSchema` unit tests | API + unit |
| M1-11 | `M1_11_MEM2_listsActiveFirstThenAlphabetical`, `M1_11_MEM2_sortsByNicknameWhenPresent`, `M1_11_MEM2_pagesWithCursor`; `MembersPage`: `M1_11_MEM5_showsInactiveSectionLast`, `M1_11_showsMoreOnRequest` | API + component |
| M1-12 | `M1_12_MEM2_memberGetsFullProfileWithETag`; `MemberProfilePage`: `M1_12_MEM2_showsProfileSections` | API + component |
| M1-13 | `M1_13_MEM3_memberEditsOwnProfile`, `M1_13_MEM3_administratorEditsAnyProfile`, `M1_13_MEM3_friendCannotEditOthers`, `M1_13_MEM3_staleIfMatchReturns412`; `MemberFormPage`: `M1_13_MEM3_savesEditsAndOpensProfile` | API + component |
| M1-14 | `M1_14_MEM4_newGuyDoesNotSeeOthersContactFields` (list and profile), `M1_14_MEM4_friendAndAdministratorSeeContactFields`, `M1_14_MEM4_newGuySeesOwnContactFields`; `MemberProfilePage`: `M1_14_MEM4_hidesContactSectionWhenAbsent` | API + component |
| M1-15 | `M1_15_MEM5_inactiveMemberStaysViewableAndEditable` | API |
| M1-16 | `M1_16_nonMemberGets404LikeUnknownCrew` (for every endpoint, parameterized), `M1_16_deletedCrewReturns404`, `M1_16_everyCrewEndpointHasPreAuthorize` (reflection over the `crews` controllers; M2 generalizes it into the guard test) | API |
| M1-17 | `M1_17_recordsAuthorsAndTimes`, `M1_17_changesAreLoggedWithIdsOnly` (log capture, as M0-26) | API |
| M1-18 | `M0_9_openApiContractIsUpToDate` (existing), `api-client` CI job | Integration + CI |
| M1-19 | `AppShell`: `M1_19_showsHeaderAndCrewNav`, `M1_19_marksCurrentSection` | Component |
| M1-20 | `CrewSwitcher`: `M1_20_ACC2_listsCrewsAndMarksCurrent`, `M1_20_ACC2_pickingCrewOpensItsHome` | Component |
| M1-21 | Covered per page by the rows above, plus `M1_21_pageShowsReferenceOnApiError` per page | Component |
| M1-22 | `applyServerErrors` unit tests; `client.test.ts`: `M1_22_apiErrorCarriesProblemDetails`; the form rows above | Unit + component |
| M1-23 | `CrewHomePage`: `M1_23_editCrewOnlyForAdministrators`; `MembersPage`: `M1_23_addMemberOnlyForAdministrators`; `MemberProfilePage`: `M1_23_editOnlyForSelfOrAdministrators` | Component |
| M1-24 | `NotFoundPage` routes: `M1_24_unknownCrewShowsNotFound`, `M1_24_unknownMemberShowsNotFound` | Component |
| M1-25 | `M1_25_<page>HasNoAxeViolations` per page (TEST-10); `M1_25_switcherAndMenusWorkByKeyboard`; Playwright axe on the six pages at 360 px and desktop | Component + e2e |
| M1-26 | `m1-26-golden-path.spec.ts`: `M1_26_createCrewAddMemberSwitchAndDelete` | Playwright (local) |
| JAVA-14 | All new IDs from `Ids.newId()` (Semgrep rule, existing) | Semgrep |

## 7. Task outline (becomes `tasks.md`)

Backend first, so the generated client exists before the screens that use it.

1. Shared API plumbing: field errors, `ApiProblemException`, `ETags`, cursor pages (M1-1, M1-5, M1-11 mechanics).
2. `identity.Users` public API (M1-2, M1-16 groundwork).
3. Crews: migration, create, my crews, view, `CrewAccess.isMember`, method security (M1-1…M1-4, M1-16, M1-17).
4. Edit and delete a crew, `isAdministrator` (M1-5, M1-6).
5. Purge job (M1-7).
6. Members: list tables, add, list, view, contact hiding (M1-9…M1-12, M1-14, M1-15).
7. Edit a member, `canEditMember` (M1-8, M1-13, M1-14, M1-15).
8. Frontend plumbing: `ApiError.problem`, `applyServerErrors`, `renderWithProviders`, form dependencies (M1-22).
9. Routes, Welcome and My crews, not found; the approved M0 test change (M1-3, M1-21, M1-24).
10. App shell: header, switcher, user menu, crew nav, crew home (M1-19, M1-20, M1-23, M1-25).
11. Create crew and crew settings forms (M1-1, M1-5, M1-22).
12. Delete crew dialog (M1-6).
13. Members list and member profile (M1-11, M1-12, M1-14, M1-15, M1-23).
14. Add and edit member form (M1-9, M1-10, M1-13, M1-22).
15. Playwright: golden path and axe on new pages (M1-25, M1-26).
16. Docs: `requirements.md` §11, ADR-003, `architecture.md` §6 and §10, project log.
17. `/review-standards`, verification, pull request.

## Verification (end to end)

1. `docker compose up -d`, `./mvnw spring-boot:run`, `npm run dev`. Sign in as the dev user: My crews shows the greeting and the empty state.
2. Create "Sweinbrothar Boy's Night" (October 1999, an icon, the prefilled time zone): its home opens. Add three historical members, one Inactive; check the order and the "Inactive" section. Edit your own profile.
3. Create a second crew; switch between them with the switcher, by mouse and by keyboard alone. Delete the second: My crews shows the message, and its old URL shows "not found".
4. At 360 px (browser dev tools): bottom bar, no sideways scrolling; at 1280 px: sidebar.
5. `curl` without a token → `401`; a crew id you aren't in → `404` (with the dev user's token from the browser).
6. `./mvnw verify`; `npm run lint && npm run format:check && npm run typecheck && npm run test:coverage && npm run build`; `npm run e2e`. Coverage at or above the limits.
7. PR checks all green, and `/review-standards` reports "Ready for PR".

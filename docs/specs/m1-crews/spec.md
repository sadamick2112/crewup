# M1 — Crews and members: Spec

**Status:** Approved · October 29, 2026 (amended the same day: "group" renamed to "crew", decision 19) · Milestone M1 in `roadmap.md`
**Requirements:** GRP-1 (partial, see §6) · GRP-2 · GRP-3 · GRP-4 (partial) · MEM-1 · MEM-2 (partial) · MEM-3 · MEM-4 · MEM-5 (partial) · ACC-2 · NFR-7 (partial)

## 1. Goal

Turn the M0 skeleton into a usable app for one crew of friends: a signed-in user creates a crew, fills it with the people in its history, keeps their profiles up to date, and switches between crews. The demo: **sign in, create "Sweinbrothar Boy's Night", add three historical members, edit your own profile, create a second crew, switch between them, and delete one.**

M1 also builds the **app shell**, the frame around every signed-in page (header, navigation, crew switcher). Later milestones add their screens into it instead of designing a layout again.

## 2. User stories

- **US-1** As a signed-in user, I create a crew for my friends with its name, when it started, a motto and an icon, and I become its Administrator.
- **US-2** As a user in several crews, I see all my crews and switch between them from any page.
- **US-3** As an Administrator, I add the people in the crew's history, including those who will never have an account (moved away, not online, or deceased).
- **US-4** As a member, I keep my own profile up to date: names, birthday, contact details, links, food preferences and favorites.
- **US-5** As an Administrator, I edit the crew's details and any member's profile, and I can mark a member inactive without erasing them from the crew's history.
- **US-6** As an Administrator, I delete a crew I no longer need, with a confirmation step that makes an accidental delete unlikely.
- **US-7** As a newer member (New Guy), I see who is in the crew but not their contact details, until I'm promoted.

## 3. Acceptance criteria

Test names reference the criterion ID and the requirement ID where one applies (for example, `M1_2_GRP1_creatorBecomesAdministrator`).

### Crews (GRP-1…4, ACC-2)
- **M1-1** **Create a crew.** `POST /api/crews` creates a crew from: name (required), established month and year (required), icon (required, one of a built-in set of 12), time zone (required, an IANA name such as `America/Chicago`), motto (optional) and description (optional), plus the creator's first name in this crew (required). Field rules are in §3 *Field rules*. It returns `201` with a `Location` header. Invalid input returns `400` Problem Details naming each invalid field. *(GRP-1, without the picture, see §6)*
- **M1-2** **The creator becomes the first Member,** linked to their User, with the Administrator role, status Active, and the first name from the form. *(GRP-1)*
- **M1-3** **My crews.** `GET /api/crews` returns the crews in which the caller's User has a Member, and no others. Each item has the crew's id, name, icon, motto, established month, and the caller's role. Deleted crews are left out. Sorted by name (case-insensitive), with cursor pagination (API-7). *(ACC-2)*
- **M1-4** **View a crew.** `GET /api/crews/{crewId}` returns every crew field, plus the caller's own member id and role, so the UI knows which actions to offer. *(GRP-2, ACC-2)*
- **M1-5** **Edit a crew.** An Administrator updates every crew field with `PUT /api/crews/{crewId}` and the same field rules as M1-1. Updates use optimistic locking (API-7): the request must carry the `ETag` from the last read in `If-Match`. A stale value returns `412`; a missing one returns `428`. A non-Administrator gets `403`. *(GRP-2)*
- **M1-6** **Delete a crew.** An Administrator deletes a crew by sending its current name as confirmation. A name that doesn't match (ignoring leading and trailing spaces, otherwise exact) returns `400` and deletes nothing. On success (`204`), the crew is soft-deleted: from then on it returns `404` for everyone and is gone from M1-3. A non-Administrator gets `403`. *(GRP-3)*
- **M1-7** **Permanent deletion.** A daily job permanently deletes crews soft-deleted more than 30 days ago, with their members. Crews deleted less than 30 days ago are kept. *(GRP-3)*
- **M1-8** **Always an Administrator.** Every crew has at least one Administrator linked to a User from the moment it is created (M1-2). In M1 nothing can change a role or remove a member: a member update (M1-13) can't change the role or the account link. The checks against leaving or demoting the last Administrator arrive with those actions in M2. *(GRP-4, partial)*

### Members (MEM-1…5)
- **M1-9** **Add a historical member.** An Administrator adds a Member without an account with `POST /api/crews/{crewId}/members`, giving at least a first name and any other profile fields (M1-10). The new member has no linked User, the role New Guy (the ROL-1 default; it matters once someone claims the profile in M2), and status Active unless set otherwise. Returns `201` with `Location`. A non-Administrator gets `403`. *(MEM-1)*
- **M1-10** **Profile fields.** A member profile holds: first name, last name, nickname, pronouns, joined month and year, birthday (month and day, year optional), mobile phone, email, social links (a list of URL and label), status (Active or Inactive), food preferences (free text), and three favorites lists (music, video games, board games, each a list of short texts). Field rules are in §3 *Field rules*. *(MEM-2, without the picture, see §6)*
- **M1-11** **Members list.** Any member of the crew gets its members with `GET /api/crews/{crewId}/members`: id, first name, last name, nickname, pronouns, role, status, and whether the member has an account. Active members come first, then inactive ones. Within each, members are sorted by nickname if present, otherwise first name, then by last name (case-insensitive). Cursor pagination (API-7). *(MEM-2, MEM-5)*
- **M1-12** **View a member.** Any member of the crew gets a full profile with `GET /api/crews/{crewId}/members/{memberId}`, with contact fields as M1-14 allows. *(MEM-2)*
- **M1-13** **Edit a member.** A member edits their own profile, and an Administrator edits any profile, with `PUT /api/crews/{crewId}/members/{memberId}` and optimistic locking as in M1-5. Anyone else gets `403`. The role and the account link are not part of the request. *(MEM-3)*
- **M1-14** **Contact fields are hidden from New Guys.** For a caller with the New Guy role, the phone, email and birth year of other members are left out of every member response (list and profile). Administrators and Friends see them. Every caller sees their own. *(MEM-4)*
- **M1-15** **Inactive members stay.** Setting a member Inactive keeps them in the crew: they are still listed (last, under "Inactive", M1-11) and their profile can still be viewed and edited. Their place in attendee pickers (M3) and alerts (P2) comes later. *(MEM-5, partial)*

### Access and records (NFR-2, NFR-7)
- **M1-16** **Crew access is checked on every crew endpoint.** Each handler under `/api/crews/{crewId}/…` has `@PreAuthorize` calling a `CrewAccessEvaluator`, which resolves the caller's Member in that crew once per request. A caller who isn't a member, or a crew that doesn't exist or is deleted, gets `404` with the same body in every case, so the response never reveals whether a crew exists (API-5). A member without the needed role gets `403`. Tests cover each M1 endpoint for an Administrator, a Friend, a New Guy and a non-member; Friend and New Guy members are created directly in test data, since invites arrive in M2.
- **M1-17** **Records keep their history.** Crews and members record when and by which User they were created and last updated (NFR-7). Crew deletion is soft (M1-6, M1-7). Creating, editing, and deleting a crew, and adding and editing a member, each write one INFO log line with IDs only, never names or contact details (LOG-2, LOG-3).
- **M1-18** **API contract.** Every M1 endpoint and DTO is documented in OpenAPI (API-8). `api/openapi.json` is updated and the frontend client regenerated from it.

### Screens (ACC-2, NFR-3, NFR-4)
- **M1-19** **App shell.** Every signed-in page shows the header (§4.2). Inside a crew, the page also shows the crew navigation: a bottom bar below 768 px, a left sidebar from 768 px on. Signed out, the page is the M0 welcome page with no shell.
- **M1-20** **Crew switcher.** From any page in a crew, the switcher lists the user's crews with the current one marked, and picking one opens that crew's home. It also links to "All crews" and "Create a crew". *(ACC-2)*
- **M1-21** **Screens work as described in §4.3**, including their loading, empty and error states. An API error shows the M0 error message with "Reference: {traceId}" (M0-28).
- **M1-22** **Forms** use React Hook Form with a Zod schema (TS-7). Each field shows its own error next to it as the user leaves it, and the form checks all fields again on submit. Field errors returned by the server (`400`) appear on the matching fields. A `412` (someone else saved first) shows a message offering to reload the latest version.
- **M1-23** **Actions the user can't take aren't offered.** "Edit crew", "Add member" and "Edit" on someone else's profile appear only for Administrators; "Edit" on your own profile appears for everyone. The server still enforces every rule (M1-16); hiding is only for clarity.
- **M1-24** **Not found.** A URL for a crew or member that doesn't exist or isn't visible to the user shows a "not found" page with a link to the user's crews.
- **M1-25** **Accessibility and phone width.** Every new page has a component test with axe and no violations (TEST-10). The Playwright axe check covers My crews, Create crew, Crew home, Members, Member profile and Edit member at 360 px and desktop widths. The switcher, the user menu and the delete dialog work by keyboard alone: they open with Enter or Space, move with arrow keys or Tab, close with Escape, and put focus back on the button that opened them. *(NFR-3, NFR-4)*
- **M1-26** **End-to-end golden path** (Playwright, local as in M0): sign in → create a crew → add a historical member → edit your own profile → create a second crew → switch to the first with the switcher → delete the second.

### Field rules

All text is trimmed; an optional field left blank is stored as empty. Limits are counted in characters.

| Field | Rule |
|---|---|
| Crew name | Required, 1–80 |
| Established | Required, month and year, from January 1900 to the current month |
| Icon | Required, one of the 12 built-in icon keys; the first is preselected |
| Time zone | Required, a valid IANA time zone; the form prefills the browser's |
| Motto | Optional, ≤ 150 |
| Description | Optional, ≤ 2,000 |
| First name | Required, 1–50 |
| Last name, nickname | Optional, ≤ 50 each |
| Pronouns | Optional, ≤ 30, free text |
| Joined | Optional, month and year, from January 1900 to the current month |
| Birthday | Optional. Month and day together, or neither. Year optional, from 1900 to the current year. The date must exist (February 29 only without a year or in a leap year). |
| Mobile phone | Optional, ≤ 30: digits, spaces, and `+ - ( )` |
| Email | Optional, a valid address, ≤ 254 |
| Social links | Up to 10. Each: an `http` or `https` URL (≤ 500) and an optional label (≤ 50) |
| Food preferences | Optional, ≤ 1,000, free text (structured preferences are MEM-6, P2) |
| Favorites | Three lists (music, video games, board games), each up to 20 entries of ≤ 100 |
| Status | Active or Inactive; Active by default |

## 4. Screens

### 4.1 UI terms used here

| Term | Plain meaning | Backend comparison |
|---|---|---|
| **Screen / page** | What the browser shows at one URL, such as `/crews/{id}/members`. | One GET endpoint, but for people |
| **Route** | The rule that maps a URL to the screen that shows it. React Router does this. | `@GetMapping` on a controller |
| **App shell** | The frame that stays the same while pages change inside it: the header and the navigation. | A servlet filter that wraps every request |
| **Header** | The bar across the top of the screen. | — |
| **Bottom bar / sidebar** | The crew navigation: links to the crew's main sections. On a phone it sits at the bottom, where thumbs reach; on a wide screen it becomes a column on the left. | — |
| **Breakpoint** | The screen width where the layout changes, here 768 px (Tailwind's `md:`). Below it is "phone", from it on is "wide". | A feature flag keyed on screen width |
| **Menu** | A small list that opens over the page when you tap a button, and closes when you pick something or tap outside. | — |
| **Dialog** | A box that opens over a dimmed page and blocks it until you answer, for decisions such as "delete?". | A blocking confirmation prompt |
| **Avatar** | A small circle that stands for a person. Without pictures (§6) it shows their initials, such as "BJ". | — |
| **Empty state** | What a list shows when it has nothing in it yet, usually with a button for the first step. | — |
| **Loading state** | What shows while the data is on its way, so the page doesn't look broken. | — |

Sketches are at phone width (360 px, about 36 characters). `[ … ]` is a button or input, `( )` a choice, `>` a row you can tap, `(SA)` the user's avatar, `[#]` the crew's icon, and `:::` a dimmed page behind a menu or dialog.

### 4.2 The app shell

**Header** (every signed-in page):
- **Left:** inside a crew, the **crew switcher**: the crew's icon and name with a `v`, as one button. Long names are cut short with "…". Outside a crew (My crews, Create crew), it shows "CrewUp" instead.
- **Right:** the **user menu**, opened by the avatar. It shows the user's display name and email (not tappable), then "My profile in this crew" (only inside a crew; opens their Member profile) and "Sign out".

**Crew navigation** (pages inside a crew): "Home" and "Members" in M1. M3 adds "Events" and M6 "Timeline". The current section is highlighted, and not only by color (TS-9): it is also bold and marked for screen readers as the current page.

At phone width:

```
+------------------------------------+
| [#] Boy's Night  v            (SA) |
+------------------------------------+
|                                    |
|   (the current page's content)     |
|                                    |
|                                    |
+------------------------------------+
|   [^] Home        [=] Members      |
+------------------------------------+
```

**On a wide screen (768 px and up)**, the bottom bar moves into a left sidebar with the same links, the header spans the full width, and the page content gets the rest, with a maximum readable width:

```
+--------------------------------------------------------------------------+
| [#] Boy's Night  v                                                  (SA) |
+--------------------------------------------------------------------------+
|  [^] Home        |  Members                               [+ Add member] |
|  [=] Members     |  (DN) Dan - Dan Novak - Administrator         >       |
|                  |  (BJ) Big Joe - Joe Smith - Friend            >       |
|                  |  (MK) Mike - Mike Kowalski - No account       >       |
|                  |                                                       |
|                  |  Inactive (1)                                         |
|                  |  (RL) Rob - Rob Lee - No account              >       |
+--------------------------------------------------------------------------+
```

**Crew switcher, opened** (a menu under the header button; on a phone it is as wide as the screen allows):

```
+------------------------------------+
| [#] Boy's Night  ^            (SA) |
+------------------------------------+
| .--------------------------.       |
| | Your crews               |       |
| | [#] Boy's Night       ok |       |
| | [*] Thursday Games       |       |
| | ------------------------ |       |
| | All crews                |       |
| | + Create a crew          |       |
| '--------------------------'       |
| :::::::::::::::::::::::::::::::::  |
+------------------------------------+
```

### 4.3 Screens

| Screen | URL | Who sees it |
|---|---|---|
| Welcome (M0, unchanged) | `/` signed out | Everyone signed out |
| My crews | `/` signed in | Every signed-in user |
| Create crew | `/crews/new` | Every signed-in user |
| Crew home | `/crews/{crewId}` | Members |
| Crew settings | `/crews/{crewId}/settings` | Administrators |
| Members | `/crews/{crewId}/members` | Members |
| Add member | `/crews/{crewId}/members/new` | Administrators |
| Member profile | `/crews/{crewId}/members/{memberId}` | Members |
| Edit member | `/crews/{crewId}/members/{memberId}/edit` | The member themselves and Administrators |
| Not found | any unknown or hidden crew or member URL | Everyone signed in |

#### My crews (`/`, signed in)

**Shows** the M0 greeting ("Hello, {display name}", so M0-7 keeps holding) and the user's crews as cards: icon, name, "Since {month year}", and the user's role. **Can:** tap a card to open that crew's home, or "New crew" to create one. **Empty state:** with no crews, an explanation and a "Create a crew" button (M2 adds "Join with an invite" here).

```
+------------------------------------+
| CrewUp                        (SA) |
+------------------------------------+
| Hello, Shawn                       |
| Your crews            [+ New crew] |
| .-------------------------------.  |
| | [#] Sweinbrothar Boy's Night  |  |
| |     Since October 1999        |  |
| |     Administrator             |  |
| '-------------------------------'  |
| .-------------------------------.  |
| | [*] Thursday Board Games      |  |
| |     Since March 2015          |  |
| |     Administrator             |  |
| '-------------------------------'  |
+------------------------------------+
```

```
+------------------------------------+
| CrewUp                        (SA) |
+------------------------------------+
| Hello, Shawn                       |
| You're not in any crews yet.       |
| Create one for your crew; you      |
| can add the others next.           |
| [ Create a crew ]                  |
+------------------------------------+
```

#### Create crew (`/crews/new`)

**Shows** a form with the M1-1 fields. The icon is picked from a grid of 12 (a *radio group*: exactly one can be chosen); the time zone is a searchable list, prefilled from the browser; "Established" is a month menu and a year box. **Can:** "Create crew", which opens the new crew's home, or "Back" without saving.

```
+------------------------------------+
| <- Back                       (SA) |
+------------------------------------+
| New crew                           |
| Name *                             |
| [ Sweinbrothar Boy's Night     ]   |
| Established *                      |
| [ October  v ] [ 1999 ]            |
| Icon *                             |
|  (#) ( ) ( ) ( ) ( ) ( )           |
|  ( ) ( ) ( ) ( ) ( ) ( )           |
| Time zone *                        |
| [ America/Chicago           v ]    |
| Motto                              |
| [ Brothers by choice           ]   |
| Description                        |
| [                              ]   |
| [                              ]   |
| Your first name in this crew *     |
| [ Shawn                        ]   |
|                    [ Create crew ] |
+------------------------------------+
```

#### Crew home (`/crews/{crewId}`)

**Shows** the crew's icon, name, motto, "Since {month year}", description, and a member count that links to Members. **Can:** Administrators see "Edit crew", which opens Crew settings. This page is where later milestones put upcoming and recent events.

```
+------------------------------------+
| [#] Boy's Night  v            (SA) |
+------------------------------------+
| [#]  Sweinbrothar Boy's Night      |
|      "Brothers by choice"          |
|      Since October 1999            |
| Every Thursday since college:      |
| dinner, games, and arguments       |
| about the rules.                   |
| 12 members  >                      |
|                      [ Edit crew ] |
+------------------------------------+
|   [^] Home        [=] Members      |
+------------------------------------+
```

#### Crew settings (`/crews/{crewId}/settings`)

**Shows** the Create crew form, filled in with the current values (without "your first name"), and a separate "Danger zone" section at the bottom. **Can:** "Save" (back to Crew home), or "Delete crew…", which opens the delete dialog. A user who isn't an Administrator and opens this URL directly sees "Only Administrators can change this crew" and a link back.

```
+------------------------------------+
| <- Back                       (SA) |
+------------------------------------+
| Crew settings                      |
| (the same fields as New crew,      |
|  filled in, without "your first    |
|  name")                            |
|                           [ Save ] |
| Danger zone                        |
| Deleting hides the crew from       |
| everyone now and erases it after   |
| 30 days.                           |
| [ Delete crew... ]                 |
+------------------------------------+
```

**Delete dialog.** The "Delete" button stays disabled until the typed text matches the crew's name. "Delete" opens My crews with a short message ("Sweinbrothar Boy's Night was deleted"). "Cancel" or Escape closes the dialog with nothing changed.

```
+------------------------------------+
| [#] Boy's Night  v            (SA) |
+------------------------------------+
| :::::::::::::::::::::::::::::::::  |
| :.------------------------------.: |
| :| Delete this crew?            |: |
| :| Type the crew's name to      |: |
| :| confirm:                     |: |
| :| Sweinbrothar Boy's Night     |: |
| :| [                          ] |: |
| :|        [ Cancel ] [ Delete ] |: |
| :'------------------------------': |
| :::::::::::::::::::::::::::::::::  |
+------------------------------------+
```

#### Members (`/crews/{crewId}/members`)

**Shows** each member as a row: avatar with initials, the name people use (nickname, otherwise first name), and below it the full name and role, or "No account" for historical members. Inactive members are in a separate "Inactive" section at the end (M1-11). **Can:** tap a row for the profile; Administrators also see "Add member". When there are more members than one page holds, a "Show more" button loads the next page.

```
+------------------------------------+
| [#] Boy's Night  v            (SA) |
+------------------------------------+
| Members             [+ Add member] |
| (DN) Dan                      >    |
|      Dan Novak - Administrator     |
| (BJ) Big Joe                  >    |
|      Joe Smith - Friend            |
| (MK) Mike                     >    |
|      Mike Kowalski - No account    |
| Inactive (1)                       |
| (RL) Rob                      >    |
|      Rob Lee - No account          |
+------------------------------------+
|   [^] Home        [=] Members      |
+------------------------------------+
```

#### Member profile (`/crews/{crewId}/members/{memberId}`)

**Shows** the profile in sections: name, pronouns and role at the top, then About (joined, birthday), Contact (phone, email), Links, Food preferences and Favorites. Empty sections are left out. For a New Guy viewing someone else, the Contact section is left out and the birthday shows without the year (M1-14). An "Inactive" badge appears next to the name when it applies. **Can:** "Edit", when allowed (M1-23); links open in a new tab.

```
+------------------------------------+
| [#] Boy's Night  v            (SA) |
+------------------------------------+
| <- Members                [ Edit ] |
| (BJ) Big Joe                       |
|      Joe Smith - he/him            |
|      Friend                        |
| About                              |
|   Joined     March 2004            |
|   Birthday   June 12, 1978         |
| Contact                            |
|   Phone      +1 555 0100           |
|   Email      joe@example.com       |
| Links                              |
|   Instagram  >                     |
| Food preferences                   |
|   No mushrooms. Loves BBQ.         |
| Favorites                          |
|   Board games  Catan, Azul         |
+------------------------------------+
|   [^] Home        [=] Members      |
+------------------------------------+
```

#### Add member and Edit member

**Shows** one form for both, with the M1-10 fields grouped in the same sections as the profile. Links and favorites are lists where each entry has its own box and a remove button, with "Add" for a new entry. Add member starts empty with a hint that the person won't have an account yet; Edit starts filled in. **Can:** "Save", which opens the profile; or "Cancel".

```
+------------------------------------+
| <- Cancel                     (SA) |
+------------------------------------+
| Edit Big Joe                       |
| Name                               |
| First name *  [ Joe            ]   |
| Last name     [ Smith          ]   |
| Nickname      [ Big Joe        ]   |
| Pronouns      [ he/him         ]   |
| About                              |
| Joined   [ March  v ] [ 2004 ]     |
| Birthday [ June v ] [ 12 ] [1978]  |
| Status   (o) Active  ( ) Inactive  |
| Contact                            |
| Phone    [ +1 555 0100        ]    |
| Email    [ joe@example.com    ]    |
| Links       [+ Add link]           |
| Food preferences    [         ]    |
| Favorites   [+ Add] per list       |
|                           [ Save ] |
+------------------------------------+
```

#### Not found

Shown for any crew or member URL the user can't see (M1-24), with the same wording whether it never existed, was deleted, or belongs to someone else's crew.

```
+------------------------------------+
| CrewUp                        (SA) |
+------------------------------------+
| Crew not found                     |
| It may have been deleted, or       |
| you're not a member.               |
| [ Go to your crews ]               |
+------------------------------------+
```

### 4.4 How the screens connect

```
Welcome --sign in--> My crews (/)
My crews --card--> Crew home
My crews --New crew--> Create crew --Create--> Crew home (new crew)
Crew home --Edit crew (Admin)--> Crew settings --Save--> Crew home
Crew settings --Delete crew... --> Delete dialog --Delete--> My crews
Crew home <--bottom bar / sidebar--> Members
Members --row--> Member profile --Edit--> Edit member --Save--> Member profile
Members --Add member (Admin)--> Add member --Save--> Member profile (new)
Header: crew switcher --> another crew's Home | All crews (My crews) | Create crew
Header: user menu --> My profile in this crew (Member profile) | Sign out (Welcome)
```

## 5. Documentation deliverables

- `requirements.md` §11: statuses for every M1 requirement, with the partial ones naming what's left (pictures, GRP-4 checks in M2, MEM-5's picker in M3 and alerts in P2).
- ADR-003 (User/Member split, roles on Member) written in `docs/adr/` and marked Accepted, since M1 implements it. `architecture.md` §10 links it.
- `architecture.md` §6 updated if the `CrewAccessEvaluator` design differs from what it describes.

## 6. Out of scope

- **Crew and member pictures** (the picture part of GRP-1 and MEM-2). Crews use the built-in icon and members their initials. Pictures get one storage design together with Activity photos (PHO-1, MinIO), so GRP-1 and MEM-2 stay partial.
- **Role changes, removing members, and leaving a crew** (ROL-1, ROL-2, and the GRP-4 checks): M2. Members can be marked Inactive instead.
- **Invitations** (INV-1…3, M2). Until then, a crew's only account holder is its creator.
- **The endpoint guard test** that fails when a crew handler lacks `@PreAuthorize` (M2).
- **Restoring a deleted crew** within the 30 days. There is no screen or endpoint for it; the 30 days are a safety net a developer can use in the database.
- **Structured food preferences** (MEM-6, P2) and **favorites linked to Board Game records** (M5). Both are free text in M1.
- Attendee pickers (M3), alerts (SOC-1…3, P2), "On this day" (P2).
- Remembering the last-used crew so the app opens straight into it.
- Warning about unsaved changes when leaving a form.
- Editing the User's display name or email, which still come from the identity provider (as in M0).

## 7. Decisions

Resolved October 29, 2026:

1. **Pictures are deferred** (§6). An interim store (image bytes in Postgres) and bringing MinIO forward were considered. Both build storage twice or pull Phase 2 work into M1.
2. **M1 checks access for its own rules.** It builds the `CrewAccessEvaluator` and `@PreAuthorize` for "is a member" and "is an Administrator", plus MEM-4's field hiding (M1-14, M1-16). M2 adds the full ROL-2 matrix, role changes and the guard test. Leaving the Administrator checks to M2 was considered, but would leave GRP-2, GRP-3, MEM-1, MEM-3 and MEM-4 untested in M1.
3. **A deleted crew is hidden at once and never restored**; a daily job erases it after 30 days (M1-6, M1-7).
4. **No leave, remove or demote in M1** (M1-8). GRP-4's checks arrive in M2 with the actions that could break the rule.
5. **"Joined" has month and year precision,** like the crew's "established" date.
6. **The crew's time zone is a create and edit field,** prefilled from the browser. Events default to it in M3 (EVT-1).
7. **The creator's first name is a create-form field,** prefilled from the account's display name.
8. **"Sign out" moves into the user menu.** This changes two committed M0 tests: `M0_7_signOutButtonSignsOut` (`HomePage.test.tsx`) and the sign-out step of `M0_1_M0_5_devUserSignsInAndOut` (e2e) now open the menu first. Approved under TEST-11, in their own `test:` commit, with the same assertions.

Proposed in the draft and approved with the spec, the same day:

9. **`/` signed in is My crews,** with the M0 greeting on it, so M0-7 still holds. Opening the last-used crew directly is out of scope (§6).
10. **Picking a crew in the switcher opens its home,** not the same section in the new crew, so the result is always predictable.
11. **Historical members get the New Guy role** (ROL-1's default). The role is shown as "No account" for them; it matters only once someone claims the profile (INV-3, M2).
12. **Non-members get `404`, members without the role get `403`** (M1-16, API-5).
13. **`created_by` and `updated_by` hold the User's id,** not the Member's: a crew is created before its first Member exists. "Own only" checks in M2 map the User to their Member in that crew.
14. **The delete confirmation is checked by the server too** (M1-6), so a stray API call can't delete a crew. The plan picks how the name travels with the request.
15. **The 12 crew icons come from `lucide-react`,** which the project already uses. The plan lists them.
16. **The breakpoint is 768 px** (Tailwind's `md:`, TS-12). The bottom bar has only two links in M1; it is built for the four it will have by M6.
17. **Field limits** are those in §3 *Field rules*.
18. **New frontend dependencies:** React Hook Form, Zod and the resolver that connects them (already in the planned stack), and the shadcn/ui components the screens need (menu, dialog, form fields). The plan names each with its reason (GEN-9).

19. **"Group" is renamed "crew" everywhere** (screens, API paths, code, tables, docs; added after approval, the same day), to match the product's name and to avoid clashes with `GROUP BY`, Tailwind's `group` class and shadcn's `*Group` components. Requirement IDs keep the GRP prefix. Plain-English uses that aren't the entity stay, such as "radio group".

## 8. Open questions

None.

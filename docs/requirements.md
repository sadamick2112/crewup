# CrewUp — Product Requirements

**Status:** Draft v0.2 · October 13, 2026 (open questions resolved, see §9) · Source: "CrewUp App" OneNote page (Shawn Adamick)
**Decisions already made:** React web client first (mobile later) · Spring Boot modular monolith · MVP = core logging + one AI feature (board game lookup)

---

## 1. Vision

Friend groups that have met for years build up thousands of shared memories that blur together. CrewUp gives a group one fun, shared place to **plan** get-togethers, **log** what happened (where, who, what was eaten, played, or watched, and who won), and **look back** on it. Activity Types (Dining Out, Board Gaming, Movies, and others later) plug in as independent modules, so new kinds of activities can be added without touching existing ones.

### Goals

1. Logging a weekly get-together takes under a minute on a phone.
2. Any member can answer "When did we last go to Joe's?", "Who's won Catan the most?", or "What did I order last time?" in a few taps.
3. Adding a new Activity Type requires no changes to other Activity Types.
4. The codebase is a credible portfolio piece: tested, documented, deployable, with a live demo.

### Non-goals (for now)

- Public or social-network features outside a group
- Payments, ticket purchasing, or bill splitting
- Native mobile apps (planned after the web MVP)
- Group chat or messaging
- Importing past history (spreadsheets, photos). History is entered manually; the demo uses generated seed data.
- Commercial use. CrewUp is a non-commercial portfolio project; third-party API terms (BGG, TMDB, places) are checked for non-commercial use only. Revisit them if this changes.

## 2. Users and roles

| Persona | Description |
|---|---|
| **Administrator** | Creates and manages the group, members, and roles. Usually the group's organizer. |
| **Friend** | A regular member who plans and logs activities. |
| **New Guy** | A newly invited member with mostly read-only access until promoted. Can add their own ratings and notes on Activities they attended, but can't see members' contact info. |
| **Historical member** | Someone in the group's history who has no account (moved away, not tech-savvy, or deceased). They still appear in attendee lists. |

> **Modeling change:** The original document combines "Person" with the login identity. This spec separates the **User** (a login account) from the **Member** (a person's profile within one group). This allows historical members without accounts, lets one User belong to several groups, and assigns roles per group. See §7.

## 3. Release phases

| Phase | Contents |
|---|---|
| **MVP (Phase 1)** | Accounts · groups · members · roles · QR invites · Events and Activities · attendee picker · copy event · memory timeline and search · Dining Out (manual) · Board Gaming (manual) · win chart · **AI: board game lookup** · deployed demo with seed data |
| **Phase 2** | Photos · birthday and anniversary alerts · Movies Activity Type · AI restaurant details lookup · AI restaurant recommendations · AI board game suggestions · AI movie lookup · email invites · "On this day" |
| **Phase 3** | AI event timing (movie + dinner) · showtime lookup · SMS invites · cloud photo integrations · mobile app (React Native/Expo) · data export |

Each requirement below is tagged **[MVP]**, **[P2]**, or **[P3]**.

## 4. Glossary

| Term | Meaning |
|---|---|
| **Group** | A friend group, such as "Sweinbrothar Boy's Night". The top-level tenant: all data belongs to exactly one group. |
| **User** | A login account (email or OIDC identity). Can belong to several groups. |
| **Member** | A person's profile in one group. May or may not be linked to a User. Holds the role. |
| **Event** | A get-together on a date, containing one or more Activities. Example: "Boy's Night, Oct 6, 2026". |
| **Activity** | One thing done during an Event, such as dinner or a board game night. It has a type, a start time, attendees, and a log. |
| **Activity Type** | A plug-in module (Dining Out, Board Gaming, Movies) that defines the Activity's specialized details, UI, and AI features. |
| **Catalog item** | A reusable reference record owned by an Activity Type, such as a Restaurant, Board Game, Host, Cinema, or Movie. Catalogs are per group; nothing is shared across groups. |

## 5. Functional requirements

### 5.1 Accounts and authentication

- **ACC-1 [MVP]** Users sign up and sign in through an OIDC provider (email/password plus at least one social login).
- **ACC-2 [MVP]** A signed-in User sees the list of groups they belong to and can switch between them.
- **ACC-3 [P2]** Users can delete their account. Their Member profiles remain as historical members, unlinked from the account.

### 5.2 Groups

- **GRP-1 [MVP]** Any User can create a group with: name (required), established date (month and year precision, such as "October 1999"), motto (optional), description (optional), icon (choice from a built-in icon set), and picture (optional, JPEG/PNG, ≤ 5 MB).
  - *Acceptance:* The creator becomes a Member with the Administrator role.
- **GRP-2 [MVP]** Administrators can edit all group fields.
- **GRP-3 [MVP]** Administrators can delete a group after typing its name to confirm. Deletion is soft for 30 days, then permanent.
- **GRP-4 [MVP]** A group must always have at least one Administrator. The last Administrator cannot leave or be demoted.

### 5.3 Members

- **MEM-1 [MVP]** Administrators can add a Member without an account (historical member), with at least a first name.
- **MEM-2 [MVP]** Member profile fields (see §7 for types): first name, last name, nickname, pronouns (optional), picture, joined date, birthday (month and day required, year optional), mobile phone, email, social links (list), status (active/inactive), food preferences, and favorites (music, video games, board games).
- **MEM-3 [MVP]** Members can edit their own profile. Administrators can edit any profile.
- **MEM-4 [MVP]** Contact fields (phone, email, birthday year) are visible only to Administrators and Friends of the same group. New Guys don't see them until promoted. A Member always sees their own contact fields.
- **MEM-5 [MVP]** Inactive members still appear in history, but are sorted to the bottom of attendee pickers and excluded from alerts.
- **MEM-6 [P2]** Food preferences are structured, not free text:
  - Cuisine sentiments: a list of (cuisine, LOVE/LIKE/DISLIKE/HATE)
  - Favorite restaurants and avoided restaurants, as links to Restaurant records
  - Preferred price range
  - Dietary restrictions, chosen from a list (Gluten-free, Vegetarian, Vegan, Dairy-free, Nut allergy, Halal, Kosher, Other)

### 5.4 Roles and permissions

- **ROL-1 [MVP]** Each Member has exactly one role per group: Administrator, Friend, or New Guy. Invited Members default to New Guy.
- **ROL-2 [MVP]** Default permission matrix:

| Action | Administrator | Friend | New Guy |
|---|---|---|---|
| View group, members, events, history | ✓ | ✓ | ✓ |
| View members' contact info (MEM-4) | ✓ | ✓ | — |
| Create or edit Events and Activities | ✓ | ✓ | — |
| Edit or delete any Event or Activity | ✓ | Own only | — |
| Add own rating, dishes, or notes on an attended Activity | ✓ | ✓ | ✓ |
| Manage catalog (restaurants, games, hosts) | ✓ | ✓ | — |
| Invite members (always joined as New Guy, INV-2) | ✓ | ✓ | — |
| Change roles, remove members, edit group | ✓ | — | — |

- **ROL-3 [P2]** Permissions can be overridden per Activity Type. For example, New Guys could log board game scores but not create dinners. This implements "privileges based on the activity" from the original document.

### 5.5 Invitations

- **INV-1 [MVP]** A permitted Member can generate an invite as a QR code and a shareable link. Invites expire (default 7 days, configurable) and can be revoked.
- **INV-2 [MVP]** Scanning the code or opening the link leads the person through sign-up or sign-in and joins them to the group as a New Guy.
- **INV-3 [MVP]** When creating an invite, the inviter can optionally link it to an existing historical Member, so the new User claims that profile and its history.
- **INV-4 [P2]** Email invites. **INV-5 [P3]** SMS invites.

### 5.6 Events and Activities (core)

- **EVT-1 [MVP]** An Event has a name, date, start time, time zone (defaulting to the group's), and an ordered list of Activities. Every Activity belongs to an Event; a single dinner is an Event with one Activity.
- **EVT-2 [MVP]** The Event name is auto-suggested, for example "Boy's Night – Dinner & Board Gaming – Oct 6, 2026 @ 7:30 PM", and can be edited.
- **EVT-3 [MVP]** Every Activity has: Activity Type, start date and time (defaulting to the Event start), optional end time or duration, attendees (Members), a free-text log (Markdown), and type-specific details.
- **EVT-4 [MVP]** Events can be in the past (logging) or the future (planning). For a future Activity, attendees means "planning to attend". After the start time passes, the list is treated as actual attendance and can be confirmed or edited.
- **EVT-5 [MVP]** **Copy event:** Copy an Event with a new date and start time. Activity order, types, catalog selections (restaurant, host, games), and attendees carry over. Logs, ratings, scores, and photos do not. Activity times shift by the same offset.
- **EVT-6 [MVP]** Members can add, reorder, and remove Activities within an Event.
- **EVT-7 [MVP]** The create flow is mobile-first: pick a type, confirm time, pick attendees, then optionally add details. Everything except type and attendees has a default.

### 5.7 Attendee picker

- **PICK-1 [MVP]** A multi-select list of the group's Members with search by name or nickname.
- **PICK-2 [MVP]** Sorting is configurable per user, with the choice remembered:
  - Alphabetical (by nickname if present, otherwise first name)
  - Most frequent attendee first (count over the last 12 months), then alphabetical
- **PICK-3 [MVP]** A "Same as last time" shortcut selects the previous Event's attendees.
- **PICK-4 [MVP]** Inactive members appear last, under a collapsed "Inactive" section.

### 5.8 Memory timeline and search

This addresses the core pain point: "everything blends together."

- **MEM-T1 [MVP]** A reverse-chronological timeline of Events, with filters for date range, Activity Type, attendee, and catalog item (restaurant, game, host).
- **MEM-T2 [MVP]** Free-text search across Event names, Activity logs, notes, and dish names.
- **MEM-T3 [MVP]** Each catalog item has a history page. For example, a Restaurant page lists every visit, its date, attendees, average group rating, and what each person ordered.
- **MEM-T4 [P2]** "On this day": Events from the same date in previous years, shown on the home screen.
- **MEM-T5 [P3]** Export all group data as JSON and CSV, plus photos as a ZIP. Decades of memories must not be locked in the app.

### 5.9 Photos

- **PHO-1 [P2]** Attach up to 20 photos per Activity (JPEG, PNG, or HEIC converted to JPEG; ≤ 10 MB each), with thumbnails generated.
- **PHO-2 [P3]** Link albums from Google Photos, OneDrive, Dropbox, or iCloud instead of uploading.

### 5.10 Social alerts

- **SOC-1 [P2]** In-app and email alerts to active members about a member's birthday (on the day, with an optional 3-days-ahead reminder).
- **SOC-2 [P2]** Group anniversary alerts: yearly ("CrewUp: 27 years!"), with special messaging every 5 years ("Party time!") and every 10 years ("Big party!").
- **SOC-3 [P2]** Each member can opt out of each alert type.

### 5.11 Dining Out Activity Type

- **DIN-1 [MVP]** Select a Restaurant from the group's list, sorted by most recently visited, showing the "last visited" date. Search by name or cuisine.
- **DIN-2 [MVP]** Add a Restaurant manually: name, cuisines (multi-select from a preset list), address, phone, website URL, menu URL, price range, hours (structured by day), and notes.
- **DIN-3 [MVP]** After the Activity, each attendee can add their own star rating (1–5), dishes ordered (a list), and notes. These appear on the Activity and on the Restaurant's history page.
- **DIN-4 [MVP]** Optional dining duration in hours, in 0.25 increments.
- **DIN-5 [MVP]** A Restaurant's group rating is the average of all attendee ratings across visits, shown with the count.
- **DIN-6 [P2] AI lookup:** See AI-2.
- **DIN-7 [P2] AI recommendations:** See AI-3.

### 5.12 Board Gaming Activity Type

- **BG-1 [MVP]** A Board Gaming Activity has a Host (selected or added), start time, optional total duration, and one or more Game Sessions.
- **BG-2 [MVP]** A Host has a name (such as "Dan's house"), an optional address, and an optional linked Member.
- **BG-3 [MVP]** A Game Session records: Board Game, start time (defaulting to the Activity's), expansions used (selected from the game's known expansions, or free text), players (Members), each player's optional score, each player's finishing placement (ties allowed), optional team assignment, notes, and duration.
- **BG-4 [MVP]** Board Game catalog fields: name, year published, designers, picture, BoardGameGeek ID and URL, BGG average rating, categories and mechanics, description, minimum and maximum players, estimated play time, rules URL, and an optional "music theme" text.
- **BG-5 [MVP]** Add a Board Game manually or through AI lookup (AI-1).
- **BG-6 [MVP]** Win statistics: a bar chart of first-place finishes by Member for a selected game, filterable by date range. Also show games played and win rate.
- **BG-7 [P2] AI game suggestions:** See AI-7.
- **BG-8 [P2]** Game ownership: record which Members own each Board Game (zero or more owners). A game's page lists its owners, and a Member's profile lists the games they own. AI-7 uses this.

### 5.13 Movies Activity Type [P2]

- **MOV-1** Select or add a Cinema: name, address, phone, website URL.
- **MOV-2** Select or add a Movie to the catalog: title, year, external IDs and URL (IMDb and/or TMDB), director, cast, genres, runtime in minutes, MPAA rating, and critic score (where licensable).
- **MOV-3** A Movie Activity has: Cinema, Movie, showing date and time, attendees, notes (such as "tickets bought by Dan, Theater 6, seats D1–D5"), and per-attendee rating (1–5) with notes.

> **Modeling change:** The original "Movie details" mixes the movie itself (title, director) with one viewing (cinema, time, attendees, ratings). This spec splits them into a **Movie** catalog item and a **Movie Activity**, so the same movie seen twice isn't duplicated.

### 5.14 AI features

General rules for all AI features:

- **AI-0a** AI never writes data without the user confirming it. It proposes, and the user accepts or edits.
- **AI-0b** Each AI feature shows where the data came from (source links).
- **AI-0c** Every AI feature has a non-AI fallback (manual entry), so the app works if AI is unavailable or over budget.
- **AI-0d** There is a per-group monthly AI budget with graceful degradation when it runs out.
- **AI-0e [MVP]** The LLM provider is configurable per deployment: Claude (default) or a local model served by Ollama. Switching providers requires no code changes.

| ID | Phase | Feature | Notes |
|---|---|---|---|
| **AI-1** | MVP | **Board game lookup:** Type a name; the AI searches BoardGameGeek, shows matching candidates (name, year, picture), and fills in the catalog fields for the one the user picks | Uses the BGG XML API via tool calling. BGG now requires registering an application for API access; check the terms. |
| **AI-2** | P2 | **Restaurant lookup:** Search by name and city; fill in address, phone, hours, cuisine, price range, and URLs | Uses a places API (Google Places, Foursquare, or OpenStreetMap) as a tool. Fetching the restaurant's own site is a fallback. |
| **AI-3** | P2 | **Restaurant recommendations** for a future Dining Out Activity, based on attendees | Deterministic scoring (dietary needs, cuisine sentiment, group ratings, visited in the last 2 weeks, open at the planned time) ranks restaurants. The LLM explains the ranking and shows diet icons. See architecture. |
| **AI-4** | P2 | **Movie lookup:** Type a title and pick from matches | Use the TMDB or OMDb API. Scraping IMDb violates IMDb's terms, and IMDb has no free public API. |
| **AI-5** | P3 | **Showtime lookup** for a selected Cinema | High risk: there is no reliable free showtimes API. Prototype first. |
| **AI-6** | P3 | **Event timing:** For a movie + dinner Event, project the movie's end time (runtime plus about 30 minutes of previews) and travel time, and suggest restaurants open within a radius. Offer to drop the dinner if it's too late. | Needs AI-2, AI-3, AI-4, and a routing API. |
| **AI-7** | P2 | **Board game suggestions:** Given the player count and time available, suggest owned (BG-8) or played games that fit, with projected end time | |

## 6. Non-functional requirements

| ID | Requirement |
|---|---|
| **NFR-1 Privacy** | All data is scoped to one group. No cross-group visibility. Contact info is visible to the group's Administrators and Friends only (MEM-4). |
| **NFR-2 Security** | OIDC authentication. Group-scoped authorization is checked on every request. OWASP Top 10 is addressed. Secrets are never stored in the repo. |
| **NFR-3 Responsiveness** | Mobile-first responsive UI, usable at a 360 px width. Installable as a PWA. |
| **NFR-4 Accessibility** | WCAG 2.2 AA: keyboard navigable, sufficient contrast, labeled controls. |
| **NFR-5 Performance** | p95 API latency under 300 ms for non-AI calls with the demo dataset (one group, about 2,000 Events). AI calls show progress and can be cancelled. |
| **NFR-6 Time** | Store instants in UTC with the Event's IANA time zone. Display in the viewer's local time. |
| **NFR-7 Auditability** | Every record has created and updated timestamps and authors. Deletes are soft for 30 days. |
| **NFR-8 Testability** | Every requirement ID maps to at least one automated test. Module boundaries are verified in CI. |
| **NFR-9 Operability** | Structured logs with a trace ID on every line, request tracing from the browser through the backend, health checks, and basic metrics. Runs locally with one `docker compose up`. |
| **NFR-10 AI cost** | AI calls are metered per group. Responses are cached (for example, board game details by BGG ID). |

## 7. Logical data model

IDs are UUIDs (v7, which sorts by time). Every table carries `group_id` (except User), plus `created_at`, `created_by`, `updated_at`, `updated_by`, and `deleted_at`.

**Core**
- **User:** id, oidc_subject, email, display_name
- **Group:** id, name, established_month (YYYY-MM), motto, description, icon_key, picture_media_id, time_zone
- **Member:** id, group_id, user_id (nullable), role (ADMIN/FRIEND/NEW_GUY), first_name, last_name, nickname, pronouns, picture_media_id, joined_on, birth_month, birth_day, birth_year (nullable), phone, email, status (ACTIVE/INACTIVE)
- **MemberSocialLink:** member_id, url, label
- **MemberCuisineSentiment:** member_id, cuisine_code, sentiment (LOVE/LIKE/DISLIKE/HATE)
- **MemberDietaryRestriction:** member_id, restriction_code
- **MemberFavorite:** member_id, kind (MUSIC/VIDEO_GAME/BOARD_GAME), value, board_game_id (nullable)
- **Invitation:** id, group_id, token_hash, role, member_id (nullable, for claiming a profile), expires_at, revoked_at, used_by_user_id
- **Event:** id, group_id, name, starts_at, time_zone, copied_from_event_id
- **Activity:** id, event_id, type_code, position, starts_at, ends_at, log_markdown
- **ActivityAttendee:** activity_id, member_id, status (PLANNED/ATTENDED)
- **Media [P2]:** id, group_id, owner (type and id), storage_key, content_type, width, height

**Dining Out module**
- **Restaurant:** id, group_id, name, cuisine_codes[], address fields, lat/lng, phone, website_url, menu_url, price_range (UNDER_10…OVER_50), notes, external_place_id
- **RestaurantHours:** restaurant_id, day_of_week, opens, closes
- **DiningDetail:** activity_id, restaurant_id, duration_hours
- **DiningReview:** activity_id, member_id, stars (1–5), notes
- **DiningDish:** review_id, name, verdict (GET_AGAIN/OK/AVOID), notes

**Board Gaming module**
- **Host:** id, group_id, name, address fields, member_id (nullable)
- **BoardGame:** id, group_id, bgg_id, name, year, designers[], image_url, bgg_rating, categories[], mechanics[], description, min_players, max_players, play_time_min, play_time_max, rules_url, music_theme
- **BoardGamingDetail:** activity_id, host_id, duration_hours
- **GameSession:** id, activity_id, board_game_id, starts_at, expansions[], notes, duration_hours
- **GameSessionPlayer:** session_id, member_id, score (nullable), placement (nullable; ties allowed), team (nullable)
- **BoardGameOwner [P2]:** board_game_id, member_id

**Movies module [P2]**
- **Cinema:** id, group_id, name, address fields, lat/lng, phone, website_url
- **Movie:** id, group_id, title, year, imdb_id, tmdb_id, director, cast[], genres[], runtime_min, mpaa_rating, critic_score
- **MovieDetail:** activity_id, cinema_id, movie_id, showing_at, notes
- **MovieReview:** activity_id, member_id, stars, notes

## 8. Review notes: changes from the original document

| Original | Change | Reason |
|---|---|---|
| Person holds login, profile, and role | Split into User, Member, and role on Member | Supports historical members, multiple groups, and per-group roles |
| CSV strings (cuisines, actors, genres, favorites) | Child tables or arrays with codes | CSV can't be queried, validated, or reasoned over reliably by the AI |
| Gender: male / female / they | Optional free-text pronouns | No feature needs gender, and "they" is a pronoun, not a gender. Ask only for what's used. |
| Dates and times as strings | Typed dates, instants with time zones, and month precision where needed | Sorting, copying events, and timing math all depend on real types |
| Winners stored by name (1st/2nd/3rd) | Placement per player, ties allowed | Scales to any player count and supports win statistics |
| Movie record includes the viewing | Movie catalog vs. Movie Activity | Avoids duplicates when a movie is seen twice |
| Restaurant rating as a field | Derived from per-visit reviews | Ratings come from attendees, per visit |
| Activities optionally in Events | Every Activity belongs to an Event | One model for planning, copying, and the timeline |
| IMDb search or scrape | TMDB or OMDb API | IMDb's terms prohibit scraping, and it has no free API |
| Each Activity Type as a microservice | Modules in a modular monolith, extractable later | See `architecture.md` ADR-001 |

## 9. Resolved questions

All resolved on October 13, 2026.

| # | Question | Decision | Reflected in |
|---|---|---|---|
| 1 | Can a **New Guy** add their own ratings and dish notes for an Activity they attended? | Yes | §2, ROL-2 |
| 2 | Can **Friends** send invites, or only Administrators? | Yes. Invitees always join as New Guy. | ROL-2, INV-2 |
| 3 | Are catalogs **per group or shared** across all groups? | Per group. A shared catalog raises moderation issues. | §4 Catalog item, §7 |
| 4 | Should **New Guys** see members' contact info? | No, not until promoted | MEM-4, ROL-2, NFR-1 |
| 5 | Should the app track **board game ownership** (useful for AI-7)? | Yes, in Phase 2 alongside AI-7 | BG-8, AI-7, §7 BoardGameOwner |
| 6 | Will past history be **imported** (spreadsheets, photos)? | No. No import feature is planned. | §1 Non-goals |
| 7 | Is there **commercial intent** (affects BGG, TMDB, places API terms)? | Not yet. Treat as non-commercial; revisit API terms if that changes. | §1 Non-goals |

## 10. Open questions

None at the moment.

## 11. Implementation status

As of M0 (done, PR #1). Requirements not listed here are **not started**. Each milestone updates this table (roadmap definition of done). **Partial** means some of the requirement works and the rest is planned for the milestone named in the notes.

| ID | Status | Since | Notes |
|---|---|---|---|
| ACC-1 | Partial | M0 | Email/password sign-up, sign-in and sign-out through the OIDC provider; `GET /api/me` provisions the User. Social login in M8 (ADR-004). |
| NFR-2 | Partial | M0 | OIDC resource server, no secrets in the repo, CI security scans (Semgrep, CodeQL, gitleaks, Dependabot). Group-scoped authorization arrives with groups (M1, M2). |
| NFR-3 | Partial | M0 | Home page usable at 360 px (Playwright check). PWA later. |
| NFR-4 | Partial | M0 | axe checks (component tests and Playwright) on every page so far. Applies to each new page. |
| NFR-8 | Partial | M0 | Test names reference requirement IDs; Modulith `verify()` runs in CI. Applies to each new requirement. |
| NFR-9 | Partial | M0 | Structured logs with trace IDs, browser-to-backend trace correlation, health check, `docker compose up` for local services. Basic metrics not yet. |

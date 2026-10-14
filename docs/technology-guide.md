# CrewUp — Technology Guide

A plain-language guide to the technologies and terms used in CrewUp. For each one, it covers what it is, what it does in CrewUp, and why it was chosen. No prior knowledge of any of them is assumed.

For how the pieces fit together in detail, see `architecture.md`. For the reasoning behind the bigger choices, see the ADRs in `docs/adr/`.

**Contents:** [The big picture](#the-big-picture) · [Languages and build tools](#languages-and-build-tools) · [Backend](#backend) · [Sign-in and security](#sign-in-and-security) · [Logging and tracing](#logging-and-tracing) · [AI](#ai) · [Frontend](#frontend) · [Testing](#testing) · [Code quality](#code-quality) · [Running and delivering](#running-and-delivering) · [Conventions](#conventions) · [Planned for later](#planned-for-later) · [Quick reference](#quick-reference)

---

## The big picture

CrewUp has three main parts:

1. **A web app** that runs in your browser (React and TypeScript, built with Vite).
2. **A backend API** that stores and serves data (Java and Spring Boot, organized with Spring Modulith, backed by PostgreSQL).
3. **An identity provider** that handles accounts and sign-in (Keycloak during development).

The browser app never stores passwords and never talks to the database. It signs the user in through the identity provider, gets a short-lived **access token**, and sends that token with every call to the backend. The backend checks the token, then decides what the caller is allowed to do.

The backend and frontend agree on the shape of every request and response through an **OpenAPI** contract, from which the frontend's API client is generated.

---

## Languages and build tools

### Java

**What it is.** A general-purpose programming language that runs on the Java Virtual Machine (JVM). It's strongly typed and widely used for business applications and web backends.

**What it does in CrewUp.** The whole backend is written in Java 25, a **long-term support (LTS)** release, meaning it gets security fixes for years. Modern Java features such as **records** (compact, immutable data classes) keep the code short.

**Why.** It's the author's strongest language, and Java with Spring is one of the most common backend stacks in industry.

### Maven

**What it is.** A build tool for Java. It downloads the libraries a project depends on, compiles the code, runs the tests, and packages the application. The project's setup is described in one file, `pom.xml`.

**What it does in CrewUp.** Every backend command goes through the **Maven Wrapper** (`./mvnw`), a small script checked into the repository that downloads the right Maven version automatically. Nobody needs to install Maven themselves.
- `./mvnw test` runs the fast tests.
- `./mvnw verify` runs the full build, including the integration tests.

**Why.** It's the established standard for Java builds and the author's preference over Gradle (project log, 2026-10-13).

### TypeScript

**What it is.** JavaScript with **types** added. You declare what shape your data has (for example, that an event has a `startsAt` date and a list of attendees), and the compiler catches mistakes before the code runs. It compiles to plain JavaScript, which is what browsers actually run.

**What it does in CrewUp.** All frontend code is TypeScript. Because the API client is generated from the backend's OpenAPI contract, a change to the backend's data shape shows up as a type error in the frontend instead of a bug in production.

**Why.** Type errors are caught while you type instead of by users, and editors can autocomplete and refactor reliably.

### Node.js and npm

**What it is.** **Node.js** runs JavaScript outside a browser. **npm** is its package manager: it installs libraries and runs project scripts, all listed in `package.json`.

**What it does in CrewUp.** The app itself runs in the browser, not on Node. Node is used for the development tools: Vite, the test runners, the linter, and the API client generator. Commands such as `npm run dev`, `npm test` and `npm run gen:api` all go through npm.

**Why.** Nearly every frontend tool is built on Node. CrewUp pins a current LTS version.

---

## Backend

### Spring Boot

**What it is.** The most widely used Java framework for building web applications and APIs. **Spring** provides the building blocks (web requests, database access, security). **Spring Boot** wires them together with sensible defaults, so an application starts with little configuration.

**What it does in CrewUp.** It's the foundation of the backend. It receives HTTP requests, routes them to controller methods, connects to the database, applies security, and serves the REST API under `/api`. Settings live in `application.yml`, with local overrides in a gitignored `application-local.yml`.

**Why.** It's mature, well documented, very common in job postings, and every other backend piece (Modulith, Data JPA, Security, AI) is part of the same family.

### Spring Modulith

**What it is.** A library for Spring Boot applications that keeps a single application split into clearly separated **modules**, and checks that those modules only talk to each other in allowed ways.

**The problem it solves.** Large applications tend to become a "big ball of mud": any class can call any other class, so a change in one area breaks something unrelated. The usual escape is microservices (many small applications talking over the network), but those add a lot of operational work: separate deployments, network failures, distributed data. A **modular monolith** sits in between. It's one application, one deployment and one database, but with firm internal walls.

**What it does in CrewUp.**
- Each area of the app is a module: `identity`, `groups`, `events`, and one per Activity Type (`dining`, `boardgames`, `movies`).
- A module exposes a small public API. Everything else lives in an `internal` package that other modules can't use.
- A test calls Spring Modulith's `verify()`, which fails the build if, for example, the `dining` module imports something from `boardgames`. The architecture rule "Activity Types never depend on each other" is checked automatically, not just written down.
- Modules can react to each other through **application events**. For example, when a member is deactivated, other modules can respond without the `groups` module needing to know about them.

**Why.** It keeps the code base easy to change as Activity Types are added, without the cost of microservices (ADR-001).

### PostgreSQL

**What it is.** A free, open-source **relational database**, one of the most capable and widely used. Data is stored in tables with rows and columns, linked by keys, and queried with SQL.

**What it does in CrewUp.** It stores everything: users, groups, members, events, activities, reviews, game results. CrewUp also uses some of its extra features:
- **full-text search** for searching the timeline (M6)
- **JSONB** columns and arrays where a fixed table structure doesn't fit
- `timestamptz` columns, which store exact moments in time (see [Conventions](#conventions))

It runs in Docker during development and in Testcontainers during tests.

**Why.** Reliable, free, standards-compliant, and offered by every hosting platform.

### Spring Data JPA (and Hibernate)

**What it is.** **JPA** (Jakarta Persistence API) is the Java standard for **object-relational mapping (ORM)**: you write ordinary Java classes (**entities**), and the ORM turns them into database rows and back. **Hibernate** is the library that does the actual work. **Spring Data JPA** sits on top, so you only declare a **repository** interface and Spring writes the common queries for you.

**What it does in CrewUp.** Each module owns its entities and repositories. For example, `identity` has a `User` entity, and `UserRepository.findBySubject(...)` looks up the User for a sign-in token without any hand-written SQL. Custom queries use bound parameters, never string concatenation, which prevents SQL injection (and a project Semgrep rule enforces it).

**Why.** Much less boilerplate than hand-written SQL for everyday reads and writes, while still allowing SQL where it's the better tool.

### Flyway

**What it is.** A **database migration** tool. Each change to the database structure (create a table, add a column) is a numbered SQL script, such as `V3__add_events_table.sql`. On startup, Flyway records which scripts have already run and applies only the new ones, in order.

**What it does in CrewUp.** The database structure is built entirely from these scripts, so every developer, test run and deployment ends up with exactly the same schema. Each module has its own migration folder. A script that has already been applied is never edited; a fix is always a new script.

**Why.** Database changes are versioned, reviewed and repeatable like code, instead of being applied by hand.

### OpenAPI (springdoc-openapi and the generated client)

**What it is.** **OpenAPI** is a standard format for describing a REST API: every endpoint, its parameters, and the exact shape of its requests and responses. **springdoc-openapi** generates that description automatically from the Spring Boot controllers.

**What it does in CrewUp.**
1. The backend publishes its OpenAPI document.
2. `npm run gen:api` reads it and generates a TypeScript **API client**: ready-made, fully typed functions such as `getMe()`.
3. The frontend calls the backend only through that client, never with hand-written `fetch` calls.
4. In CI, a diff of the OpenAPI document catches changes that would break the frontend.

The generated client is never edited by hand. It's regenerated.

**Why.** The backend and frontend can't drift apart silently. A contract change becomes a compile error.

---

## Sign-in and security

### OIDC (OpenID Connect)

**What it is.** An industry standard for signing users in. It's built on **OAuth 2.0**, the standard for granting access with tokens. "Sign in with Google" and "Sign in with GitHub" are OIDC.

**What it does in CrewUp.** CrewUp doesn't build its own login screens or store passwords. A dedicated **identity provider** handles accounts, passwords and sign-in. When a user signs in, the provider issues tokens:
- an **ID token**, which says who the user is (for the web app to read)
- an **access token**, a signed, short-lived pass that the web app sends to the backend on every request

**Why.** Password storage, account recovery and brute-force protection are hard to get right and easy to get wrong. Using a standard also means the identity provider can be swapped (Keycloak now, a hosted provider at launch) without rewriting the app.

### JWT (JSON Web Token)

**What it is.** A compact, signed token format. A JWT holds a few facts (**claims**), such as who the user is (`sub`, the subject) and when the token expires. It's signed by the identity provider, so anyone with the provider's public key can check that it's genuine and hasn't been altered.

**What it does in CrewUp.** The access tokens are JWTs. The backend checks each token's signature and expiry without calling Keycloak on every request, then maps the `sub` claim to a CrewUp User.

### Spring Security

**What it is.** Spring's framework for authentication (who are you?) and authorization (what are you allowed to do?).

**What it does in CrewUp.** The backend is configured as an **OAuth2 resource server**: it accepts requests only with a valid JWT access token, and answers anything else with `401 Unauthorized`. From M2, **method security** checks group permissions on each endpoint, for example `@PreAuthorize("@groupAccess.can(#groupId, 'EVENT_EDIT')")`, based on the member's role in that group.

**Why.** It's the standard, heavily reviewed way to secure a Spring application. Rolling your own security is a classic source of vulnerabilities.

### SPA (Single-Page Application)

**What it is.** A web app that loads once, then updates the page with JavaScript as you use it, instead of asking the server for a new HTML page on every click. Gmail and Trello work this way.

**What it does in CrewUp.** The CrewUp web app is an SPA written in React. The server sends it data (JSON) through the API, and the SPA draws the screens.

**Why it matters for security.** An SPA runs entirely in the user's browser, so it can't keep a secret: anyone can open developer tools and read its code. In OAuth terms it's a **public client**. That's why it needs PKCE, described next.

### PKCE (Proof Key for Code Exchange, pronounced "pixy")

**What it is.** An extension to the OAuth sign-in flow that protects apps that can't keep a secret, such as SPAs and mobile apps.

**The problem it solves.** In the standard sign-in flow, the identity provider sends the browser back to the app with a one-time **authorization code**, and the app trades that code for tokens. If an attacker intercepts the code, they could trade it in first. Server-side apps prevent this with a client secret, but an SPA has nowhere safe to keep one.

**How it works.**
1. Before sign-in, the SPA makes up a random secret (the **code verifier**) and keeps it in memory.
2. It sends only a scrambled version (the **code challenge**, a SHA-256 hash) to the identity provider.
3. When trading the authorization code for tokens, it sends the original verifier. The provider hashes it and checks it against the challenge.

A stolen authorization code is useless without the verifier, which never left the browser.

**What it does in CrewUp.** The SPA signs in with "Authorization Code + PKCE", the current recommended flow for browser apps. Tokens are kept in memory only, not in `localStorage`, so a malicious script has fewer places to find them (M0 spec, decision 2).

### Keycloak

**What it is.** A free, open-source identity provider, maintained by Red Hat. It implements OIDC and provides login pages, user registration, password reset and an admin console.

**What it does in CrewUp.** During development, Keycloak runs in Docker alongside the database (`docker compose up -d`). It loads a committed `crewup` **realm** (Keycloak's term for an isolated set of users and settings) that already has the web app registered and a ready-to-use development user. A new developer can sign in without any manual setup.

**Why.** It's a real, standards-compliant OIDC provider that runs locally for free. Because CrewUp only depends on the OIDC standard, Keycloak can be replaced by a hosted provider when the app launches (ADR-004).

### Walkthrough: what happens when you sign in

1. You click **Sign in** in the CrewUp web app (the SPA).
2. The SPA creates a PKCE verifier and challenge, then redirects you to Keycloak's login page with the challenge.
3. You enter your email and password **on Keycloak's page**. CrewUp never sees your password.
4. Keycloak redirects you back to the SPA with a one-time authorization code.
5. The SPA sends the code plus the PKCE verifier to Keycloak and receives an ID token and an access token (OIDC).
6. The SPA calls `GET /api/me` on the backend with the access token (a JWT) attached.
7. Spring Security verifies the token, and the backend finds or creates your User record and returns your profile.
8. The SPA shows "Hello, {your name}".

### OWASP

**What it is.** The **Open Worldwide Application Security Project**, a non-profit that publishes free, widely used security guidance and tools. Its best-known publication is the **OWASP Top 10**, a regularly updated list of the most critical web application security risks, such as:
- **Broken access control:** users reaching data they shouldn't (for example, a member of one group reading another group's events)
- **Injection:** untrusted input changing the meaning of a database query or command (SQL injection)
- **Security misconfiguration**, **vulnerable dependencies**, **authentication failures**, and more

**What it does in CrewUp.** Requirement NFR-2 says the OWASP Top 10 is addressed. In practice:
- Group-scoped authorization is checked on every request, and tested per role.
- The CI static analysis rules include OWASP Top 10 rule sets.
- From M8, **OWASP ZAP** (a free security scanner from the same community) runs a baseline scan against the deployed app, checking things like security headers and cookie settings.

### CodeQL

**What it is.** GitHub's code analysis engine. It turns source code into a database and runs queries against it to find security vulnerabilities. It's free for public repositories.

**What makes it different.** CodeQL does **data-flow analysis**. It follows untrusted input (such as a request parameter) through the code, across methods and files, to see whether it reaches a dangerous place (such as a SQL query or the page's HTML) without being made safe first. That catches problems that a simple pattern search misses.

**What it does in CrewUp.** CodeQL runs in GitHub Actions on every push and pull request, for both the Java backend and the TypeScript frontend. Findings appear in the repository's **Security → Code scanning** tab, and serious ones fail the check (M0-15).

**Its siblings in CrewUp's CI:**
- **Semgrep:** a fast pattern-based scanner. CrewUp uses its community rules plus project-specific rules (for example, "never build SQL with string concatenation").
- **gitleaks:** scans commits for accidentally committed passwords, keys and tokens.
- **Dependabot:** GitHub's service that alerts on, and proposes updates for, libraries with known vulnerabilities.

---

## Logging and tracing

### Structured logging (SLF4J and ECS)

**What it is.** **SLF4J** is the standard Java logging interface; Spring Boot uses it out of the box. **Structured logging** writes each log line as JSON with named fields (`"http.status": 401`, `"traceId": "…"`) instead of free text, so log tools can search and filter by field. **ECS** (Elastic Common Schema) is a widely used standard for those field names, and Spring Boot can produce it with one setting.

**What it does in CrewUp.** Locally, logs are plain readable text. Outside local development, they're ECS JSON. Every API request writes exactly one summary line (method, route, status, duration), and no log line ever contains tokens, passwords, or personal data.

### Request tracing (W3C Trace Context, Micrometer Tracing, OpenTelemetry)

**What it is.** A **trace** is the record of one piece of work, such as one API request, identified by a random **trace ID**. **W3C Trace Context** is the web standard for passing that ID between systems, in an HTTP header called `traceparent`. **OpenTelemetry** is the open standard for tracing, and **Micrometer Tracing** is how Spring Boot uses it.

**What it does in CrewUp.** The web app puts a fresh `traceparent` on every API call. The backend continues that trace, so every log line it writes for the request carries the same trace ID, and so does any error response. When something fails, the user sees "Reference: {traceId}", and that one ID finds every related log line. Traces aren't sent to a tracing system yet; one can be added later without code changes.

---

## AI

AI features arrive in M7. The first one looks up board game details. Under the project rules, **AI only proposes, and the user always confirms** before anything is saved, and every AI feature has a manual alternative.

### Spring AI

**What it is.** A Spring library for calling large language models (LLMs) from Java. It gives one programming interface (`ChatClient`) for many model providers, much as Spring Data gives one interface for many databases.

**What it does in CrewUp.** Feature code talks only to `ChatClient`, never to a specific provider. Spring AI also handles:
- **tool calling:** the model can ask CrewUp to run a function, such as searching a board game database, and use the result
- **structured output:** the model's answer is turned into a typed Java object instead of free text

**Why.** It's Spring-native, so it fits the rest of the backend, and switching models is a configuration change instead of a rewrite.

### Claude and Ollama

**What they are.** **Claude** is Anthropic's family of LLMs, used through Anthropic's API (a paid, hosted service). **Ollama** is a free tool for running open-source LLMs on your own computer.

**What they do in CrewUp.** One setting, `crewup.ai.provider`, chooses the model for the whole deployment: `anthropic` (Claude, the default) or `ollama` (a local model). A small fixed **eval set** (sample questions with known correct answers) runs against both, so their quality can be compared (AI-0e).

**Why.** Claude gives the best results. Ollama allows free, offline development and a real comparison between hosted and local models.

---

## Frontend

### React

**What it is.** A JavaScript library, created at Meta, for building user interfaces out of **components**: small, reusable pieces such as `EventCard` or `AttendeePicker`. You describe what the screen should look like for the current data, and React updates the page efficiently when the data changes.

**What it does in CrewUp.** The entire web app is built from React components. Each Activity Type plugs its own components into a shared **registry** (`src/activity-types/<type>/`), so the core screens can show a dinner or a board game session without knowing the details of either.

**Why.** It's the most widely used UI library, with a huge ecosystem, and its skills carry over to the planned React Native mobile app.

### Vite

**What it is.** A build tool and development server for web apps (pronounced "veet", French for "quick").

**What it does in CrewUp.**
- **During development** (`npm run dev`), it serves the app and updates the browser almost instantly when you save a file (**hot module replacement**), without a full reload.
- **For production**, it bundles and minifies the TypeScript, CSS and assets into small static files that can be served from anywhere.

**Why.** It's fast, needs little configuration, and is the current standard starting point for React apps.

### React Router

**What it is.** The standard library for **routing** in React: matching the URL in the address bar to the screen to show.

**What it does in CrewUp.** It maps URLs such as `/groups/{groupId}/events/{eventId}` to pages, so pages can be bookmarked and shared, and the browser's back button works as expected, even though the app is a single page.

### TanStack Query

**What it is.** A library for fetching, caching and updating **server state** in a React app. Server state is data that lives on the backend, such as a group's events, as opposed to UI state such as "is this menu open". It was formerly called React Query.

**The problem it solves.** Fetching data by hand means writing the same code over and over: loading spinners, error handling, caching, retries, refetching when data goes stale, and keeping several screens in sync after a change.

**What it does in CrewUp.** Every API call goes through TanStack Query (a project rule). For example:
- The events list is fetched once and cached. Going back to it shows the cached list instantly while it refreshes in the background.
- After you log a dinner, the app tells TanStack Query that the events list is out of date, and it refetches automatically.

### React Hook Form and Zod

**What they are.** **React Hook Form** manages forms: tracking what's been typed, showing errors, and submitting. **Zod** describes the rules data must follow as a **schema** (for example, "rating is a whole number from 1 to 5; restaurant name is required") and checks values against it.

**What they do in CrewUp.** Every form uses the two together. The Zod schema states the rules once. React Hook Form uses it to show errors as you type, and TypeScript derives the form's types from the same schema. The backend still validates everything again, because anything sent from a browser can be tampered with.

**Why.** Forms are a large part of CrewUp (logging events, reviews, game results), and this pairing keeps them fast, typed and consistent.

### Tailwind CSS

**What it is.** A **utility-first** CSS framework. Instead of writing separate style sheets with custom class names, you style elements with small, single-purpose classes directly in the markup: `class="p-4 text-lg font-bold rounded-md"`.

**What it does in CrewUp.** All styling uses Tailwind. Its responsive prefixes (`md:`, `lg:`) make the mobile-first layout straightforward: the app must be usable at a 360 px phone width.

**Why.** Consistent spacing and colors, no naming debates, and no unused CSS (Tailwind ships only the classes the app actually uses).

### shadcn/ui

**What it is.** A collection of well-designed, accessible React components (buttons, dialogs, forms, dropdowns) built with Tailwind. Unusually, it isn't installed as a library: a command **copies the component source code into the project**, where it becomes CrewUp's own code to read and change.

**What it does in CrewUp.** It provides the building blocks of the UI. The components are built on accessible primitives, which helps meet the WCAG 2.2 AA accessibility requirement (NFR-4): keyboard navigation, focus handling and screen-reader labels.

**Why.** Good-looking, accessible components without being locked into a library's design or upgrade cycle.

### Recharts

**What it is.** A charting library for React. Charts are built from components such as `<BarChart>` and `<Line>`.

**What it does in CrewUp.** It draws the look-back views, starting with the board game win chart (M5): who has won most often, and how that has changed over time.

### PWA (Progressive Web App)

**What it is.** A website that can be **installed** like an app: it gets a home-screen icon, opens in its own window, and can keep working with a poor connection. It's still the same web app underneath, with no app store involved.

**What it does in CrewUp.** The web app is designed mobile-first and will become installable as a PWA (NFR-3), so friends can open CrewUp from their phone's home screen at the restaurant or game table. A native mobile app (React Native) may follow later.

---

## Testing

CrewUp tests at several levels, from small and fast to broad and slow. Test names reference the requirement they prove, such as `M0_4_unauthenticatedApiCallReturns401`.

### JUnit 5

**What it is.** The standard testing framework for Java.

**What it does in CrewUp.** Every backend test is a JUnit test, from small **unit tests** of domain rules (for example, "copying an event shifts every activity's time by the same amount") to integration tests that start the whole application.

### Testcontainers

**What it is.** A Java library that starts real services, such as databases, in throwaway Docker containers during a test, and removes them afterward.

**What it does in CrewUp.** Integration tests run against a real PostgreSQL (and, where needed, a real Keycloak) instead of a fake in-memory database. Flyway migrations, queries and security are tested exactly as they'll run in production.

**Why.** Tests against fakes can pass while the real thing fails. With Testcontainers, the only thing a developer needs installed is Docker.

### Vitest

**What it is.** A test runner for JavaScript and TypeScript, built to work with Vite.

**What it does in CrewUp.** It runs all frontend unit and component tests (`npm test`). It shares Vite's configuration, so tests understand the code exactly as the app does.

### React Testing Library

**What it is.** A library for testing React components **the way a user sees them**: finding elements by their visible text or accessible label ("the button labeled Save") instead of by internal details.

**What it does in CrewUp.** Component tests render a piece of the UI, such as the attendee picker, interact with it, and check what appears. Because tests find elements by accessible name, they also catch some accessibility problems, such as an unlabeled button.

### MSW (Mock Service Worker)

**What it is.** A library that intercepts the app's network requests and returns pre-written responses, without changing the app's code.

**What it does in CrewUp.** Frontend component tests run without a real backend. MSW answers `GET /api/me` and other calls with realistic sample data, including error responses, so the tests are fast and predictable.

### Playwright

**What it is.** A tool from Microsoft for **end-to-end testing**. It drives real browsers (Chromium, Firefox, WebKit) the way a person would: opening pages, clicking buttons, filling in forms, and checking what appears.

**What it does in CrewUp.** Playwright tests cover complete user journeys across every part of the system at once: the browser, the SPA, Keycloak, the backend and the database. The first one (M0) signs in through Keycloak and checks that the home page greets the user. Later ones cover golden paths such as "create a group → invite a friend → log a dinner → see it on the timeline". They run locally in M0 and join CI in M8.

**Why.** Unit and component tests check pieces in isolation. Only an end-to-end test proves the pieces actually work together.

---

## Code quality

CrewUp's coding rules are in `coding-standards.md`. These tools enforce the mechanical ones automatically, so code reviews can focus on what needs judgment.

### Spotless and Palantir Java Format

**What they are.** **Palantir Java Format** is an automatic formatter for Java: it decides indentation, line breaks and spacing, so there's exactly one correct layout for any code. **Spotless** is a build plugin that runs formatters.

**What they do in CrewUp.** `./mvnw spotless:apply` reformats the backend code. `./mvnw verify` (and therefore CI) fails if any file isn't formatted.

### ESLint and Prettier

**What they are.** **ESLint** is a **linter** for JavaScript and TypeScript: it finds likely bugs and rule violations, such as a React hook used incorrectly or a button with no accessible label. **Prettier** is an automatic formatter for TypeScript, CSS, JSON and Markdown.

**What they do in CrewUp.** `npm run lint` and `npm run format` run them locally, and CI checks both on every pull request.

### JaCoCo and Vitest coverage

**What they are.** **Code coverage** tools record which lines and branches of the code run while the tests run. **JaCoCo** does this for Java; **Vitest's coverage** (using the V8 JavaScript engine's built-in counters) does it for the frontend.

**What they do in CrewUp.** The build fails if tests exercise less than 80% of the lines or 70% of the branches (each `if`/`else` path), on either side. CI shows a coverage table on every workflow run. Coverage only proves code *ran*, not that a test checked the result, so it's a safety net next to the requirement-named tests, not a replacement for them.

### axe

**What it is.** An accessibility testing engine from Deque. It checks a rendered page against the WCAG rules: images without text alternatives, inputs without labels, buttons without names, invalid ARIA, poor color contrast, and more.

**What it does in CrewUp.** Component tests run axe on each page, and the Playwright test runs it in a real browser at desktop and phone widths. Any violation fails the test. This backs up the WCAG 2.2 AA requirement (NFR-4) with real checks, beyond what a linter can see in the source code.

### The `/review-standards` skill

**What it is.** A Claude Code **skill**: a set of instructions, stored in the repository under `.claude/skills/`, that Claude Code follows when you type `/review-standards`.

**What it does in CrewUp.** Before a pull request is opened, it reviews the branch's changes against the rules in `coding-standards.md` that need judgment, and reports each finding with its rule ID. The pull request template asks for confirmation that it was run.

---

## Running and delivering

### Docker and Docker Compose

**What they are.** **Docker** runs software in **containers**: lightweight, isolated packages that include everything a program needs, so it runs the same on any machine. **Docker Compose** starts several containers together from one file, `docker-compose.yml`.

**What they do in CrewUp.** `docker compose up -d` starts PostgreSQL and Keycloak, already configured. Nobody installs a database or identity provider by hand. Testcontainers also uses Docker during tests.

**Why.** "Clone, run one command, start coding" (US-1).

### GitHub Actions

**What it is.** GitHub's built-in automation service. **Workflows**, described in YAML files under `.github/workflows/`, run automatically on events such as a push or a pull request.

**What it does in CrewUp.** The **CI (continuous integration)** workflow runs on every push and pull request to `main`. It builds the backend and frontend, runs the tests (including Testcontainers and Modulith checks), lints and type-checks, and runs the security scans. A failure blocks the merge.

**Why.** Every change is checked the same way, automatically, before it reaches `main`.

---

## Conventions

These are small project-wide decisions that show up throughout the code.

- **UUID v7 IDs.** Every record's ID is a UUID: a 128-bit random-looking value such as `0192f1e4-7c3a-7b2e-9f4d-3a1b2c4d5e6f`. Unlike sequential numbers (1, 2, 3…), UUIDs don't reveal how many records exist and can't be guessed by counting. **Version 7** UUIDs start with a timestamp, so new IDs sort in creation order, which keeps database indexes efficient.
- **Times stored as `timestamptz`, with the time zone on the Event.** Every moment is stored as an exact instant. Each Event also stores its **IANA time zone** name (such as `America/Chicago`), so "dinner at 7 pm" still displays as 7 pm local time when friends are in different zones or daylight saving time changes.
- **RFC 9457 Problem Details for errors.** Every API error has the same standard JSON shape (`type`, `title`, `status`, `detail`), so the frontend handles all errors one way.
- **Group-scoped URLs.** Every group's data lives under `/api/groups/{groupId}/…`, and every endpoint checks the caller's role in that group.
- **Optimistic locking with `ETag` / `If-Match`.** If two people edit the same event at once, the second save is rejected instead of silently overwriting the first person's changes.
- **Conventional Commits.** Commit messages follow a standard format, such as `feat(dining): add restaurant reviews [DIN-3]`, so the history is easy to scan and every change links to its requirement.

---

## Planned for later

- **MinIO (Phase 2):** S3-compatible file storage that runs locally in Docker, for event photos. Production would use a hosted equivalent such as Amazon S3.
- **Hosted identity provider (M8):** replaces Keycloak at launch and adds social sign-in (Google, GitHub). Chosen in ADR-004.
- **Hosting platform (M8):** for example Fly.io, Render or AWS. Chosen in ADR-005.
- **React Native with Expo (Phase 3):** a native mobile app that reuses CrewUp's React skills and the same API.

---

## Quick reference

| Term | One-line summary |
|---|---|
| Java | The backend's programming language (version 25, LTS) |
| Maven | Builds and tests the backend, run through `./mvnw` |
| TypeScript | JavaScript with types, used for all frontend code |
| Node.js / npm | Run the frontend's development tools and scripts |
| Spring Boot | The backend framework: web, data, security, configuration |
| Spring Modulith | Keeps one Spring Boot app split into modules and checks the boundaries in tests |
| PostgreSQL | The relational database |
| Spring Data JPA / Hibernate | Maps Java classes to database tables |
| Flyway | Applies versioned database migration scripts |
| OpenAPI | The API contract the frontend client is generated from |
| OIDC | The standard for signing users in with an identity provider and tokens |
| JWT | The signed token format of CrewUp's access tokens |
| Spring Security | Checks tokens and permissions on every API call |
| SPA | A web app that loads once and updates the page with JavaScript |
| PKCE | Protects the sign-in flow for apps that can't keep a secret |
| Keycloak | Open-source identity provider used for development |
| OWASP | Non-profit behind the Top 10 web security risks and the ZAP scanner |
| CodeQL | GitHub's data-flow security scanner, run in CI |
| Structured logging / ECS | Logs as JSON with standard field names, outside local development |
| W3C Trace Context / OpenTelemetry | One trace ID follows a request from the browser through the backend logs |
| Spring AI | One Java interface for calling AI models |
| Claude / Ollama | Hosted AI model (default) / local AI models (alternative) |
| React | Builds the UI out of reusable components |
| Vite | Fast dev server and production bundler for the frontend |
| React Router | Maps URLs to screens |
| TanStack Query | Fetches, caches and refreshes server data in React |
| React Hook Form + Zod | Forms, plus one schema for validation rules and types |
| Tailwind CSS | Styling with small utility classes in the markup |
| shadcn/ui | Accessible React components copied into the project as source |
| Recharts | React charts, such as the board game win chart |
| PWA | A web app that can be installed on a phone's home screen |
| JUnit 5 | The Java testing framework |
| Testcontainers | Real databases in throwaway containers during tests |
| Vitest | Runs the frontend tests |
| React Testing Library | Tests components the way a user sees them |
| MSW | Fakes API responses in frontend tests |
| Playwright | Drives real browsers to test full user journeys |
| Spotless / Palantir Java Format | Formats the Java code automatically and checks it in the build |
| ESLint / Prettier | Lints and formats the frontend code |
| JaCoCo / Vitest coverage | Fail the build if tests exercise too little of the code |
| axe | Checks rendered pages for accessibility problems |
| `/review-standards` | Claude Code skill that reviews a branch against the coding standards |
| Docker / Compose | Runs PostgreSQL and Keycloak locally with one command |
| GitHub Actions | Runs CI on every push and pull request |

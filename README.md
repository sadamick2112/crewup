# CrewUp

A shared memory log for long-running friend groups: plan get-togethers, log what happened (dinners, board games, movies), and look back on it all. It's a portfolio and learning project: a Spring Boot modular monolith and a React web app, built spec first with Claude Code.

**Status:** Milestone 0 (foundation). You can sign up, sign in, and get greeted by name with data from an authenticated API call. Crews and activities arrive from M1 (see [docs/roadmap.md](docs/roadmap.md)).

New to the stack? The [Technology Guide](docs/technology-guide.md) explains each technology in plain language and what it does in CrewUp.

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| Docker with Compose v2 | Docker 29, Compose v5 tested | Runs PostgreSQL and Keycloak, the backend's Testcontainers tests, and the local security scans |
| Java (JDK) | 25 | For example Temurin through [SDKMAN](https://sdkman.io/). Maven isn't needed: the repo includes the Maven Wrapper (`./mvnw`) |
| Node.js | 24.22.0 (in `.nvmrc`) | For example through [nvm](https://github.com/nvm-sh/nvm): `nvm install` in the repo root |
| Python 3 | any recent | Optional: only for `infra/keycloak/get-dev-token.py` (manual API calls) |

Developed on Ubuntu in WSL 2 with Docker Desktop. Linux and macOS work the same way.

## Run locally

From a fresh clone:

```sh
# 1. Local settings (gitignored). The defaults work as they are.
cp .env.example .env

# 2. PostgreSQL and Keycloak. Keycloak imports the dev realm from infra/keycloak/ on startup.
docker compose up -d
docker compose ps          # wait until both show "healthy" (Keycloak takes about 30 seconds)

# 3. Backend on http://localhost:8080 (Flyway migrates the database on startup). Leave it running.
cd backend
./mvnw spring-boot:run

# 4. Frontend on http://localhost:5173, in a second terminal. It proxies /api to the backend.
cd frontend
npm ci
npm run dev
```

Open http://localhost:5173, choose **Sign in**, and sign in as the dev user below (or use **Register** on the Keycloak page to create your own account). The home page greets you by name.

To stop: Ctrl+C in each terminal, then `docker compose down`. That resets Keycloak to the committed realm (accounts you registered are gone); the database keeps its data in a Docker volume.

### Dev user (DEV ONLY)

| | |
|---|---|
| Email | `dev@crewup.local` |
| Password | `dev` |

This account, the Keycloak admin login (`admin` / `admin-dev-only`, console at http://localhost:8180/admin) and every value in `.env.example` exist for local development only. Never use them anywhere else.

### Ports

Backend 8080, frontend 5173, Keycloak 8180, PostgreSQL 5432. Change them in `.env` if one is taken (`CREWUP_BACKEND_PORT`, `KEYCLOAK_PORT`, `CREWUP_DB_PORT`). If you change the Keycloak port, change `CREWUP_OIDC_ISSUER_URI` to match.

### Useful URLs

- Health: http://localhost:8080/actuator/health (public; shows the database status)
- API docs: http://localhost:8080/swagger-ui.html. Use **Authorize** with a token from `python3 infra/keycloak/get-dev-token.py`.

## Tests and checks

| | Command | Where |
|---|---|---|
| Backend build, tests, coverage, formatting check | `./mvnw verify` | `backend/` |
| Fix Java formatting | `./mvnw spotless:apply` | `backend/` |
| Frontend lint, format check, types, tests with coverage | `npm run lint && npm run format:check && npm run typecheck && npm run test:coverage` | `frontend/` |
| End-to-end sign-in test (needs Compose and the backend running) | `npm run e2e` | `frontend/` |

The backend tests start their own PostgreSQL with Testcontainers, so they only need Docker, not `docker compose up`.

**First end-to-end run:** Playwright needs its browser, and on a minimal Linux (such as WSL Ubuntu) some system libraries. Once per machine, in `frontend/`:

```sh
npx playwright install chromium
sudo env "PATH=$PATH" npx playwright install-deps chromium   # env PATH keeps nvm's Node visible to sudo
```

## Security scans

Every pull request to `main` and every push to `main` runs these checks in GitHub Actions. Results appear under the repository's **Security → Code scanning** tab.

| Scan | What it looks for | Workflow |
|---|---|---|
| **Semgrep** | Insecure patterns in Java, Spring, TypeScript and React code (OWASP Top 10 rules), plus CrewUp's own rules in `.semgrep/` (SQL built from strings, `dangerouslySetInnerHTML`, field injection, legacy date types, log string concatenation, `UUID.randomUUID()` for IDs). Error-level findings fail the check. | `security.yml` |
| **CodeQL** | Security flaws found by data-flow analysis across methods (Java and TypeScript, `security-extended` queries) | `codeql.yml` |
| **gitleaks** | Secrets committed anywhere in the history of the change | `security.yml` |
| **Dependabot** | Known-vulnerable dependencies, and weekly version updates for Maven, npm, GitHub Actions and Docker images | `dependabot.yml` |

A suppressed or dismissed finding always carries its reason next to it. For example, CSRF protection is off for the stateless bearer-token API on purpose; see [ADR-004](docs/adr/004-oidc-provider.md).

### Run the scans locally

All three run through Docker, from the repo root, with the same image versions as CI:

```sh
# Semgrep with the project rules (add --error to exit non-zero on a finding)
docker run --rm -v "$PWD:/src" -w /src semgrep/semgrep:1.182.0 semgrep scan --metrics=off --config .semgrep

# The full CI scan: registry rulesets plus the project rules
docker run --rm -v "$PWD:/src" -w /src semgrep/semgrep:1.182.0 semgrep scan --metrics=off \
  --config p/java --config r/java.spring --config p/typescript --config p/react --config p/owasp-top-ten \
  --config .semgrep

# Test the project rules against their fixtures (mounted as /rules: semgrep --test skips hidden folders)
docker run --rm -v "$PWD/.semgrep:/rules:ro" semgrep/semgrep:1.182.0 semgrep --test --metrics=off /rules

# gitleaks over the whole history (add --log-opts="origin/main..HEAD" for this branch only)
docker run --rm -v "$PWD:/repo" ghcr.io/gitleaks/gitleaks:v8.30.1 git /repo --redact --verbose
```

Semgrep scans only files tracked by git, so add a new file (`git add`) before scanning it.

### Optional: block secrets before they're committed

A local git pre-commit hook can run gitleaks on the staged changes, so a secret never reaches a commit:

```sh
cat > .git/hooks/pre-commit <<'EOF'
#!/bin/sh
# gitleaks on the staged changes (M0-17). Skip once with: git commit --no-verify
exec docker run --rm -v "$(git rev-parse --show-toplevel):/repo" ghcr.io/gitleaks/gitleaks:v8.30.1 git /repo --staged --redact
EOF
chmod +x .git/hooks/pre-commit
```

## Project docs

- [docs/requirements.md](docs/requirements.md): what CrewUp does, with requirement IDs
- [docs/architecture.md](docs/architecture.md): modules, the Activity Type plug-in contract, security
- [docs/roadmap.md](docs/roadmap.md): milestones
- [docs/coding-standards.md](docs/coding-standards.md): coding rules and how they're checked
- [docs/specs/](docs/specs/): spec, plan and tasks per feature
- [docs/adr/](docs/adr/): architecture decision records
- [docs/project-log.md](docs/project-log.md): decisions and context from each working session
- [CLAUDE.md](CLAUDE.md): instructions for Claude Code

## License

[Apache License 2.0](LICENSE)

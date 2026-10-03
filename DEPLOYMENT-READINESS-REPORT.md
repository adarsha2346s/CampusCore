# CampusCore Deployment Readiness Report

## Current Release

- Branch: `master`.
- Release commit: `f4c8ac9d850af9d80e3ad737b879a63bd53b21f3` (`Finalize CampusCore public release`).
- Remote: `origin` points to the CampusCore GitHub repository.
- Repository status: release commit is checked out and tracks `origin/master`; one untracked local file, `GITHUB-PUSH-REPORT.md`, is present. It was not modified or staged. No source/configuration changes were made for this audit.
- The frontend production build completed successfully during this review. The backend container image build was attempted and failed; details are below.

## Frontend

- Build status: `npm run build` passed (`tsc -b` and Vite production build). The generated `frontend/dist/` output is ignored.
- Production API configuration: `frontend/src/lib/api/http.ts` reads `VITE_API_BASE_URL`; if unset, it falls back to `http://localhost:8080/api/v1`. The production build must receive the real deployed API base URL at build time. No production URL is currently configured or known.
- Authentication calls use the shared configured API client. Authenticated requests send a Bearer token in the `Authorization` header. The token is held in frontend memory, not browser storage.
- SPA routing: `frontend/vercel.json` rewrites paths to `/index.html`, which supports Vite client-side routes on Vercel. Vercel's Vite guidance describes static production output and build-time environment variables: [Vercel Vite documentation](https://vercel.com/docs/frameworks/frontend/vite), [Vercel project configuration](https://vercel.com/docs/project-configuration).
- Hosting compatibility: Vercel is a practical frontend target. Set `VITE_API_BASE_URL` in the Vercel build environment; it is public client configuration, not a place for secrets.
- Frontend Dockerfile limitation: `frontend/Dockerfile` runs the Vite development server (`npm run dev`), so it is not a production static-serving image. Use Vercel/static hosting, or separately choose a production static server if container hosting is required.
- Blockers: the production API URL must be supplied after the backend has a stable public URL. The backend Docker build currently fails, blocking the repository's configured Docker-based backend deployment path.

## Backend

- Production configuration exists at `backend/src/main/resources/application-production.properties`.
- Datasource URL, username, and password are required from `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. The production configuration contains no local Docker hostname or localhost datasource fallback.
- JWT signing secret is required from `JWT_SECRET`. `JWT_EXPIRATION` is configurable and has a 24-hour default.
- `CORS_ALLOWED_ORIGINS` is required by the production properties. Configure it to the exact HTTPS origin of the deployed frontend (no domain is known yet). Use a comma-separated list only when multiple explicit origins are needed; do not use `*`.
- Port: `server.port=${PORT:8080}` supports a host-provided `PORT` and falls back to 8080.
- The production profile sets Hibernate to validate the existing schema and disables SQL display/format logging. It does not auto-create schema or load the demo seed.
- JWT authentication, Bearer API calls, role rules, and the existing 401/403 behavior are already implemented. Security configuration enables CORS for API routes and handles preflight requests. Set `CORS_ALLOWED_ORIGINS` to the final frontend origin so login and authenticated browser calls can pass preflight checks.
- Docker status: a local-only image build was attempted with the repository's `backend/Dockerfile`; it failed before Maven dependency resolution. The Dockerfile copies `backend/.mvn` to `/workspace/backend/.mvn`, while `backend/mvnw` looks for `.mvn/wrapper/maven-wrapper.properties` relative to `/workspace`. The metadata is therefore absent at the path the wrapper expects. This is a confirmed deployment blocker for the Docker image path used by `render.yaml` and other Dockerfile-based hosting. No file was changed. Render's Docker runtime uses a configured Dockerfile/build context, so it will encounter this repository build failure until the copy path is corrected: [Render Blueprint specification](https://render.com/docs/blueprint-spec).
- Health/startup: no Spring Actuator dependency or HTTP health endpoint was found. Startup requires a reachable database and schema compatible with Hibernate validation. Render supports TCP checks by default and optional HTTP health checks; without an application health endpoint, use provider TCP/startup checks or add a health endpoint in a future separately approved change: [Render health checks](https://render.com/docs/health-checks).
- The local-only `backend/src/main/resources/application.properties` exists but is ignored and is not tracked. Its values were not read or printed.

## Database

- `backend/database/schema.sql` defines 12 tables and 15 foreign-key clauses. It begins with `USE campuscore;`, so create/select the intended fresh database named `campuscore` before applying it. The schema includes the user/profile, department/course, enrollment, assessment/marks, attendance, grading, and audit relationships.
- MySQL 8.4 compatibility has previously been exercised in the isolated demo runtime verification. This audit did not connect to or modify any database.
- Production initialization is manual: create a fresh hosted MySQL-compatible database, apply `schema.sql` once to that database, then configure the backend. Hibernate is `validate`, not a schema migration/creation mechanism. No migration framework was found in the inspected configuration.
- `backend/database/demo-data.sql` is optional synthetic demo data. It is insert-only and contains no `DROP`, `TRUNCATE`, or broad `DELETE` statements, but it creates demo accounts/data and must not be run against production. Docker Compose initializes the schema, not the demo seed.
- Do not use the demo seed as production data. The schema itself creates no user rows, and the repository does not document a production first-admin bootstrap procedure. Before real users can use a fresh production deployment, define a controlled, private initial-admin provisioning process; do not expose public registration or enable the demo seed in production.
- Keep production and local/demo databases separate. Railway documents a MySQL service/template and private service variables; if selected, map its database values to the application's `DB_*` variables without publishing them: [Railway MySQL](https://docs.railway.com/databases/mysql).

## Security

- Production DB and JWT values are environment-driven. No real production credentials were found in the inspected tracked configuration.
- The tracked local `application.properties` is absent from Git and covered by the root ignore rule. `.env`, frontend `.env`, build output, Maven target, IDE/OS files, private key extensions, keystores, and truststores are covered by ignore rules.
- Static scanning did not find a committed private-key block, credential-bearing database URL, or JWT-shaped token. The scan did find synthetic BCrypt password hashes in `backend/database/demo-data.sql` (intended demo account material); hashes were not printed. Keep this optional demo seed out of production database initialization. Placeholder variable assignments in example/CI/config files are not actual credentials.
- `render.yaml` declares production secrets as unsynchronized environment values rather than embedding values. `.env.example` uses placeholders; `frontend/.env.example` contains only the local API URL. No secret values are reproduced here.
- CORS must be restricted to the real deployed frontend origin. Never configure an unrestricted wildcard for the credential-bearing API.
- The untracked `GITHUB-PUSH-REPORT.md` is a local internal artifact and is not part of the release commit; keep internal reports out of public deployment artifacts.
- Result: production configuration is designed for external secrets, but production safety still depends on setting correct values, restricting CORS, excluding demo data, and provisioning the initial admin safely.

## CI/CD

- `.github/workflows/ci.yml` runs on push and pull request.
- Backend job provisions ephemeral `mysql:8.4`, uses Temurin Java 25, initializes `schema.sql`, and runs `./mvnw -B clean test`. A missing CI JWT secret is replaced at runtime with a temporary random value; no long-lived secret is required for that fallback.
- Frontend job uses Node 24, `npm ci`, `npm run lint`, and `npm run build`.
- The workflow is suitable for current project checks. It is CI, not deployment automation; it does not publish images or deploy services.
- The configured Java/Node versions and test commands match the project configuration. Current release history records backend tests 8/8 and frontend lint/build as passing; the frontend build was also run successfully during this preparation. The Docker image build failure is independent of those checks because CI does not build the Dockerfile.

## Environment Variables

Names only are listed below; no values are included.

| Variable | Required for production | Consumed by | Currently documented | Production action |
| --- | --- | --- | --- | --- |
| `DB_URL` | Yes | Spring datasource | Yes | Supply hosted MySQL JDBC URL. |
| `DB_USERNAME` | Yes | Spring datasource | Yes | Supply a least-privilege database user. |
| `DB_PASSWORD` | Yes | Spring datasource | Yes | Supply through host secret management. |
| `JWT_SECRET` | Yes | JWT signing | Yes | Supply a private random value meeting the documented minimum length. |
| `JWT_EXPIRATION` | No; defaults to 24 hours | JWT configuration | Yes | Set only if a different lifetime is intended. |
| `CORS_ALLOWED_ORIGINS` | Yes in production profile | Spring Security CORS | Yes | Set the final frontend origin(s), exactly. |
| `PORT` | Host-dependent; app defaults to 8080 | Spring Boot server | Not prominently listed in README | Allow the platform-provided port to reach the application. |
| `SPRING_PROFILES_ACTIVE` | Yes to select production properties outside a provider that sets it | Spring profile selection | Set in `render.yaml`; not listed in README env table | Set to the production profile on the backend service. |
| `VITE_API_BASE_URL` | Yes for production frontend build | Vite frontend API client | Yes | Set to the deployed API base URL before building the frontend. It is public browser configuration. |

The root `.env.example` is for local Compose and contains placeholders. `frontend/.env.example` is local-only configuration. Neither provides production values.

## Recommended Deployment Architecture

`React + Vite static frontend (Vercel) → Spring Boot REST API (Docker service) → private hosted MySQL 8.4-compatible database`

Vercel's documented Vite workflow fits the static frontend and existing SPA rewrite. Railway documents MySQL and Dockerfile-based application builds; colocating the backend and MySQL in a private Railway project is a practical option, subject to account/plan/region and current service limits. Railway's Compose guide maps Compose services into Railway services rather than running the repository's Compose file directly: [Railway Dockerfiles](https://docs.railway.com/builds/dockerfiles), [Railway Compose guide](https://docs.railway.com/guides/docker-compose). Render is also represented by `render.yaml`, but it uses the same currently failing backend Dockerfile; its Blueprint specification lists managed Render databases as PostgreSQL, so a MySQL deployment there would require a separately sourced compatible database and verified networking. Verify provider pricing, region availability, persistent database backups, network access, and current plans before choosing.

No hosting account was accessed, no platform behavior was exercised, and no deployment occurred.

## Exact Deployment Sequence

1. Select hosting providers and regions; provision a new, private MySQL 8.4-compatible production database. Do not reuse local/demo databases.
2. Create the empty application database with the name expected by `schema.sql` and a least-privilege application user.
3. Apply `backend/database/schema.sql` once to that fresh production database after confirming its target. Do not apply `demo-data.sql`.
4. Establish a controlled initial ADMIN provisioning process for the empty production schema before inviting users. Keep it private and audit the operation.
5. Resolve the backend Docker build blocker in a separately approved change, then build the backend image locally/through CI and verify the same production profile path.
6. Create the backend service and set `SPRING_PROFILES_ACTIVE`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, and `CORS_ALLOWED_ORIGINS` using the provider's secret/environment interface. Set `JWT_EXPIRATION` only if overriding its default; let the host provide `PORT` when applicable.
7. Deploy the backend service after its image build succeeds. Confirm startup connects to the hosted database and validates the schema.
8. Obtain the backend's actual HTTPS API base URL. Do not guess or hardcode a domain in source.
9. Set `CORS_ALLOWED_ORIGINS` to the final frontend HTTPS origin (and only explicitly required preview origins).
10. Set Vercel's `VITE_API_BASE_URL` to the actual backend `/api/v1` base before the production build.
11. Deploy the frontend using Vite's production build and the existing SPA rewrite configuration.
12. Confirm browser requests reach the deployed API and CORS preflight succeeds.
13. Verify login and `/api/v1/auth/me`, then verify ADMIN, FACULTY, and STUDENT flows with separately provisioned accounts.
14. Verify dashboards, GPA, role boundaries, anonymous 401, wrong-role 403, logout/session clearing, and production error responses.
15. Inspect built assets/runtime requests to ensure no localhost/demo URL or demo account is used; verify backups, monitoring, and restore procedure for the hosted database.

These are recommendations only; none of the steps above was executed as a deployment.

## Blockers

### Deployment blockers

- `backend/Dockerfile` fails its local image build because the Maven wrapper metadata is copied to a different path than `backend/mvnw` expects. The configured Render backend deployment depends on that Dockerfile. No source/configuration fix was made under the preparation-only scope.
- The production frontend requires `VITE_API_BASE_URL` after a backend URL exists; the backend requires final `CORS_ALLOWED_ORIGINS`. Neither domain exists yet because nothing has been deployed.
- A fresh schema contains no ADMIN or other application users. A safe production first-admin provisioning procedure is not documented; do not use demo seed data for this.

### Non-blocking limitations

- No HTTP health endpoint/Actuator configuration was found; hosted service health may rely on TCP/startup checks until a health endpoint is deliberately added.
- No deployment or live provider/database integration was performed. Provider account access, pricing, network reachability, backups, and service availability remain to be verified at deployment time.
- JWTs are memory-only and users must log in again after a page reload; there is no refresh-token endpoint, as documented in README.
- `frontend/Dockerfile` is a development server image, not a production static-serving image; the recommended frontend target is static Vercel hosting.
- One untracked internal `GITHUB-PUSH-REPORT.md` remains in the local working tree; it is not tracked or part of the release commit.

## Production Smoke Test Checklist

- [ ] Public landing/login page loads over HTTPS.
- [ ] Login succeeds for the separately provisioned ADMIN account; `/api/v1/auth/me` returns the authenticated profile.
- [ ] Admin dashboard and permitted Admin directories/modules load from live API data.
- [ ] Faculty login, self profile, and only supported Faculty workspace pages load.
- [ ] Student login, self profile, own enrollments, dashboard, and GPA load for real linked records.
- [ ] Student ownership checks reject attempts to read another student's profile/enrollment/GPA.
- [ ] Anonymous protected API request returns 401; authenticated wrong-role request returns 403.
- [ ] Browser CORS preflight and authenticated API calls succeed only from the deployed frontend origin.
- [ ] Logout clears the client session; 401 clears expired session and 403 does not log out a valid user.
- [ ] API errors do not expose stack traces, credentials, SQL details, or tokens.
- [ ] Responsive layout is checked on desktop, tablet, and mobile.
- [ ] Built frontend and API requests contain no localhost URL, demo account, or local-only configuration.
- [ ] Database backups and restore procedure are confirmed; no demo seed is present in production.

## Final Status

**BLOCKED**

Production deployment is not ready to execute through the repository's configured Docker backend path: the backend image build demonstrably fails at Maven wrapper startup. A production frontend origin/backend URL and a controlled initial-admin provisioning procedure are also still outstanding. The frontend production build succeeds, production properties externalize secrets and database settings, and no deployment was performed. No project source/configuration was changed; only this review report was created.

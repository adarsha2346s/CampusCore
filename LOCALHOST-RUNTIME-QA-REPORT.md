# CampusCore — Localhost Runtime QA Report

## Root cause

No current localhost startup or API connectivity blocker was found. The earlier `admin01` HTTP 401 was due to its invalid/unknown local password; the local account credential had already been reset in the preceding authorized QA task. In this pass, login succeeds. CORS accepts both Vite development origins, so no CORS or application-code fix was needed.

## Files inspected

- `frontend/package.json`
- `frontend/vite.config.ts`
- `frontend/.env.example`
- `frontend/src/lib/api/http.ts`
- `frontend/src/features/auth/auth.api.ts`
- `frontend/src/features/auth/auth-context.tsx`
- `frontend/src/features/auth/session.ts`
- `frontend/src/features/admin-dashboard/AdminDashboardPage.tsx`
- `frontend/src/lib/role-home.ts`
- `backend/pom.xml`
- `backend/src/main/java/com/campuscore/backend/controller/AuthController.java`
- `backend/src/main/java/com/campuscore/backend/service/AuthService.java`
- `backend/src/main/java/com/campuscore/backend/config/SecurityConfig.java`
- `backend/src/main/resources/application.properties` (datasource target inspected without revealing credential values)
- `backend/src/main/resources/application-production.properties`

## Files changed

- Created `LOCALHOST-RUNTIME-QA-REPORT.md`.
- No frontend/backend source or configuration files were changed.
- No database changes were made during this pass.

## Backend status

The existing CampusCore Spring Boot process was already running on port 8080 and connected to the local CampusCore MySQL database. `GET http://localhost:8080/` returned HTTP 401, expected for the secured root path. Anonymous `GET /api/v1/auth/me` returned HTTP 401. The backend command used for a fresh start is `cd backend && ./mvnw -B spring-boot:run`.

## Frontend status

The existing Vite server was already running on port 5173; it was not silently moved to another port. `GET http://localhost:5173/` returned HTTP 200 and served the application root. The frontend command is `cd frontend && npm run dev`.

## API URL

`frontend/src/lib/api/http.ts` uses `VITE_API_BASE_URL`, falling back to `http://localhost:8080/api/v1`. The only frontend env file present is `.env.example`, which specifies that same local API base URL.

## CORS status

Spring Security preflight checks returned HTTP 200 for both `http://localhost:5173` and `http://127.0.0.1:5173`, with each origin explicitly echoed as allowed. The configured allowed methods include GET, POST, PUT, DELETE, and OPTIONS. No wildcard CORS setting was introduced.

## Login status

The existing login API was exercised with the local `admin01` test account. `POST /api/v1/auth/login` returned HTTP 200 and a token response identifying role `ADMIN`; the token itself was not printed. Authenticated `GET /api/v1/auth/me` returned HTTP 200 and role `ADMIN`. The existing client sets the token in its in-memory session before fetching `/auth/me`; no token persistence or authentication behavior was changed.

The frontend maps ADMIN to `/admin/dashboard`. Its five existing dashboard API requests returned HTTP 200: users, students, faculty, departments, and courses. These are API checks; no browser-rendered dashboard claim is made.

## Browser status

Browser automation was unavailable. No actual Chrome interaction, rendered page inspection, console/network panel inspection, screenshot, logout click-through, or visual/responsive review was performed. Command-line checks confirmed that Vite serves the application and that its configured backend API is reachable. Manual browser verification remains.

## Lint status

PASS — `cd frontend && npm run lint` completed successfully.

## Build status

PASS — `cd frontend && npm run build` completed successfully, including TypeScript and Vite production build.

## Diff check

PASS — `git diff --check` completed successfully using the GitHub Desktop bundled Git executable.

## Remaining issues

- Manual browser verification in Chrome remains necessary for rendered login/dashboard behavior, console inspection, logout interaction, and responsive/theme/accessibility checks.
- No application code blocker was found. Backend and frontend were already running locally and remain available at ports 8080 and 5173.

# CampusCore Frontend Auth Runtime Fix Report

## 1. Root cause

The frontend's shared API client uses `VITE_API_BASE_URL` when set and otherwise calls `http://localhost:8080/api/v1`. No local Vite API override was present, so the default was in use. Login is `POST /api/v1/auth/login` with JSON `{ "username": ..., "password": ... }`; the API returns `{ "token", "username", "role" }`.

The frontend displays “The server could not be reached” only when `fetch()` rejects; ordinary HTTP errors are normalized as `ApiError`. The Spring CORS fallback originally allowed `http://localhost:5173` only. A preflight from that origin succeeded, while a preflight from `http://127.0.0.1:5173` returned 403. Thus opening the Vite UI through the loopback-IP alias caused the browser to reject the request and enter the generic network-error branch. The actual browser address could not be inspected here, so the failure is confirmed for that origin and is consistent with the reported UI error.

The supplied `admin01` test login request reached Spring but returned HTTP 401. This is an authentication rejection, not a connectivity failure. No password or database record was changed.

## 2. Files inspected

- `frontend/src/features/auth/LoginPage.tsx`
- `frontend/src/features/auth/auth.api.ts`
- `frontend/src/features/auth/auth-context.tsx`
- `frontend/src/features/auth/session.ts`
- `frontend/src/lib/api/http.ts`
- `frontend/src/lib/api/api-error.ts`
- `frontend/.env.example` and local frontend environment-file presence (values were not exposed)
- `frontend/vite.config.ts`
- `backend/src/main/java/com/campuscore/backend/controller/AuthController.java`
- `backend/src/main/java/com/campuscore/backend/service/AuthService.java`
- `backend/src/main/java/com/campuscore/backend/config/SecurityConfig.java`
- `backend/src/main/java/com/campuscore/backend/dto/LoginRequest.java`
- `backend/src/main/java/com/campuscore/backend/dto/LoginResponse.java`
- Backend local configuration keys relevant to server port and CORS (secret values were not read or printed)

## 3. Files changed

- `backend/src/main/java/com/campuscore/backend/config/SecurityConfig.java`
- `FRONTEND-AUTH-RUNTIME-FIX-REPORT.md` (this report)

No frontend source, database, user record, or authentication implementation was changed. Other working-tree changes that predated this task were left untouched.

## 4. Exact fix

Added the explicit development origin `http://127.0.0.1:5173` to the SecurityConfig fallback CORS origin list, retaining `http://localhost:5173`. Configured `CORS_ALLOWED_ORIGINS` / `app.cors.allowed-origins` values continue to override the fallback, so production origin behavior is unchanged.

After restarting the existing backend on port 8080, preflight returned HTTP 200 with the matching `Access-Control-Allow-Origin` for both loopback origins. A login request from `127.0.0.1:5173` also received an HTTP response with the matching CORS header rather than being blocked by CORS.

## 5. Login endpoint verified

- Exact endpoint: `POST http://localhost:8080/api/v1/auth/login`.
- Contract: JSON username/password request; successful response contains token, username, and role. The frontend keeps the token in memory, calls `GET /api/v1/auth/me`, and clears the session if identity verification fails. No local/session storage is used.
- Preflight: HTTP 200 for `http://localhost:5173` and `http://127.0.0.1:5173` after the fix.
- Smoke login with the existing `admin01` test credential: HTTP 401. The backend was reached; authentication did not succeed, so JWT issuance, `/auth/me`, and Admin dashboard navigation could not be runtime-verified.
- A deliberately invalid smoke login also returned HTTP 401 with the expected CORS response header. The UI should therefore receive an authentication response rather than a fetch/network rejection for either supported loopback origin.

## 6. Lint result

- `cd frontend && npm run lint` — **PASS**.

## 7. Build result

- `cd frontend && npm run build` — **PASS** (`tsc -b` and Vite production build completed).

## 8. Backend result

- Restart command: `./mvnw -B spring-boot:run` from `backend/`.
- Result: **PASS**. CampusCore started on port 8080 after the CORS change. A temporary verification instance also started on port 8081 and was stopped after testing.
- The application connected to the configured MySQL instance in Hibernate `validate` mode during startup; no schema or record changes were made.
- `GET http://localhost:8080/` returned HTTP 401 as expected for a protected/non-public root request. `POST /api/v1/auth/login` returned HTTP responses as described above.

## 9. Login/browser result

- Vite `npm run dev` started and served the frontend with HTTP 200 at `http://localhost:5173/`; the server started by this verification was stopped afterward.
- Browser automation was unavailable, so rendered-page interaction, browser console, Admin dashboard loading, and in-browser token behavior were not tested.
- The CORS path is verified with HTTP preflight requests. Admin authentication remains unverified as successful because the attempted existing credential returned 401. No credential was printed or changed.

## 10. Remaining issues, if any

- Confirm the correct current local password for `admin01` outside this report; the attempted existing test credential was rejected with HTTP 401. Do not change the database solely to make this smoke test pass.
- If the browser uses a hostname other than `localhost` or `127.0.0.1`, that exact origin must be added to local `CORS_ALLOWED_ORIGINS` configuration. The production allowlist remains environment-configured.
- Successful JWT login, `/auth/me`, role redirect, and authenticated dashboard loading require valid credentials and browser-level verification.
- `git diff --check` — **PASS**.

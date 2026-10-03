# CampusCore — Frontend Final Browser QA Report

## 1. Authentication root cause/status

The frontend sends `POST /api/v1/auth/login` with JSON `{ username, password }`; the API contract returns `token`, `username`, and `role`. The earlier connectivity symptom was consistent with CORS when using `127.0.0.1`; the current backend accepts both configured local origins. The remaining `admin01` login failure was an invalid/unknown local account password (HTTP 401), not a server-connectivity failure. After the authorized local credential reset, login returned HTTP 200.

## 2. CORS status

Preflight requests to `/api/v1/auth/login` returned HTTP 200 for both `http://localhost:5173` and `http://127.0.0.1:5173`. Each response allowed its requested origin and listed GET, POST, PUT, DELETE, and OPTIONS.

## 3. Local test account status

The existing local `admin01` account was reset using the application’s BCrypt implementation. The active datasource was verified as loopback MySQL database `campuscore` with the default (non-production) profile before the update. Post-update checks confirmed username `admin01`, role `ADMIN`, and active status `true`. The report intentionally contains no password or hash.

## 4. Files changed

- Created: `FRONTEND-FINAL-BROWSER-QA-REPORT.md`.
- No backend or frontend source/configuration files were changed for this task.
- A temporary reset helper was used outside the repository and removed after use.
- Local database change: updated only `admin01.password_hash` and ensured `active = true`; no other user fields or records were changed.
- Other modified/untracked repository files shown by Git predated this task and were left untouched.

## 5. Database changes made ONLY for local test account

One local record was updated in the verified loopback `campuscore` database. The stored password is a BCrypt hash; plaintext was not written to the database, emitted in application output, or included in this report. No production database was accessed.

## 6. Backend startup result

`./mvnw -B spring-boot:run` started successfully. Spring Boot connected to local MySQL 8.4.11 and started Tomcat on port 8080. `GET http://localhost:8080/` returned HTTP 401, as expected for the secured root path.

## 7. Frontend startup result

`npm run dev` started Vite at `http://localhost:5173`. A local request to `/` returned HTTP 200 and the CampusCore HTML application shell.

## 8. Login result

`POST http://localhost:8080/api/v1/auth/login` returned HTTP 200. The response included a token, username `admin01`, and role `ADMIN`. The token value was not printed or saved by the verification script.

## 9. JWT/authentication result

Using the returned JWT in an Authorization Bearer header, `GET /api/v1/auth/me` returned HTTP 200 and confirmed `admin01`, role `ADMIN`, active `true`. Anonymous `GET /api/v1/auth/me` returned HTTP 401. Code inspection confirmed that the frontend holds the session token in its existing in-memory session mechanism; no authentication changes were made.

## 10. Admin dashboard result

The frontend’s role-home mapping resolves ADMIN to `/admin/dashboard`. The dashboard’s existing data requests all returned HTTP 200 with the authenticated admin token: paginated users, students, and faculty; departments; and paginated courses. This verifies the API data needed by the dashboard, but does not constitute browser rendering/click-through verification.

## 11. Browser QA result

Browser automation was unavailable in this environment. No actual browser click-through, rendered-page inspection, browser console inspection, or screenshot capture was performed. The Vite server response and live API requests were verified from the command line.

## 12. Responsive QA result

NOT VERIFIED visually. Desktop/mobile viewport behavior could not be checked without browser automation.

## 13. Theme QA result

NOT VERIFIED visually. Light, dark, and monochrome theme rendering could not be checked without browser automation.

## 14. Accessibility QA result

NOT VERIFIED interactively. Keyboard navigation, visible focus, dialog Escape behavior, and reduced-motion rendering could not be exercised without browser automation.

## 15. Console/network errors found

No browser console or browser network panel was available. Live HTTP checks had no request failures for the tested endpoints. Backend startup emitted non-blocking warnings about explicitly configured MySQL dialect, `spring.jpa.open-in-view`, and Mockito/Byte Buddy dynamic agent attachment during tests.

## 16. Fixes applied

No application code fixes were needed. The local test account password was reset as explicitly authorized, using BCrypt, after confirming the local datasource target. No authentication bypass, security weakening, or API contract change was introduced.

## 17. `npm run lint` result

PASS — ESLint completed successfully.

## 18. `npm run build` result

PASS — TypeScript project build and Vite production build completed successfully.

## 19. `git diff --check` result

PASS — no whitespace errors. The system Git command could not start because Xcode Command Line Tools are unavailable; the check was run successfully with the GitHub Desktop bundled Git executable.

## 20. Remaining issues

- Actual browser visual/interaction QA, including mobile layouts, themes, accessibility interactions, and logout click-through, remains unverified because browser automation was unavailable.
- Backend tests passed (8 tests, 0 failures, 0 errors, 0 skipped).
- Frontend dev server and backend were left running for local browser testing.

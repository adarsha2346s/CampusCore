# CampusCore

CampusCore is a role-based university operations application for maintaining campus accounts and academic records. It brings user, student, faculty, department, course, enrollment, assessment, marks, attendance, grading, and audit workflows into one web application backed by a Spring Boot REST API.

The project is intended for local development and portfolio demonstration. Real records are loaded from the configured MySQL database; the frontend does not include demo records or seeded dashboard statistics.

## Architecture

```text
React + TypeScript + Vite
             │ HTTPS/JSON + Bearer JWT
             ▼
        REST API routes
             ▼
Spring Boot controllers and services
             ▼
Spring Security + JWT authorization
             ▼
          MySQL
```

The frontend calls the versioned REST API, sends the current JWT in the `Authorization: Bearer` header, and uses React Query to cache server data. Spring Security validates the JWT and applies role rules before controller and service logic. Spring Data JPA persists the domain entities in MySQL.

## Roles and capabilities

| Role | Current workspace capabilities |
| --- | --- |
| `ADMIN` | Manage users, students, departments, and courses; create faculty profiles; operate enrollments, assessments, marks, attendance, grading policies, and audit-log views. Operations are limited to the methods the API actually exposes. |
| `FACULTY` | Read the student directory, course catalog, and department catalog. The backend does not currently expose an assignment feed or authorize faculty access to attendance, assessment, or marks APIs. |
| `STUDENT` | The API has ownership-checked student record, dashboard, and GPA operations. The frontend also provides the general course catalog. The current account response does not provide a `studentId`, so student-specific information remains unavailable in the UI until the backend exposes a safe identity lookup. |

The frontend protects role workspaces with route guards for usability. Backend authorization remains authoritative; hiding or guarding a frontend route is not a replacement for Spring Security.

## Features

- In-memory JWT login and client-side logout.
- Admin overview composed from existing admin list endpoints.
- Searchable user, student, faculty, department, and course directories with supported create/update/deactivate/delete flows and record details.
- Admin academic operations for enrollments, assessments, marks, attendance sessions and records, grading policies, GPA lookup, and audit logs.
- Faculty read-only directory and catalog workspace, with unsupported teaching operations called out clearly.
- Student course catalog and explicit identity-availability states instead of fabricated personal records.
- Responsive role navigation, directory tables/cards, form dialogs, validation, loading/error/empty states, and mutation feedback.

## Technology

### Frontend

- React 19, TypeScript, Vite
- React Router for routing and route-level lazy loading
- TanStack React Query for server state and cache invalidation
- React Hook Form and Zod for form state and validation
- Radix UI Dialog and Dropdown Menu primitives
- Lucide React icons and Sonner notifications

### Backend

- Java 25 and Spring Boot 4.1.1 (Maven wrapper)
- Spring MVC, Spring Data JPA, Bean Validation, Spring Security
- JWT via JJWT 0.13.0
- BCrypt password encoding
- MySQL Connector/J

### Database

The schema is defined in [`backend/database/schema.sql`](backend/database/schema.sql). Its principal relationships connect users to student/faculty profiles, departments to courses, students and courses through enrollments, assessments and marks, attendance sessions and records, grading policies, and audit logs. The schema script creates tables; it does not provide demonstration accounts or academic records.

## Authentication and security

- `POST /api/v1/auth/login` authenticates credentials and returns a signed JWT.
- The frontend then calls `GET /api/v1/auth/me` to verify the account and role.
- The JWT is held in JavaScript memory only. It is not written to `localStorage` or `sessionStorage`; reloading the page ends the client session.
- The current local backend configuration expires JWTs after 24 hours; there is no refresh-token endpoint, so expiry requires signing in again.
- Logout clears the client token, authenticated user, and React Query cache. There is no backend logout endpoint.
- A protected API 401 clears the current client session and cache. A 403 remains a permission error and does not log the user out.
- The backend uses stateless Spring Security, BCrypt, and role-based request authorization. Student record, dashboard, and GPA lookups have backend ownership checks.
- Backend CORS currently allows the development origin `http://localhost:5173` with `GET`, `POST`, `PUT`, `DELETE`, and `OPTIONS`; credentials are disabled.

Do not commit local database credentials, signing secrets, or account passwords. Keep them in local backend configuration or a secrets manager appropriate to the deployment environment.

## REST API modules

All routes are under `/api/v1`.

| Module | Existing API surface |
| --- | --- |
| Authentication | `POST /auth/login`, `GET /auth/me` |
| Users | `GET /users`, `GET /users/{id}`, `POST /users`, `PUT /users/{id}`, `DELETE /users/{id}` (deactivation behavior) |
| Students | `GET /students`, `GET /students/{id}`, `POST /students`, `PUT /students/{id}`, `DELETE /students/{id}` (deactivation behavior) |
| Faculty | `GET /faculty`, `GET /faculty/{id}`, `POST /faculty` |
| Departments | `GET /departments`, `GET /departments/{id}`, `POST /departments`, `PUT /departments/{id}`, `DELETE /departments/{id}` |
| Courses | `GET /courses`, `GET /courses/{id}`, `POST /courses`, `PUT /courses/{id}`, `DELETE /courses/{id}` |
| Enrollments | `GET /enrollments`, detail and student/course lookups, `POST /enrollments` |
| Assessments | `GET /assessments`, detail and `/assessments/course/{courseId}`, `POST /assessments` |
| Marks | `GET /marks`, detail and enrollment/assessment lookups, `POST /marks` |
| Attendance sessions | `GET /attendance-sessions`, detail and course/faculty lookups, `POST /attendance-sessions` |
| Attendance records | `GET /attendance-records`, detail and session/enrollment lookups, `POST /attendance-records` |
| Grading and GPA | `GET /grading-policies`, `POST /grading-policies`, `GET /gpa/enrollment/{enrollmentId}` with optional `policyName` |
| Audit | `GET /audit-logs`, user and entity lookups |
| Student dashboard | `GET /dashboard/student/{studentId}` (student role; ownership checked) |

Some academic create operations use query parameters rather than JSON request bodies. The frontend API modules follow those controller signatures. List APIs do not provide general server-side pagination; frontend search/filtering applies to the collection currently loaded.

## Repository layout

```text
CampusCore/
├── backend/
│   ├── database/schema.sql
│   ├── src/main/java/com/campuscore/backend/
│   │   ├── config/       # Spring Security and CORS
│   │   ├── controller/   # REST endpoints
│   │   ├── dto/          # API request and response types
│   │   ├── entity/       # JPA domain model
│   │   ├── exception/    # API validation response handling
│   │   ├── repository/   # Spring Data repositories
│   │   ├── security/     # JWT filter
│   │   └── service/      # Application/domain operations
│   ├── src/main/resources/application-example.properties
│   ├── mvnw
│   └── pom.xml
└── frontend/
    ├── src/app/          # Providers, routing, guards, system pages
    ├── src/components/   # Shared UI, navigation, and state components
    ├── src/features/     # Role and academic feature modules with API clients
    ├── src/layouts/      # Auth and role application shells
    ├── src/lib/          # HTTP client, API errors, query client, formatting
    ├── src/styles/       # Design tokens and responsive application styles
    ├── .env.example
    └── package.json
```

## Local development

### Prerequisites

- Java 25
- MySQL compatible with the JDBC driver
- Node.js compatible with the Vite toolchain and npm

### Database and backend

1. Create a local MySQL database and apply [`backend/database/schema.sql`](backend/database/schema.sql) using your local MySQL client.
2. Copy `backend/src/main/resources/application-example.properties` to `backend/src/main/resources/application.properties`, then set `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`, and a private JWT signing secret. The example uses schema validation and does not create or update tables. `application.properties` is ignored by Git; keep actual credentials and signing secrets local.
3. Start the backend:

   ```sh
   cd backend
   ./mvnw spring-boot:run
   ```

The API is configured for `http://localhost:8080`. Provision local user and academic records through the existing application/API as needed; this repository does not promise seeded login credentials.

### Frontend

```sh
cd frontend
npm ci
cp .env.example .env
npm run dev
```

The Vite app runs at `http://localhost:5173`. `.env.example` sets:

```dotenv
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

The API client defaults to this local API base URL when the variable is unset. For another environment, set `VITE_API_BASE_URL` to that environment's versioned API root. Values prefixed with `VITE_` are bundled into the client and must never contain secrets.

## Build and verification commands

Frontend, from `frontend/`:

```sh
npm run lint
npm run build
npm run dev
```

Backend, from `backend/`:

```sh
./mvnw test
./mvnw clean compile
./mvnw spring-boot:run
```

The frontend package currently defines lint and production-build scripts; it does not define a browser test or end-to-end test script.

## Known limitations and next improvements

- Add an authenticated student identity endpoint or include a safe student identifier in `/auth/me` so the UI can request the existing ownership-checked student dashboard and GPA data.
- Add an explicit faculty-course assignment API and authorization-scoped academic workflows before presenting assigned classes, attendance, assessments, or marks to faculty.
- Add server-side pagination/filtering for larger directories and audit collections.
- Add browser-level automated tests for authentication, role redirects, form behavior, dialogs, and responsive layouts.
- Add a supported token refresh/session renewal contract if persistent sessions are required. The current client intentionally requires a new login after a page reload.

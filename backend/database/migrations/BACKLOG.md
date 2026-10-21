# Migration log and deferred work (CampusCore V2)

Migrations are applied by hand. There is no Flyway or Liquibase in this project, so
ordering is a manual responsibility. `spring.jpa.hibernate.ddl-auto=validate` will
refuse to start the application until the matching script has been run.

| Script | What it does | Status |
|---|---|---|
| `V2__faculty_course_assignment.sql` | faculty/course assignment table | applied in development |
| `V3__optimistic_locking_version_columns.sql` | `version` column on four academic tables | **must be applied before deploying a build that maps `@Version`** |

## APPLIED IN CODE — V3 optimistic locking (`@Version`)

**Status: the entity mapping ships in Phase 4. The script is committed but must be
run by hand against any existing database first, including production, and only
with approval.**

### Why it is needed now

The Phase 2 assessment recorded that no resource had an update route, so a lost
update was impossible and no version column was warranted. Phase 3 added `PUT`
routes for marks, assessments, enrollments and attendance sessions. That is
exactly the trigger the earlier assessment named: two members of staff correcting
the same row, with the second silently overwriting the first. For a grade, that is
a real correctness problem, not a theoretical one.

### Scope

Version columns are on the four tables whose concurrent edits have academic
consequence:

- `mark` — a stale overwrite changes a recorded grade.
- `assessment` — the maximum and weight apply to every mark recorded against it.
- `enrollment` — status and term decide how academic history is counted.
- `attendance_session` — attendance records are filed against the session.

Deliberately **not** included: `course`, `department`, `student`, `faculty`, `user`.
Those are single-writer administrator catalog edits with no academic blast radius,
and `attendance_record` is still create-only, where the existing
`UNIQUE (attendance_session_id, enrollment_id)` already rejects double submission.

### What this changes

- Every accepted write to those four tables now carries `WHERE version = ?`, and a
  stale write raises `OptimisticLockingFailureException`.
- `GlobalExceptionHandler` already mapped that to **409** before this change, so
  the HTTP contract needed no work. `GlobalExceptionHandlerTest` covers it.
- The frontend surfaces the 409 message through `notifyError` rather than claiming
  the edit succeeded.
- Callers that retry after a 409 must re-read first; they cannot blindly resubmit.

### Verification

`OptimisticLockingIT` proves the real behaviour against a database rather than
mocks: it loads a detached copy, lets a second writer commit, then writes the stale
copy back and asserts it is rejected and the first value survives.

```
mvn test -Dtest=OptimisticLockingIT \
  -Dcc.it.datasource.url=jdbc:mysql://127.0.0.1:PORT/campuscore
```

### Do not

- run V3 against production without approval,
- edit `database/schema.sql` to add the columns. It is the V1 baseline for fresh
  installs; incremental changes belong in `migrations/`,
- add `@Version` speculatively to the remaining entities. It changes the UPDATE
  statement for all writes and turns a lost update into a 409 that every caller
  then has to handle.

## Still pending

- **Attendance records have no update route.** Create-only today, protected by the
  unique constraint. It will need the same treatment the moment a `PUT` appears.
- **No `@Version` on the catalog tables.** Revisit only if more than one
  administrator starts editing the same course or student record concurrently.
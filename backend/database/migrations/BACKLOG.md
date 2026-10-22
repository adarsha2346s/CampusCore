# Migration log and deferred work (CampusCore V2)

Migrations are applied by hand. There is no Flyway or Liquibase in this project, so
ordering is a manual responsibility. `spring.jpa.hibernate.ddl-auto=validate` will
refuse to start the application until the matching script has been run.

## The baseline and the upgrade scripts

`database/schema.sql` is the **complete current baseline**. Applying it to an empty
database produces exactly the schema the application expects, so a fresh install
needs no follow-up script and no ordering to remember.

`database/migrations/` holds the scripts that upgrade databases created from an
earlier baseline. They are additive and idempotent: each one adds a column only
when it is missing, so it is safe to run against a database that already has it
and safe to run more than once.

| Database state | What to run |
|---|---|
| New / empty | `database/schema.sql` only |
| Created from an earlier baseline | `database/schema.sql` was already applied; run the migration scripts in `database/migrations/` in version order |

| Script | What it does | Status |
|---|---|---|
| `V2__faculty_course_assignment.sql` | faculty/course assignment table | applied in development |
| `V3__optimistic_locking_version_columns.sql` | `version` column on four academic tables | **already part of the baseline; run it only against a database created before the baseline was completed** |

## APPLIED IN CODE — V3 optimistic locking (`@Version`)

**Status: the entity mapping ships in Phase 4. The `version` columns are now part of
`database/schema.sql`, so a fresh install is complete. A database created before
that change must still run this script by hand, including production, and only with
approval.**

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
- remove the `version` columns from `database/schema.sql`. The baseline must stay
  complete: a fresh install applies that file alone and the application starts with
  `ddl-auto=validate`. Incremental changes for existing databases belong in
  `migrations/`,
- add `@Version` speculatively to the remaining entities. It changes the UPDATE
  statement for all writes and turns a lost update into a 409 that every caller
  then has to handle.

## Still pending

- **Attendance records have no update route.** Create-only today, protected by the
  unique constraint. It will need the same treatment the moment a `PUT` appears.
- **No `@Version` on the catalog tables.** Revisit only if more than one
  administrator starts editing the same course or student record concurrently.
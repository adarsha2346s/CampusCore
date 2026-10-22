-- V3: optimistic locking on the academic records that carry a PUT route.
--
-- Phase 3 added update routes for marks, assessments, enrollments and attendance
-- sessions. Before that these tables were create-only, so a lost update was
-- impossible and no version column was warranted (see BACKLOG.md). Now that a
-- second member of staff can correct the same row, a read-modify-write can
-- silently overwrite someone else's change, which for a grade is a real problem.
--
-- Scope is deliberately limited to the four tables whose concurrent edits have
-- academic consequence. course, department, student, faculty and user are not
-- included: those edits are single-writer administrator catalog work, and
-- attendance_record is still create-only with a unique constraint that rejects
-- double submission.
--
-- One column per table, defaulting to 0 so existing rows need no backfill and no
-- record becomes invalid. Adding a NOT NULL column with a constant default is an
-- instant metadata-only operation on MySQL 8.
--
-- WHO NEEDS THIS SCRIPT
--   A database created from database/schema.sql already contains these columns:
--   the baseline was completed to match the entity mapping, so a fresh install
--   needs no follow-up script and no ordering to remember.
--
--   A database created from an earlier baseline does not. Apply this script once
--   to it before deploying a build that maps @Version on these entities,
--   otherwise startup validation fails with
--   "Schema validation: missing column [version] in table [assessment]".
--
--   mysql campuscore < backend/database/migrations/V3__optimistic_locking_version_columns.sql
--
-- IDEMPOTENCE
--   Each column is added only when it is missing, so this script is safe to run
--   against a database created from the current schema.sql (it changes nothing)
--   and safe to run more than once (it never adds the same column twice).
--   MySQL 8.4 has no `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`, so each
--   statement is guarded by an information_schema lookup executed as a prepared
--   statement instead.
--
-- There is no Flyway or Liquibase in this project, so this script is applied by
-- hand. Do not run it against production without approval.

SET @campuscore_schema := DATABASE();

-- mark.version
SET @stmt := (
    SELECT IF(
        EXISTS (SELECT 1
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = @campuscore_schema
                  AND TABLE_NAME = 'mark'
                  AND COLUMN_NAME = 'version'),
        'SELECT ''mark.version already present'' AS note',
        'ALTER TABLE mark ADD COLUMN version BIGINT NOT NULL DEFAULT 0'
    )
);
PREPARE campuscore_stmt FROM @stmt;
EXECUTE campuscore_stmt;
DEALLOCATE PREPARE campuscore_stmt;

-- assessment.version
SET @stmt := (
    SELECT IF(
        EXISTS (SELECT 1
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = @campuscore_schema
                  AND TABLE_NAME = 'assessment'
                  AND COLUMN_NAME = 'version'),
        'SELECT ''assessment.version already present'' AS note',
        'ALTER TABLE assessment ADD COLUMN version BIGINT NOT NULL DEFAULT 0'
    )
);
PREPARE campuscore_stmt FROM @stmt;
EXECUTE campuscore_stmt;
DEALLOCATE PREPARE campuscore_stmt;

-- enrollment.version
SET @stmt := (
    SELECT IF(
        EXISTS (SELECT 1
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = @campuscore_schema
                  AND TABLE_NAME = 'enrollment'
                  AND COLUMN_NAME = 'version'),
        'SELECT ''enrollment.version already present'' AS note',
        'ALTER TABLE enrollment ADD COLUMN version BIGINT NOT NULL DEFAULT 0'
    )
);
PREPARE campuscore_stmt FROM @stmt;
EXECUTE campuscore_stmt;
DEALLOCATE PREPARE campuscore_stmt;

-- attendance_session.version
SET @stmt := (
    SELECT IF(
        EXISTS (SELECT 1
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = @campuscore_schema
                  AND TABLE_NAME = 'attendance_session'
                  AND COLUMN_NAME = 'version'),
        'SELECT ''attendance_session.version already present'' AS note',
        'ALTER TABLE attendance_session ADD COLUMN version BIGINT NOT NULL DEFAULT 0'
    )
);
PREPARE campuscore_stmt FROM @stmt;
EXECUTE campuscore_stmt;
DEALLOCATE PREPARE campuscore_stmt;

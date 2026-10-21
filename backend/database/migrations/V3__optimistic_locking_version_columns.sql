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
-- V1 record becomes invalid. Adding a NOT NULL column with a constant default is
-- an instant metadata-only operation on MySQL 8.
--
-- Because spring.jpa.hibernate.ddl-auto=validate does not create columns, this
-- script MUST be applied before deploying a build that maps @Version on these
-- entities, otherwise startup validation fails. Apply by hand; there is no
-- Flyway or Liquibase in this project. Do not run against production without
-- approval.

ALTER TABLE mark ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE assessment ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE enrollment ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE attendance_session ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
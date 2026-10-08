-- CampusCore V2 migration: faculty <-> course assignment.
--
-- PURPOSE
--   Adds the normalised faculty_course_assignment relationship. This is purely
--   additive: it creates one new table and two new indexes. No existing table is
--   altered, truncated or dropped, and no existing row is read, rewritten or
--   deleted. Existing V1 enrollments, assessments, marks and attendance records
--   therefore remain valid and untouched.
--
-- DATA SAFETY
--   * This script does NOT backfill assignments. Pre-existing V1 courses are left
--     with no assignment and stay valid: the table does not require one. Inventing
--     a faculty-to-course mapping would fabricate academic relationships, and the
--     available data cannot justify one (for example a department can hold courses
--     with no faculty member). Assignments are created deliberately through the
--     new API instead.
--   * Additive DDL only, so it is safe to run against a live database and safe to
--     re-run: every statement is guarded.
--   * Run it manually against a disposable or staging database first, then
--     against production during a maintenance window. It is intentionally not
--     applied by the application: spring.jpa.hibernate.ddl-auto=validate only
--     validates the schema and never creates it.
--
-- COMPATIBILITY
--   MySQL 8.0+ / MariaDB 10.2+. Matches the DDL in schema.sql exactly so a fresh
--   database and a migrated database end up identical.

USE campuscore;

CREATE TABLE IF NOT EXISTS faculty_course_assignment
(
    assignment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id     BIGINT      NOT NULL,
    faculty_id    BIGINT      NOT NULL,
    assigned_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at      TIMESTAMP   NULL,

    CONSTRAINT fk_assignment_course
        FOREIGN KEY (course_id)
            REFERENCES course (course_id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_assignment_faculty
        FOREIGN KEY (faculty_id)
            REFERENCES faculty (faculty_id)
            ON DELETE RESTRICT
);

-- MySQL has no "CREATE INDEX IF NOT EXISTS", so the index creation is guarded
-- through information_schema. That keeps the whole script re-runnable.
SET @idx_course_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'faculty_course_assignment'
      AND INDEX_NAME = 'idx_assignment_course'
);

SET @idx_course_sql = IF(
        @idx_course_exists = 0,
        'CREATE INDEX idx_assignment_course ON faculty_course_assignment (course_id)',
        'DO 0'
);

PREPARE idx_course_stmt FROM @idx_course_sql;
EXECUTE idx_course_stmt;
DEALLOCATE PREPARE idx_course_stmt;

SET @idx_faculty_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'faculty_course_assignment'
      AND INDEX_NAME = 'idx_assignment_faculty'
);

SET @idx_faculty_sql = IF(
        @idx_faculty_exists = 0,
        'CREATE INDEX idx_assignment_faculty ON faculty_course_assignment (faculty_id)',
        'DO 0'
);

PREPARE idx_faculty_stmt FROM @idx_faculty_sql;
EXECUTE idx_faculty_stmt;
DEALLOCATE PREPARE idx_faculty_stmt;

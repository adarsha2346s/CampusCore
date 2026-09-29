-- DEMO ONLY: synthetic records for a fresh, isolated CampusCore demo database.
-- Initialize that disposable database with schema.sql first, then run this file once.
-- Do not run against an existing local, shared, or production database.
-- This seed is insert-only and uses natural identifiers to resolve generated IDs.
-- Password values below are BCrypt hashes generated with the application's
-- Spring Security BCryptPasswordEncoder. The plaintext demo password is not stored here.

START TRANSACTION;

INSERT INTO department (name, code) VALUES
    ('Computer Science and Engineering', 'CSE'),
    ('Electronics and Communication Engineering', 'ECE'),
    ('Mathematics', 'MATH');

INSERT INTO users (username, email, password_hash, role, active) VALUES
    ('demo_admin', 'demo_admin@example.test', '$2a$10$YljpvdPELNgi980tT9bCUOKo39edMHhEPsB3uhT1tZDtHGoq3Zdsq', 'ADMIN', TRUE),
    ('demo_faculty_01', 'demo_faculty01@example.test', '$2a$10$YljpvdPELNgi980tT9bCUOKo39edMHhEPsB3uhT1tZDtHGoq3Zdsq', 'FACULTY', TRUE),
    ('demo_faculty_02', 'demo_faculty02@example.test', '$2a$10$YljpvdPELNgi980tT9bCUOKo39edMHhEPsB3uhT1tZDtHGoq3Zdsq', 'FACULTY', TRUE),
    ('demo_student_01', 'demo_student01@example.test', '$2a$10$YljpvdPELNgi980tT9bCUOKo39edMHhEPsB3uhT1tZDtHGoq3Zdsq', 'STUDENT', TRUE),
    ('demo_student_02', 'demo_student02@example.test', '$2a$10$YljpvdPELNgi980tT9bCUOKo39edMHhEPsB3uhT1tZDtHGoq3Zdsq', 'STUDENT', TRUE),
    ('demo_student_03', 'demo_student03@example.test', '$2a$10$YljpvdPELNgi980tT9bCUOKo39edMHhEPsB3uhT1tZDtHGoq3Zdsq', 'STUDENT', TRUE),
    ('demo_student_04', 'demo_student04@example.test', '$2a$10$YljpvdPELNgi980tT9bCUOKo39edMHhEPsB3uhT1tZDtHGoq3Zdsq', 'STUDENT', TRUE),
    ('demo_student_05', 'demo_student05@example.test', '$2a$10$YljpvdPELNgi980tT9bCUOKo39edMHhEPsB3uhT1tZDtHGoq3Zdsq', 'STUDENT', TRUE),
    ('demo_student_06', 'demo_student06@example.test', '$2a$10$YljpvdPELNgi980tT9bCUOKo39edMHhEPsB3uhT1tZDtHGoq3Zdsq', 'STUDENT', TRUE);

INSERT INTO student (
    user_id, department_id, enrollment_number, first_name, last_name,
    admission_year, status
)
SELECT u.user_id, d.department_id, seed.enrollment_number, seed.first_name,
       seed.last_name, 2026, 'ACTIVE'
FROM (
    SELECT 'demo_student_01' AS username, 'DEMO-2026-001' AS enrollment_number,
           'Avery' AS first_name, 'Chen' AS last_name, 'CSE' AS department_code
    UNION ALL SELECT 'demo_student_02', 'DEMO-2026-002', 'Riley', 'Brooks', 'CSE'
    UNION ALL SELECT 'demo_student_03', 'DEMO-2026-003', 'Casey', 'Morgan', 'ECE'
    UNION ALL SELECT 'demo_student_04', 'DEMO-2026-004', 'Jordan', 'Lee', 'ECE'
    UNION ALL SELECT 'demo_student_05', 'DEMO-2026-005', 'Taylor', 'Singh', 'MATH'
    UNION ALL SELECT 'demo_student_06', 'DEMO-2026-006', 'Quinn', 'Parker', 'MATH'
) AS seed
JOIN users u ON u.username = seed.username
JOIN department d ON d.code = seed.department_code;

INSERT INTO faculty (
    user_id, department_id, employee_number, first_name, last_name, status
)
SELECT u.user_id, d.department_id, seed.employee_number, seed.first_name,
       seed.last_name, 'ACTIVE'
FROM (
    SELECT 'demo_faculty_01' AS username, 'DEMO-FAC-001' AS employee_number,
           'Jamie' AS first_name, 'Patel' AS last_name, 'CSE' AS department_code
    UNION ALL SELECT 'demo_faculty_02', 'DEMO-FAC-002', 'Morgan', 'Rivera', 'MATH'
) AS seed
JOIN users u ON u.username = seed.username
JOIN department d ON d.code = seed.department_code;

INSERT INTO course (
    department_id, course_code, course_name, credits, capacity, status
)
SELECT d.department_id, seed.course_code, seed.course_name, seed.credits,
       seed.capacity, 'ACTIVE'
FROM (
    SELECT 'CSE' AS department_code, 'CSE101' AS course_code,
           'Data Structures' AS course_name, 4 AS credits, 40 AS capacity
    UNION ALL SELECT 'CSE', 'CSE201', 'Database Management Systems', 4, 40
    UNION ALL SELECT 'CSE', 'CSE202', 'Computer Networks', 3, 40
    UNION ALL SELECT 'CSE', 'CSE203', 'Operating Systems', 4, 40
    UNION ALL SELECT 'ECE', 'ECE101', 'Java Programming', 3, 35
    UNION ALL SELECT 'MATH', 'MTH101', 'Engineering Mathematics', 4, 45
) AS seed
JOIN department d ON d.code = seed.department_code;

INSERT INTO enrollment (
    student_id, course_id, semester, academic_year, enrollment_date, status
)
SELECT s.student_id, c.course_id, 'Fall', '2026-2027', '2026-08-10', 'ENROLLED'
FROM (
    SELECT 'DEMO-2026-001' AS enrollment_number, 'CSE101' AS course_code
    UNION ALL SELECT 'DEMO-2026-001', 'CSE201'
    UNION ALL SELECT 'DEMO-2026-001', 'MTH101'
    UNION ALL SELECT 'DEMO-2026-002', 'CSE101'
    UNION ALL SELECT 'DEMO-2026-002', 'CSE202'
    UNION ALL SELECT 'DEMO-2026-003', 'CSE201'
    UNION ALL SELECT 'DEMO-2026-003', 'ECE101'
    UNION ALL SELECT 'DEMO-2026-004', 'CSE202'
    UNION ALL SELECT 'DEMO-2026-004', 'MTH101'
    UNION ALL SELECT 'DEMO-2026-005', 'CSE203'
    UNION ALL SELECT 'DEMO-2026-005', 'ECE101'
    UNION ALL SELECT 'DEMO-2026-006', 'CSE203'
    UNION ALL SELECT 'DEMO-2026-006', 'MTH101'
) AS seed
JOIN student s ON s.enrollment_number = seed.enrollment_number
JOIN course c ON c.course_code = seed.course_code;

INSERT INTO assessment (
    course_id, name, assessment_type, max_marks, weight, assessment_date
)
SELECT c.course_id, seed.name, seed.assessment_type, seed.max_marks,
       seed.weight, seed.assessment_date
FROM course c
CROSS JOIN (
    SELECT 'Assignment' AS name, 'ASSIGNMENT' AS assessment_type,
           20.00 AS max_marks, 20.00 AS weight, '2026-08-20' AS assessment_date
    UNION ALL SELECT 'Midterm', 'MIDTERM', 30.00, 30.00, '2026-09-05'
    UNION ALL SELECT 'Final', 'FINAL', 50.00, 50.00, '2026-09-20'
) AS seed
WHERE c.course_code IN ('CSE101', 'CSE201', 'CSE202', 'CSE203', 'ECE101', 'MTH101');

INSERT INTO mark (enrollment_id, assessment_id, marks_obtained)
SELECT e.enrollment_id, a.assessment_id,
       CASE
           WHEN u.username = 'demo_student_01' AND c.course_code = 'CSE101'
                AND a.assessment_type = 'ASSIGNMENT' THEN 18.00
           WHEN u.username = 'demo_student_01' AND c.course_code = 'CSE101'
                AND a.assessment_type = 'MIDTERM' THEN 26.00
           WHEN u.username = 'demo_student_01' AND c.course_code = 'CSE101'
                AND a.assessment_type = 'FINAL' THEN 44.00
           WHEN u.username = 'demo_student_01' AND c.course_code = 'CSE201'
                AND a.assessment_type = 'ASSIGNMENT' THEN 19.00
           WHEN u.username = 'demo_student_01' AND c.course_code = 'CSE201'
                AND a.assessment_type = 'MIDTERM' THEN 27.00
           WHEN u.username = 'demo_student_01' AND c.course_code = 'CSE201'
                AND a.assessment_type = 'FINAL' THEN 45.00
           WHEN u.username = 'demo_student_01' AND c.course_code = 'MTH101'
                AND a.assessment_type = 'ASSIGNMENT' THEN 17.00
           WHEN u.username = 'demo_student_01' AND c.course_code = 'MTH101'
                AND a.assessment_type = 'MIDTERM' THEN 25.00
           WHEN u.username = 'demo_student_01' AND c.course_code = 'MTH101'
                AND a.assessment_type = 'FINAL' THEN 42.00
           WHEN a.assessment_type = 'ASSIGNMENT' THEN 16.00
           WHEN a.assessment_type = 'MIDTERM' THEN 23.00
           ELSE 40.00
       END AS marks_obtained
FROM enrollment e
JOIN student s ON s.student_id = e.student_id
JOIN users u ON u.user_id = s.user_id
JOIN course c ON c.course_id = e.course_id
JOIN assessment a ON a.course_id = c.course_id;

INSERT INTO grading_policy (
    name, min_percentage, max_percentage, grade, grade_point
) VALUES
    ('DEFAULT_10_POINT', 0.00, 49.99, 'F', 0.00),
    ('DEFAULT_10_POINT', 50.00, 59.99, 'D', 4.00),
    ('DEFAULT_10_POINT', 60.00, 69.99, 'C', 6.00),
    ('DEFAULT_10_POINT', 70.00, 79.99, 'B', 7.00),
    ('DEFAULT_10_POINT', 80.00, 89.99, 'A', 8.00),
    ('DEFAULT_10_POINT', 90.00, 100.00, 'A+', 10.00);

INSERT INTO attendance_session (course_id, faculty_id, session_date, topic)
SELECT c.course_id, f.faculty_id, dates.session_date, dates.topic
FROM course c
JOIN department d ON d.department_id = c.department_id
JOIN (
    SELECT '2026-08-20' AS session_date, 'Lecture 1' AS topic
    UNION ALL SELECT '2026-09-03', 'Lecture 2'
    UNION ALL SELECT '2026-09-17', 'Lecture 3'
) AS dates
JOIN users faculty_user
  ON faculty_user.username = CASE
      WHEN d.code = 'MATH' THEN 'demo_faculty_02'
      ELSE 'demo_faculty_01'
  END
JOIN faculty f ON f.user_id = faculty_user.user_id
WHERE c.course_code IN ('CSE101', 'CSE201', 'CSE202', 'CSE203', 'ECE101', 'MTH101');

INSERT INTO attendance_record (attendance_session_id, enrollment_id, status)
SELECT att_sess.attendance_session_id, e.enrollment_id,
       CASE
           WHEN u.username = 'demo_student_01'
                AND c.course_code = 'CSE201'
                AND att_sess.session_date = '2026-09-17' THEN 'ABSENT'
           ELSE 'PRESENT'
       END AS status
FROM attendance_session att_sess
JOIN course c ON c.course_id = att_sess.course_id
JOIN enrollment e ON e.course_id = c.course_id
JOIN student s ON s.student_id = e.student_id
JOIN users u ON u.user_id = s.user_id;

COMMIT;

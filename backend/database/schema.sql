USE campuscore;

CREATE TABLE department (
                            department_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            name VARCHAR(100) NOT NULL,
                            code VARCHAR(20) NOT NULL,
                            CONSTRAINT uk_department_name UNIQUE (name),
                            CONSTRAINT uk_department_code UNIQUE (code)
);

CREATE TABLE users (
                       user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       username VARCHAR(50) NOT NULL,
                       email VARCHAR(150) NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       role ENUM('ADMIN', 'FACULTY', 'STUDENT') NOT NULL,
                       active BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                           ON UPDATE CURRENT_TIMESTAMP,
                       CONSTRAINT uk_user_username UNIQUE (username),
                       CONSTRAINT uk_user_email UNIQUE (email)
);

CREATE TABLE student (
                         student_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         user_id BIGINT NOT NULL,
                         department_id BIGINT NOT NULL,
                         enrollment_number VARCHAR(50) NOT NULL,
                         first_name VARCHAR(100) NOT NULL,
                         last_name VARCHAR(100),
                         date_of_birth DATE,
                         phone VARCHAR(20),
                         admission_year INT NOT NULL,
                         status ENUM('ACTIVE', 'INACTIVE', 'GRADUATED') NOT NULL DEFAULT 'ACTIVE',
                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                         CONSTRAINT uk_student_user UNIQUE (user_id),
                         CONSTRAINT uk_student_enrollment_number UNIQUE (enrollment_number),

                         CONSTRAINT fk_student_user
                             FOREIGN KEY (user_id)
                                 REFERENCES users(user_id)
                                 ON DELETE RESTRICT,

                         CONSTRAINT fk_student_department
                             FOREIGN KEY (department_id)
                                 REFERENCES department(department_id)
                                 ON DELETE RESTRICT
);

CREATE TABLE faculty (
                         faculty_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         user_id BIGINT NOT NULL,
                         department_id BIGINT NOT NULL,
                         employee_number VARCHAR(50) NOT NULL,
                         first_name VARCHAR(100) NOT NULL,
                         last_name VARCHAR(100),
                         phone VARCHAR(20),
                         status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                         CONSTRAINT uk_faculty_user UNIQUE (user_id),
                         CONSTRAINT uk_faculty_employee_number UNIQUE (employee_number),

                         CONSTRAINT fk_faculty_user
                             FOREIGN KEY (user_id)
                                 REFERENCES users(user_id)
                                 ON DELETE RESTRICT,

                         CONSTRAINT fk_faculty_department
                             FOREIGN KEY (department_id)
                                 REFERENCES department(department_id)
                                 ON DELETE RESTRICT
);

CREATE TABLE course (
                        course_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        department_id BIGINT NOT NULL,
                        course_code VARCHAR(30) NOT NULL,
                        course_name VARCHAR(150) NOT NULL,
                        credits INT NOT NULL,
                        capacity INT NOT NULL,
                        status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                        CONSTRAINT uk_course_code UNIQUE (course_code),

                        CONSTRAINT fk_course_department
                            FOREIGN KEY (department_id)
                                REFERENCES department(department_id)
                                ON DELETE RESTRICT
);

CREATE TABLE enrollment (
                            enrollment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            student_id BIGINT NOT NULL,
                            course_id BIGINT NOT NULL,
                            semester VARCHAR(30) NOT NULL,
                            academic_year VARCHAR(20) NOT NULL,
                            enrollment_date DATE NOT NULL,
                            status ENUM('ENROLLED', 'DROPPED', 'COMPLETED') NOT NULL DEFAULT 'ENROLLED',

                            CONSTRAINT uk_student_course_semester
                                UNIQUE (student_id, course_id, semester, academic_year),

                            CONSTRAINT fk_enrollment_student
                                FOREIGN KEY (student_id)
                                    REFERENCES student(student_id)
                                    ON DELETE RESTRICT,

                            CONSTRAINT fk_enrollment_course
                                FOREIGN KEY (course_id)
                                    REFERENCES course(course_id)
                                    ON DELETE RESTRICT
);

CREATE TABLE assessment (
                            assessment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            course_id BIGINT NOT NULL,
                            name VARCHAR(100) NOT NULL,
                            assessment_type ENUM(
        'QUIZ',
        'ASSIGNMENT',
        'MIDTERM',
        'FINAL',
        'PROJECT'
    ) NOT NULL,
                            max_marks DECIMAL(6,2) NOT NULL,
                            weight DECIMAL(5,2) NOT NULL,
                            assessment_date DATE,

                            CONSTRAINT fk_assessment_course
                                FOREIGN KEY (course_id)
                                    REFERENCES course(course_id)
                                    ON DELETE RESTRICT
);

CREATE TABLE mark (
                      mark_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      enrollment_id BIGINT NOT NULL,
                      assessment_id BIGINT NOT NULL,
                      marks_obtained DECIMAL(6,2) NOT NULL,
                      entered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                      CONSTRAINT uk_enrollment_assessment
                          UNIQUE (enrollment_id, assessment_id),

                      CONSTRAINT fk_mark_enrollment
                          FOREIGN KEY (enrollment_id)
                              REFERENCES enrollment(enrollment_id)
                              ON DELETE RESTRICT,

                      CONSTRAINT fk_mark_assessment
                          FOREIGN KEY (assessment_id)
                              REFERENCES assessment(assessment_id)
                              ON DELETE RESTRICT
);

CREATE TABLE attendance_session (
                                    attendance_session_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    course_id BIGINT NOT NULL,
                                    faculty_id BIGINT NOT NULL,
                                    session_date DATE NOT NULL,
                                    topic VARCHAR(255),
                                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                    CONSTRAINT fk_attendance_session_course
                                        FOREIGN KEY (course_id)
                                            REFERENCES course(course_id)
                                            ON DELETE RESTRICT,

                                    CONSTRAINT fk_attendance_session_faculty
                                        FOREIGN KEY (faculty_id)
                                            REFERENCES faculty(faculty_id)
                                            ON DELETE RESTRICT,

                                    CONSTRAINT uk_course_session
                                        UNIQUE (course_id, session_date)
);

CREATE TABLE attendance_record (
                                   attendance_record_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                   attendance_session_id BIGINT NOT NULL,
                                   enrollment_id BIGINT NOT NULL,
                                   status ENUM('PRESENT', 'ABSENT', 'LATE') NOT NULL,
                                   marked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                   CONSTRAINT uk_session_enrollment
                                       UNIQUE (attendance_session_id, enrollment_id),

                                   CONSTRAINT fk_attendance_record_session
                                       FOREIGN KEY (attendance_session_id)
                                           REFERENCES attendance_session(attendance_session_id)
                                           ON DELETE RESTRICT,

                                   CONSTRAINT fk_attendance_record_enrollment
                                       FOREIGN KEY (enrollment_id)
                                           REFERENCES enrollment(enrollment_id)
                                           ON DELETE RESTRICT
);

CREATE TABLE grading_policy (
                                grading_policy_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                name VARCHAR(100) NOT NULL,
                                min_percentage DECIMAL(5,2) NOT NULL,
                                max_percentage DECIMAL(5,2) NOT NULL,
                                grade VARCHAR(5) NOT NULL,
                                grade_point DECIMAL(4,2) NOT NULL,

                                CONSTRAINT uk_grading_range
                                    UNIQUE (name, min_percentage, max_percentage)
);

CREATE TABLE audit_log (
                           audit_log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           user_id BIGINT,
                           action VARCHAR(100) NOT NULL,
                           entity_name VARCHAR(100) NOT NULL,
                           entity_id BIGINT,
                           description VARCHAR(500),
                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT fk_audit_user
                               FOREIGN KEY (user_id)
                                   REFERENCES users(user_id)
                                   ON DELETE RESTRICT
);

CREATE INDEX idx_student_department
    ON student(department_id);

CREATE INDEX idx_faculty_department
    ON faculty(department_id);

CREATE INDEX idx_course_department
    ON course(department_id);

CREATE INDEX idx_enrollment_student
    ON enrollment(student_id);

CREATE INDEX idx_enrollment_course
    ON enrollment(course_id);

CREATE INDEX idx_mark_enrollment
    ON mark(enrollment_id);

CREATE INDEX idx_attendance_enrollment
    ON attendance_record(enrollment_id);

CREATE INDEX idx_audit_user
    ON audit_log(user_id);
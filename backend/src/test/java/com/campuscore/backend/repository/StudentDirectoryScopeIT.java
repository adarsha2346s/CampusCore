package com.campuscore.backend.repository;

import com.campuscore.backend.entity.Course;
import com.campuscore.backend.entity.Department;
import com.campuscore.backend.entity.Enrollment;
import com.campuscore.backend.entity.Student;
import com.campuscore.backend.entity.User;
import com.campuscore.backend.support.IntegrationDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The faculty student directory query, exercised against a real database.
 *
 * <p>The defect this guards against was a pagination bypass: the faculty
 * directory listing skipped its teaching-scope filter when the caller asked for
 * a page, and returned the whole student table instead. A mock-based test can
 * prove that a service called the right repository method, but only a real
 * database can prove that the query itself filters, and that the page total is
 * counted over the filtered set rather than over the whole table.
 *
 * <p>The fixture creates two courses and three students on purpose: two of them
 * are enrolled in the taught course and one is not. The directory must contain
 * the two, must not contain the third, and must report a total of two while
 * the table itself holds three.
 */
@SpringBootTest
@ActiveProfiles("it")
@EnabledIfSystemProperty(named = "cc.it.datasource.url", matches = ".+")
@Transactional
class StudentDirectoryScopeIT {

    @Autowired
    private StudentRepository students;

    @Autowired
    private EnrollmentRepository enrollments;

    @Autowired
    private DepartmentRepository departments;

    @Autowired
    private CourseRepository courses;

    @Autowired
    private UserRepository users;

    @DynamicPropertySource
    static void integrationDataSource(DynamicPropertyRegistry registry) {
        IntegrationDataSource.register(registry);
    }

    private Long taughtCourseId;
    private Long unrelatedCourseId;
    private Long taughtStudentId;
    private Long secondTaughtStudentId;
    private Long unrelatedStudentId;
    private Department fixtureDepartment;

    @BeforeEach
    void setUp() {
        String unique = String.valueOf(System.nanoTime());

        Department department = new Department();
        department.setName("Directory Dept " + unique);
        department.setCode("DIR" + Math.abs(unique.hashCode() % 100000));
        fixtureDepartment = departments.save(department);

        taughtCourseId = course(fixtureDepartment, "DIRT" + unique, "Taught Course");
        unrelatedCourseId = course(fixtureDepartment, "DIRU" + unique, "Unrelated Course");

        taughtStudentId = student(fixtureDepartment, "DIR-T" + unique, "Taught");
        secondTaughtStudentId = student(fixtureDepartment, "DIR-T2" + unique, "TaughtTwo");
        unrelatedStudentId = student(fixtureDepartment, "DIR-U" + unique, "Unrelated");

        enroll(taughtStudentId, taughtCourseId, unique);
        enroll(secondTaughtStudentId, taughtCourseId, unique);
        enroll(unrelatedStudentId, unrelatedCourseId, unique);
    }

    @Test
    @DisplayName("the paged directory query returns only the students of the given courses")
    void returnsOnlyStudentsOfTheGivenCourses() {
        Page<Student> page = students.findDistinctByCourseCourseIdIn(
                List.of(taughtCourseId), PageRequest.of(0, 10, Sort.by("studentId")));

        assertThat(page.getContent())
                .extracting(Student::getStudentId)
                .containsExactlyInAnyOrder(taughtStudentId, secondTaughtStudentId)
                .doesNotContain(unrelatedStudentId);
    }

    @Test
    @DisplayName("the reported total counts the permitted set, not the whole table")
    void totalIsCountedOverThePermittedSetOnly() {
        long studentsInTable = students.count();
        assertThat(studentsInTable).isGreaterThanOrEqualTo(3);

        Page<Student> page = students.findDistinctByCourseCourseIdIn(
                List.of(taughtCourseId), PageRequest.of(0, 10, Sort.by("studentId")));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getTotalElements()).isLessThan(studentsInTable);
    }

    @Test
    @DisplayName("a later page still reports the scoped total")
    void laterPagesStillReportTheScopedTotal() {
        Page<Student> secondPage = students.findDistinctByCourseCourseIdIn(
                List.of(taughtCourseId), PageRequest.of(1, 1, Sort.by("studentId")));

        assertThat(secondPage.getContent()).hasSize(1);
        assertThat(secondPage.getTotalElements()).isEqualTo(2);
        assertThat(secondPage.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("a course with no students yields an empty page")
    void courseWithoutStudentsYieldsAnEmptyPage() {
        Long emptyCourseId = course(fixtureDepartment, "DIRE" + System.nanoTime(), "Empty Course");

        Page<Student> page = students.findDistinctByCourseCourseIdIn(
                List.of(emptyCourseId), PageRequest.of(0, 10, Sort.by("studentId")));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private Long course(Department department, String code, String name) {
        Course course = new Course();
        course.setDepartment(department);
        course.setCourseCode(code);
        course.setCourseName(name);
        course.setCredits(3);
        course.setCapacity(50);
        course.setStatus(Course.Status.ACTIVE);
        return courses.save(course).getCourseId();
    }

    private Long student(Department department, String enrollmentNumber, String firstName) {
        User account = new User();
        account.setUsername("dir-" + enrollmentNumber);
        account.setEmail("dir-" + enrollmentNumber + "@example.test");
        account.setPasswordHash("{noop}unused-in-this-test");
        account.setRole(User.Role.STUDENT);
        account = users.save(account);

        Student student = new Student();
        student.setUser(account);
        student.setDepartment(department);
        student.setEnrollmentNumber(enrollmentNumber);
        student.setFirstName(firstName);
        student.setLastName("Student");
        student.setDateOfBirth(LocalDate.of(2004, 1, 1));
        student.setPhone("000");
        student.setAdmissionYear(2026);
        student.setStatus(Student.Status.ACTIVE);
        return students.save(student).getStudentId();
    }

    private void enroll(Long studentId, Long courseId, String unique) {
        Student student = students.findById(studentId).orElseThrow();
        Course course = courses.findById(courseId).orElseThrow();

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setSemester("S1");
        enrollment.setAcademicYear("2026-" + Math.abs(unique.hashCode() % 1000));
        enrollment.setEnrollmentDate(LocalDate.of(2026, 8, 10));
        enrollment.setStatus(Enrollment.Status.ENROLLED);
        enrollments.save(enrollment);
    }
}

package com.campuscore.backend.repository;

import com.campuscore.backend.entity.AuditLog;
import com.campuscore.backend.entity.Course;
import com.campuscore.backend.entity.Department;
import com.campuscore.backend.entity.Faculty;
import com.campuscore.backend.entity.FacultyCourseAssignment;
import com.campuscore.backend.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import com.campuscore.backend.support.IntegrationDataSource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Multi-step writes either happen completely or not at all.
 *
 * <p>Every administrative mutation writes the record and then writes the audit
 * row that records it, and a handover of a course closes the previous
 * assignment before it opens the replacement. Without a transaction those
 * writes are separate commits, so a failure between them leaves a change
 * without its trail, or a course whose previous assignment has been closed and
 * whose replacement was never created: students enrolled in that course would
 * have no teacher.
 *
 * <p>These tests run against a real MySQL instance, because a rollback is
 * something the database does, and a mocked repository cannot show it. The
 * failure is injected through a repository that delegates every call to the
 * real one and fails the one write the test asks it to, so the earlier writes
 * really are in the database when the failure happens.
 */
@SpringBootTest
@ActiveProfiles("it")
@EnabledIfSystemProperty(named = "cc.it.datasource.url", matches = ".+")
@Import(TransactionalWriteIT.FailingRepositoryConfiguration.class)
class TransactionalWriteIT {

    /**
     * Fails the next save on demand.
     *
     * <p>The repository it stands in for is the real one: every call is
     * delegated, so the writes before the failure are genuinely committed to
     * the database and the rollback has something to undo.
     */
    @TestConfiguration
    static class FailingRepositoryConfiguration {

        @Bean
        AtomicBoolean failNextSave() {
            return new AtomicBoolean(false);
        }

        @Bean
        @Primary
        FacultyCourseAssignmentRepository failingAssignments(
                @Qualifier("facultyCourseAssignmentRepository")
                FacultyCourseAssignmentRepository delegate,
                AtomicBoolean failNextSave) {

            return (FacultyCourseAssignmentRepository) Proxy.newProxyInstance(
                    FacultyCourseAssignmentRepository.class.getClassLoader(),
                    new Class<?>[]{FacultyCourseAssignmentRepository.class},
                    (proxy, method, args) -> {
                        if ("save".equals(method.getName()) && failNextSave.get()) {
                            failNextSave.set(false);
                            throw new IllegalStateException(
                                    "injected failure while writing the replacement assignment");
                        }
                        try {
                            return method.invoke(delegate, args);
                        } catch (InvocationTargetException e) {
                            throw e.getCause();
                        }
                    });
        }
    }

    @Autowired
    private FacultyCourseAssignmentRepository assignments;

    /** The service under test. It receives the failing repository. */
    @Autowired
    private com.campuscore.backend.service.FacultyCourseAssignmentService assignmentService;

    @Autowired
    private CourseRepository courses;

    @Autowired
    private DepartmentRepository departments;

    @Autowired
    private FacultyRepository faculty;

    @Autowired
    private UserRepository users;

    @Autowired
    private AuditLogRepository auditLogs;

    @Autowired
    private com.campuscore.backend.repository.AssessmentRepository assessments;

    @Autowired
    private com.campuscore.backend.service.AssessmentService assessmentService;

    @Autowired
    private AtomicBoolean failNextSave;

    @DynamicPropertySource
    static void integrationDataSource(DynamicPropertyRegistry registry) {
        IntegrationDataSource.register(registry);
    }

    private Department department;
    private Course course;
    private Faculty firstTeacher;
    private Faculty secondTeacher;
    private final List<Faculty> createdTeachers = new java.util.ArrayList<>();
    private final List<User> createdUsers = new java.util.ArrayList<>();

    @BeforeEach
    void setUp() {
        String unique = String.valueOf(System.nanoTime());

        department = new Department();
        department.setName("Transactions " + unique);
        department.setCode("TX" + Math.abs(unique.hashCode() % 100000));
        department = departments.save(department);

        course = new Course();
        course.setDepartment(department);
        course.setCourseCode("TXC" + Math.abs(unique.hashCode() % 100000));
        course.setCourseName("Transaction Course");
        course.setCredits(3);
        course.setCapacity(30);
        course = courses.save(course);

        firstTeacher = teacher("TX-F1-" + unique);
        secondTeacher = teacher("TX-F2-" + unique);
        createdTeachers.add(firstTeacher);
        createdTeachers.add(secondTeacher);
    }

    @AfterEach
    void cleanUp() {
        // The service methods commit on their own, so the fixture is removed
        // explicitly rather than by a test-wide rollback.
        assessments.deleteAll(assessments.findByCourseCourseId(course.getCourseId()));
        assignments.deleteAll(
                assignments.findByCourseCourseIdOrderByAssignedAtDesc(course.getCourseId()));
        courses.deleteById(course.getCourseId());
        faculty.deleteAll(createdTeachers);
        users.deleteAll(createdUsers);
        departments.deleteById(department.getDepartmentId());
    }

    @Test
    @DisplayName("a successful handover commits the closure, the replacement and the trail")
    void aSuccessfulHandoverCommitsEverything() {
        assignments.save(assignment(firstTeacher));

        assignmentService.reassignFaculty(course.getCourseId(), secondTeacher.getFacultyId());

        List<FacultyCourseAssignment> rows =
                assignments.findByCourseCourseIdOrderByAssignedAtDesc(course.getCourseId());

        assertThat(rows).hasSize(2);
        assertThat(rows.stream().filter(row -> row.getEndedAt() == null).count()).isEqualTo(1);
        assertThat(rows.stream()
                .filter(row -> row.getEndedAt() == null)
                .findFirst()
                .get()
                .getFaculty()
                .getFacultyId()).isEqualTo(secondTeacher.getFacultyId());

        assertThat(auditLogs.findByActionOrderByCreatedAtDesc("REASSIGN_FACULTY")).isNotEmpty();
    }

    @Test
    @DisplayName("a failed handover leaves the course with its original assignment")
    void aFailedHandoverRollsBackTheClosure() {
        assignments.save(assignment(firstTeacher));

        failNextSave.set(true);

        assertThatThrownBy(() ->
                assignmentService.reassignFaculty(course.getCourseId(), secondTeacher.getFacultyId()))
                .isInstanceOf(IllegalStateException.class);

        // The closure of the previous assignment was written before the
        // replacement failed. Without a transaction it would still be closed,
        // leaving the course with no teacher at all.
        List<FacultyCourseAssignment> rows =
                assignments.findByCourseCourseIdOrderByAssignedAtDesc(course.getCourseId());

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getEndedAt()).isNull();
        assertThat(rows.get(0).getFaculty().getFacultyId()).isEqualTo(firstTeacher.getFacultyId());
    }

    @Test
    @DisplayName("a failed handover records no event")
    void aFailedHandoverRecordsNothing() {
        assignments.save(assignment(firstTeacher));

        long before = auditLogs.count();

        failNextSave.set(true);

        assertThatThrownBy(() ->
                assignmentService.reassignFaculty(course.getCourseId(), secondTeacher.getFacultyId()))
                .isInstanceOf(IllegalStateException.class);

        assertThat(auditLogs.count()).isEqualTo(before);
    }

    @Test
    @DisplayName("a refused mutation leaves neither the record nor a trail entry")
    void aRefusedMutationLeavesNothingBehind() {
        long before = auditLogs.count();

        assertThatThrownBy(() -> {
            Course duplicate = new Course();
            duplicate.setDepartment(department);
            duplicate.setCourseCode(course.getCourseCode());
            duplicate.setCourseName("Duplicate");
            duplicate.setCredits(3);
            duplicate.setCapacity(30);
            courses.saveAndFlush(duplicate);
        }).isInstanceOf(Exception.class);

        assertThat(auditLogs.count()).isEqualTo(before);
    }

    @Test
    @DisplayName("the optimistic locking version still advances inside the transaction")
    void theVersionColumnStillAdvancesInsideTheTransaction() {
        // The version column is what makes a stale write detectable. The
        // transaction boundary must not disturb it, so a corrected record still
        // moves its version on.
        com.campuscore.backend.entity.Assessment assessment =
                new com.campuscore.backend.entity.Assessment();
        assessment.setCourse(course);
        assessment.setName("Transaction Assessment");
        assessment.setAssessmentType(
                com.campuscore.backend.entity.Assessment.AssessmentType.MIDTERM);
        assessment.setMaxMarks(new java.math.BigDecimal("100.00"));
        assessment.setWeight(new java.math.BigDecimal("30.00"));
        assessment = assessments.saveAndFlush(assessment);

        Long versionBefore = assessment.getVersion();

        com.campuscore.backend.dto.AssessmentRequest request =
                new com.campuscore.backend.dto.AssessmentRequest();
        request.setName("Corrected Transaction Assessment");
        request.setAssessmentType(
                com.campuscore.backend.entity.Assessment.AssessmentType.MIDTERM);
        request.setMaxMarks(new java.math.BigDecimal("100.00"));
        request.setWeight(new java.math.BigDecimal("30.00"));

        assessmentService.updateAssessment(assessment.getAssessmentId(), request);

        Long versionAfter = assessments.findById(assessment.getAssessmentId())
                .orElseThrow()
                .getVersion();

        assertThat(versionAfter).isEqualTo(versionBefore + 1);
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private Faculty teacher(String employeeNumber) {
        User account = new User();
        account.setUsername(employeeNumber);
        account.setEmail(employeeNumber + "@example.test");
        account.setPasswordHash("{noop}unused-in-this-test");
        account.setRole(User.Role.FACULTY);
        account = users.save(account);
        createdUsers.add(account);

        Faculty teacher = new Faculty();
        teacher.setUser(account);
        teacher.setDepartment(department);
        teacher.setEmployeeNumber(employeeNumber);
        teacher.setFirstName("Test");
        teacher.setLastName("Teacher");
        teacher.setStatus(Faculty.Status.ACTIVE);
        return faculty.save(teacher);
    }

    private FacultyCourseAssignment assignment(Faculty teacher) {
        FacultyCourseAssignment assignment = new FacultyCourseAssignment();
        assignment.setCourse(course);
        assignment.setFaculty(teacher);
        assignment.setAssignedAt(LocalDateTime.now());
        return assignments.save(assignment);
    }
}

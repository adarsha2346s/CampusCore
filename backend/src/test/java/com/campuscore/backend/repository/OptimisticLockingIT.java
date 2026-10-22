package com.campuscore.backend.repository;

import com.campuscore.backend.entity.Assessment;
import com.campuscore.backend.entity.Course;
import com.campuscore.backend.entity.Department;
import com.campuscore.backend.entity.Enrollment;
import com.campuscore.backend.entity.Mark;
import com.campuscore.backend.entity.Student;
import com.campuscore.backend.entity.User;
import com.campuscore.backend.support.IntegrationDataSource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Optimistic locking on the academic records that gained an update route in Phase 3.
 *
 * <p>Runs against a real database rather than mocks, because the guarantee under
 * test is what Hibernate emits on the UPDATE statement, not what the service
 * intends.
 *
 * <p>Each test loads a record into a detached copy and lets a second writer commit
 * before the detached copy is written back. That is the shape of the actual race:
 * two members of staff open the same record, the first one's save wins, and the
 * second one's now-stale copy must be rejected rather than silently overwriting it.
 * Re-reading the row inside each block would not test anything, because the second
 * block would simply observe the already-committed value.
 *
 * <p>Needs a datasource supplied with -Dcc.it.datasource.url and the V3 migration
 * applied, since the "it" profile validates the schema instead of creating it.
 */
@SpringBootTest
@ActiveProfiles("it")
@EnabledIfSystemProperty(named = "cc.it.datasource.url", matches = ".+")
@TestPropertySource(properties = {
        "jwt.secret=optimistic-locking-test-secret-value-32b",
        "jwt.expiration=3600000",
        "app.cors.allowed-origins=http://localhost:5173"
})
class OptimisticLockingIT {

    /**
     * Connects the opt-in integration datasource onto Spring Boot's own
     * datasource properties. Without this the context has no driver and the
     * test cannot run at all.
     */
    @DynamicPropertySource
    static void integrationDataSource(DynamicPropertyRegistry registry) {
        IntegrationDataSource.register(registry);
    }

    @Autowired
    private MarkRepository marks;

    @Autowired
    private AssessmentRepository assessments;

    @Autowired
    private EnrollmentRepository enrollments;

    @Autowired
    private StudentRepository students;

    @Autowired
    private CourseRepository courses;

    @Autowired
    private DepartmentRepository departments;

    @Autowired
    private UserRepository users;

    @Autowired
    private TransactionTemplate transactions;

    @PersistenceContext
    private EntityManager entityManager;

    private long markId;
    private long assessmentId;
    private long enrollmentId;

    /**
     * The fixture is committed rather than rolled back, because the race under test
     * needs real committed state in separate persistence contexts. That makes it
     * non-idempotent, so identifiers are unique per invocation rather than relying
     * on a rollback to clean up.
     */
    @BeforeEach
    void setUp() {
        String unique = String.valueOf(System.nanoTime());

        transactions.executeWithoutResult(status -> {
            Department department = new Department();
            department.setName("Locking Dept " + unique);
            department.setCode("LKD" + Math.abs(unique.hashCode() % 100000));
            department = departments.save(department);

            Course course = new Course();
            course.setDepartment(department);
            course.setCourseCode("LK" + Math.abs(unique.hashCode() % 100000));
            course.setCourseName("Locking Basics");
            course.setCredits(3);
            course.setCapacity(30);
            course = courses.save(course);

            User user = new User();
            user.setUsername("lock-" + unique);
            user.setEmail("lock-" + unique + "@example.test");
            user.setPasswordHash("{noop}unused-in-this-test");
            user.setRole(User.Role.STUDENT);
            user = users.save(user);

            Student student = new Student();
            student.setUser(user);
            student.setDepartment(department);
            student.setEnrollmentNumber("LK-" + unique);
            student.setFirstName("Lock");
            student.setLastName("Ing");
            student.setDateOfBirth(LocalDate.of(2004, 1, 1));
            student.setPhone("000");
            student.setAdmissionYear(2026);
            student.setStatus(Student.Status.ACTIVE);
            student = students.save(student);

            Enrollment enrollment = new Enrollment();
            enrollment.setStudent(student);
            enrollment.setCourse(course);
            enrollment.setSemester("Fall");
            enrollment.setAcademicYear("2026");
            enrollment.setEnrollmentDate(LocalDate.of(2026, 8, 20));
            enrollment = enrollments.save(enrollment);

            Assessment assessment = new Assessment();
            assessment.setCourse(course);
            assessment.setName("Locking Quiz");
            assessment.setAssessmentType(Assessment.AssessmentType.QUIZ);
            assessment.setMaxMarks(new BigDecimal("100.00"));
            assessment.setWeight(new BigDecimal("10.00"));
            assessment = assessments.save(assessment);

            Mark mark = new Mark();
            mark.setEnrollment(enrollment);
            mark.setAssessment(assessment);
            mark.setMarksObtained(new BigDecimal("50.00"));
            marks.save(mark);

            markId = mark.getMarkId();
            assessmentId = assessment.getAssessmentId();
            enrollmentId = enrollment.getEnrollmentId();
        });
    }

    /**
     * Takes a detached snapshot of a record, which is what a browser tab holds
     * between rendering the row and posting the edit back.
     */
    private <T> T staleCopy(Supplier<T> loader) {
        return transactions.execute(status -> {
            T entity = loader.get();
            entityManager.detach(entity);
            return entity;
        });
    }

    private <T> T fresh(Supplier<T> loader) {
        return transactions.execute(status -> loader.get());
    }

    @Test
    @DisplayName("Two staff editing the same mark: the second save is rejected, the first survives")
    void concurrentMarkEditLosesNothing() {
        Mark stale = staleCopy(() -> marks.findById(markId).orElseThrow());
        long staleVersion = stale.getVersion();

        transactions.executeWithoutResult(status -> {
            Mark winner = marks.findById(markId).orElseThrow();
            winner.setMarksObtained(new BigDecimal("77.00"));
            marks.save(winner);
        });

        stale.setMarksObtained(new BigDecimal("12.00"));

        assertThatThrownBy(() -> transactions.executeWithoutResult(status -> marks.save(stale)))
                .isInstanceOf(OptimisticLockingFailureException.class);

        assertThat(fresh(() -> marks.findById(markId).orElseThrow()).getMarksObtained())
                .isEqualByComparingTo("77.00");
        assertThat(fresh(() -> marks.findById(markId).orElseThrow()).getVersion())
                .isEqualTo(staleVersion + 1);
    }

    @Test
    @DisplayName("The version column is incremented on every accepted write")
    void versionIncrementsOnWrite() {
        long before = fresh(() -> marks.findById(markId).orElseThrow()).getVersion();

        transactions.executeWithoutResult(status -> {
            Mark managed = marks.findById(markId).orElseThrow();
            managed.setMarksObtained(new BigDecimal("61.00"));
            marks.save(managed);
        });

        assertThat(fresh(() -> marks.findById(markId).orElseThrow()).getVersion())
                .isEqualTo(before + 1);
    }

    @Test
    @DisplayName("Two staff editing the same assessment: the second save is rejected")
    void concurrentAssessmentEditLosesNothing() {
        Assessment stale = staleCopy(() -> assessments.findById(assessmentId).orElseThrow());

        transactions.executeWithoutResult(status -> {
            Assessment winner = assessments.findById(assessmentId).orElseThrow();
            winner.setName("Locking Quiz v2");
            assessments.save(winner);
        });

        stale.setName("Locking Quiz v3");

        assertThatThrownBy(() ->
                transactions.executeWithoutResult(status -> assessments.save(stale)))
                .isInstanceOf(OptimisticLockingFailureException.class);

        assertThat(fresh(() -> assessments.findById(assessmentId).orElseThrow()).getName())
                .isEqualTo("Locking Quiz v2");
    }

    @Test
    @DisplayName("Two staff editing the same enrollment: a stale status change is rejected")
    void concurrentEnrollmentEditLosesNothing() {
        Enrollment stale = staleCopy(() -> enrollments.findById(enrollmentId).orElseThrow());

        transactions.executeWithoutResult(status -> {
            Enrollment winner = enrollments.findById(enrollmentId).orElseThrow();
            winner.setStatus(Enrollment.Status.DROPPED);
            enrollments.save(winner);
        });

        stale.setStatus(Enrollment.Status.COMPLETED);

        assertThatThrownBy(() ->
                transactions.executeWithoutResult(status -> enrollments.save(stale)))
                .isInstanceOf(OptimisticLockingFailureException.class);

        assertThat(fresh(() -> enrollments.findById(enrollmentId).orElseThrow()).getStatus())
                .isEqualTo(Enrollment.Status.DROPPED);
    }
}
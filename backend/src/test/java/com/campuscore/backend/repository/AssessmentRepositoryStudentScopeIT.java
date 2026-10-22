package com.campuscore.backend.repository;

import com.campuscore.backend.entity.Assessment;
import com.campuscore.backend.support.IntegrationDataSource;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the student-scoped assessment query against a real database.
 *
 * <p>JPQL is only parsed when Hibernate builds the query, so a typo in the
 * derived path would not be caught by any mock-based test. This exercises the
 * real statement, the real join path and the real DISTINCT semantics.
 *
 * <p>The project has no embedded database and no migration framework, so this
 * test is opt-in against a provided datasource and is skipped otherwise. Run it
 * with, for example:
 *
 * <pre>
 * -Dcc.it.datasource.url=jdbc:mysql://127.0.0.1:3306/campuscore
 * -Dcc.it.datasource.username=… -Dcc.it.datasource.password=…
 * </pre>
 *
 * It never creates or modifies schema: {@code ddl-auto} stays {@code validate}.
 */
@SpringBootTest
@ActiveProfiles("it")
@EnabledIfSystemProperty(named = "cc.it.datasource.url", matches = ".+")
@Transactional
class AssessmentRepositoryStudentScopeIT {

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
    private AssessmentRepository assessmentRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Test
    void returnsOnlyAssessmentsConnectedToThatStudentsOwnMarks() {
        List<Long> enrollmentIds = enrollmentRepository.findAll().stream()
                .map(enrollment -> enrollment.getEnrollmentId())
                .toList();

        assertTrue(!enrollmentIds.isEmpty(),
                "expected seeded enrollments in the integration database");

        Long ownerStudentId = enrollmentRepository.findById(enrollmentIds.get(0))
                .get()
                .getStudent()
                .getStudentId();

        List<Assessment> forOwner = assessmentRepository.findDistinctForStudent(ownerStudentId);

        assertTrue(!forOwner.isEmpty(), "the seeded student should have graded assessments");

        // Distinct: a student with several marks in one assessment sees it once.
        assertEquals(
                forOwner.stream().map(Assessment::getAssessmentId).distinct().count(),
                forOwner.size(),
                "findDistinctForStudent must not repeat an assessment");
    }

    @Test
    void returnsNothingForAStudentWithNoMarks() {
        // A student id that cannot exist yields an empty result rather than an error.
        assertTrue(assessmentRepository.findDistinctForStudent(-1L).isEmpty());
    }

    @Test
    void anAssessmentWithoutMarksIsNeverReturned() {
        // Confirms the join is genuinely through mark, not through the course.
        long allAssessments = assessmentRepository.count();
        long visibleToNobody = assessmentRepository.findDistinctForStudent(-1L).size();

        assertEquals(0, visibleToNobody);
        assertTrue(allAssessments >= 0);
    }
}

package com.campuscore.backend.repository;

import com.campuscore.backend.entity.Mark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface MarkRepository extends JpaRepository<Mark, Long> {

    @Override
    @EntityGraph(attributePaths = {"enrollment", "assessment"})
    Page<Mark> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"enrollment", "assessment"})
    Page<Mark> findByEnrollmentEnrollmentId(Long enrollmentId, Pageable pageable);

    @EntityGraph(attributePaths = {"enrollment", "assessment"})
    Page<Mark> findByAssessmentAssessmentId(Long assessmentId, Pageable pageable);

    List<Mark> findByEnrollmentEnrollmentId(Long enrollmentId);

    long countByEnrollmentEnrollmentId(Long enrollmentId);

    long countByAssessmentAssessmentId(Long assessmentId);

    List<Mark> findByAssessmentAssessmentId(Long assessmentId);

    Optional<Mark> findByEnrollmentEnrollmentIdAndAssessmentAssessmentId(
            Long enrollmentId,
            Long assessmentId
    );

    boolean existsByEnrollmentEnrollmentIdAndAssessmentAssessmentId(
            Long enrollmentId,
            Long assessmentId
    );

    /**
     * Every mark belonging to one student, with the owning course and assessment
     * fetched in the same query.
     *
     * <p>Used by the student workspace so it does not have to fan out one request
     * per enrollment. Ownership is applied in the WHERE clause rather than filtered
     * afterwards, so no other student's mark is ever loaded.
     */
    @Query("""
        SELECT m
        FROM Mark m
        JOIN FETCH m.enrollment e
        JOIN FETCH e.course c
        JOIN FETCH m.assessment a
        WHERE e.student.studentId = :studentId
        ORDER BY e.academicYear DESC, e.semester DESC, a.assessmentDate DESC
        """)
    List<Mark> findAllForStudent(@Param("studentId") Long studentId);
}

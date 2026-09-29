package com.campuscore.backend.repository;

import com.campuscore.backend.entity.Mark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    List<Mark> findByAssessmentAssessmentId(Long assessmentId);

    Optional<Mark> findByEnrollmentEnrollmentIdAndAssessmentAssessmentId(
            Long enrollmentId,
            Long assessmentId
    );

    boolean existsByEnrollmentEnrollmentIdAndAssessmentAssessmentId(
            Long enrollmentId,
            Long assessmentId
    );
}

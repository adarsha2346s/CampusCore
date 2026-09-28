package com.campuscore.backend.repository;

import com.campuscore.backend.entity.Mark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MarkRepository extends JpaRepository<Mark, Long> {

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
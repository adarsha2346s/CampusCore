package com.campuscore.backend.repository;

import com.campuscore.backend.entity.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    @Override
    @EntityGraph(attributePaths = "course")
    Page<Assessment> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "course")
    Page<Assessment> findByCourseCourseId(Long courseId, Pageable pageable);

    List<Assessment> findByCourseCourseId(Long courseId);
}

package com.campuscore.backend.repository;

import com.campuscore.backend.entity.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    List<Assessment> findByCourseCourseId(Long courseId);
}

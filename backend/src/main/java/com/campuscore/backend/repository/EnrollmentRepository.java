package com.campuscore.backend.repository;

import com.campuscore.backend.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    @Override
    @EntityGraph(attributePaths = {"student", "course"})
    Page<Enrollment> findAll(Pageable pageable);

    List<Enrollment> findByStudentStudentId(Long studentId);

    List<Enrollment> findByCourseCourseId(Long courseId);

    boolean existsByStudentStudentIdAndCourseCourseIdAndSemesterAndAcademicYear(
            Long studentId,
            Long courseId,
            String semester,
            String academicYear
    );
}

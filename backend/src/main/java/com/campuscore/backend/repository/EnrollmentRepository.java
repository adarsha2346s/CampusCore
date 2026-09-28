package com.campuscore.backend.repository;

import com.campuscore.backend.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByStudentStudentId(Long studentId);

    List<Enrollment> findByCourseCourseId(Long courseId);

    boolean existsByStudentStudentIdAndCourseCourseIdAndSemesterAndAcademicYear(
            Long studentId,
            Long courseId,
            String semester,
            String academicYear
    );
}
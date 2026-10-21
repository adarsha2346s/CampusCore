package com.campuscore.backend.repository;

import com.campuscore.backend.entity.FacultyCourseAssignment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FacultyCourseAssignmentRepository
        extends JpaRepository<FacultyCourseAssignment, Long> {

    @EntityGraph(attributePaths = {"course", "faculty"})
    List<FacultyCourseAssignment> findByCourseCourseIdOrderByAssignedAtDesc(Long courseId);

    @EntityGraph(attributePaths = {"course", "faculty"})
    List<FacultyCourseAssignment>
            findByCourseCourseIdAndEndedAtIsNullOrderByAssignedAtDesc(Long courseId);

    @EntityGraph(attributePaths = {"course", "faculty"})
    List<FacultyCourseAssignment>
            findByFacultyFacultyIdAndEndedAtIsNullOrderByAssignedAtDesc(Long facultyId);

    @EntityGraph(attributePaths = {"course", "faculty"})
    List<FacultyCourseAssignment>
            findByFacultyFacultyIdOrderByAssignedAtDesc(Long facultyId);

    @EntityGraph(attributePaths = {"course", "faculty"})
    Optional<FacultyCourseAssignment>
            findByCourseCourseIdAndFacultyFacultyIdAndEndedAtIsNull(
                    Long courseId, Long facultyId);

    boolean existsByCourseCourseIdAndFacultyFacultyIdAndEndedAtIsNull(
            Long courseId, Long facultyId);

    long countByCourseCourseId(Long courseId);

    long countByFacultyFacultyId(Long facultyId);

    long countByCourseCourseIdAndEndedAtIsNull(Long courseId);

    long countByFacultyFacultyIdAndEndedAtIsNull(Long facultyId);
}

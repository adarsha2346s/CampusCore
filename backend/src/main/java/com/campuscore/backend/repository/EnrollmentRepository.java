package com.campuscore.backend.repository;

import com.campuscore.backend.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    /**
     * Overridden to fetch the owning student and course. Scope checks compare
     * identifiers, and reading them from an uninitialised proxy would depend on
     * the session still being open.
     */
    @Override
    @EntityGraph(attributePaths = {"student", "course"})
    Optional<Enrollment> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"student", "course"})
    Page<Enrollment> findAll(Pageable pageable);

    /**
     * Fetch the owning student and course. The enrollment response surfaces the
     * student's identity, and these are read outside a service transaction, so
     * leaving the associations lazy would fail on access.
     */
    @EntityGraph(attributePaths = {"student", "course"})
    List<Enrollment> findByStudentStudentId(Long studentId);

    /**
     * A student's enrollments in a set of courses.
     *
     * <p>Used to answer "which of this student's courses am I allowed to see"
     * without sending the caller every enrollment the student holds.
     */
    @EntityGraph(attributePaths = {"student", "course"})
    List<Enrollment> findByStudentStudentIdAndCourseCourseIdIn(
            Long studentId, Collection<Long> courseIds);

    @EntityGraph(attributePaths = {"student", "course"})
    List<Enrollment> findByCourseCourseId(Long courseId);

    boolean existsByStudentStudentIdAndCourseCourseIdAndSemesterAndAcademicYear(
            Long studentId,
            Long courseId,
            String semester,
            String academicYear
    );

    long countByCourseCourseId(Long courseId);

    long countByStudentStudentId(Long studentId);

    boolean existsByStudentStudentId(Long studentId, Long enrollmentId);

    boolean existsByStudentStudentIdAndCourseCourseIdIn(
            Long studentId, Collection<Long> courseIds);

    List<Long> findDistinctCourseCourseIdByStudentStudentId(Long studentId);

    /** Students enrolled in any of the given courses, used to scope the faculty directory. */
    @Query("""
        SELECT DISTINCT e.student
        FROM Enrollment e
        WHERE e.course.courseId IN :courseIds
        """)
    List<com.campuscore.backend.entity.Student> findDistinctStudentsByCourseCourseIdIn(
            @Param("courseIds") java.util.Collection<Long> courseIds);
}

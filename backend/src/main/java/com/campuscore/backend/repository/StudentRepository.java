package com.campuscore.backend.repository;

import com.campuscore.backend.entity.Student;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    @Override
    @EntityGraph(attributePaths = {"user", "department"})
    Page<Student> findAll(Pageable pageable);

    /**
     * Students enrolled in any of the given courses, one page at a time.
     *
     * <p>This is the faculty directory query. It walks the real schema path
     * student &lt;- enrollment -&gt; course, so the filter is applied by the
     * database rather than by fetching every student and discarding most of
     * them afterwards.
     *
     * <p>The count query counts the same filtered set, so the reported total is
     * the number of students the caller may see and never the size of the
     * institution's directory.
     */
    @Query(value = """
        SELECT DISTINCT s
        FROM Student s
        JOIN Enrollment e ON e.student = s
        WHERE e.course.courseId IN :courseIds
        """,
            countQuery = """
        SELECT COUNT(DISTINCT s)
        FROM Student s
        JOIN Enrollment e ON e.student = s
        WHERE e.course.courseId IN :courseIds
        """)
    @EntityGraph(attributePaths = {"user", "department"})
    Page<Student> findDistinctByCourseCourseIdIn(
            @Param("courseIds") Collection<Long> courseIds, Pageable pageable);

    Optional<Student> findByEnrollmentNumber(String enrollmentNumber);

    boolean existsByEnrollmentNumber(String enrollmentNumber);

    boolean existsByUserUserId(Long userId);

    @EntityGraph(attributePaths = "department")
    Optional<Student> findByUserUserId(Long userId);

    long countByDepartmentDepartmentId(Long departmentId);
}

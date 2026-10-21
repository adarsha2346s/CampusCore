package com.campuscore.backend.repository;

import com.campuscore.backend.entity.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    @Override
    @EntityGraph(attributePaths = "course")
    Page<Assessment> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "course")
    Page<Assessment> findByCourseCourseId(Long courseId, Pageable pageable);

    List<Assessment> findByCourseCourseId(Long courseId);

    long countByCourseCourseId(Long courseId);

    /**
     * Assessments connected to a student's own marks.
     *
     * <p>Walks the real schema path assessment &lt;- mark -&gt; enrollment -&gt;
     * student, so a student only ever sees assessments their own work is graded
     * against. No relationship is invented and no row is joined that the student
     * does not already own through their own mark.
     */
    @Query("""
        SELECT DISTINCT a
        FROM Assessment a
        JOIN Mark m ON m.assessment = a
        WHERE m.enrollment.student.studentId = :studentId
        """)
    List<Assessment> findDistinctForStudent(@Param("studentId") Long studentId);
}

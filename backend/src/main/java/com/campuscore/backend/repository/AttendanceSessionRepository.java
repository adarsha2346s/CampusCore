package com.campuscore.backend.repository;

import com.campuscore.backend.entity.AttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceSessionRepository
        extends JpaRepository<AttendanceSession, Long> {

    @Override
    @EntityGraph(attributePaths = {"course", "faculty"})
    Page<AttendanceSession> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"course", "faculty"})
    Page<AttendanceSession> findByCourseCourseId(Long courseId, Pageable pageable);

    @EntityGraph(attributePaths = {"course", "faculty"})
    Page<AttendanceSession> findByFacultyFacultyId(Long facultyId, Pageable pageable);

    List<AttendanceSession> findByCourseCourseId(Long courseId);

    List<AttendanceSession> findByFacultyFacultyId(Long facultyId);

    Optional<AttendanceSession> findByCourseCourseIdAndSessionDate(
            Long courseId,
            LocalDate sessionDate
    );

    boolean existsByCourseCourseIdAndSessionDate(
            Long courseId,
            LocalDate sessionDate
    );
}

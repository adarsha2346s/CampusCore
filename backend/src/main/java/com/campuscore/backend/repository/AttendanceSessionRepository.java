package com.campuscore.backend.repository;

import com.campuscore.backend.entity.AttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceSessionRepository
        extends JpaRepository<AttendanceSession, Long> {

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
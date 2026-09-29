package com.campuscore.backend.repository;

import com.campuscore.backend.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository
        extends JpaRepository<AttendanceRecord, Long> {

    @Override
    @EntityGraph(attributePaths = {"attendanceSession", "enrollment"})
    Page<AttendanceRecord> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"attendanceSession", "enrollment"})
    Page<AttendanceRecord> findByAttendanceSessionAttendanceSessionId(
            Long attendanceSessionId, Pageable pageable);

    @EntityGraph(attributePaths = {"attendanceSession", "enrollment"})
    Page<AttendanceRecord> findByEnrollmentEnrollmentId(Long enrollmentId, Pageable pageable);

    List<AttendanceRecord> findByAttendanceSessionAttendanceSessionId(
            Long attendanceSessionId
    );

    List<AttendanceRecord> findByEnrollmentEnrollmentId(
            Long enrollmentId
    );

    Optional<AttendanceRecord>
    findByAttendanceSessionAttendanceSessionIdAndEnrollmentEnrollmentId(
            Long attendanceSessionId,
            Long enrollmentId
    );

    boolean existsByAttendanceSessionAttendanceSessionIdAndEnrollmentEnrollmentId(
            Long attendanceSessionId,
            Long enrollmentId
    );
}

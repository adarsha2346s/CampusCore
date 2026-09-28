package com.campuscore.backend.repository;

import com.campuscore.backend.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository
        extends JpaRepository<AttendanceRecord, Long> {

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
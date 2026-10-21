package com.campuscore.backend.repository;

import com.campuscore.backend.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
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

    long countByEnrollmentEnrollmentId(Long enrollmentId);

    long countByAttendanceSessionAttendanceSessionId(Long attendanceSessionId);

    Optional<AttendanceRecord>
    findByAttendanceSessionAttendanceSessionIdAndEnrollmentEnrollmentId(
            Long attendanceSessionId,
            Long enrollmentId
    );

    boolean existsByAttendanceSessionAttendanceSessionIdAndEnrollmentEnrollmentId(
            Long attendanceSessionId,
            Long enrollmentId
    );

    /**
     * Every attendance row belonging to one student, with the session and course
     * fetched in the same query.
     *
     * <p>Ownership is applied in the WHERE clause, so a student only ever loads
     * their own rows.
     */
    @Query("""
        SELECT r
        FROM AttendanceRecord r
        JOIN FETCH r.attendanceSession s
        JOIN FETCH s.course c
        JOIN FETCH r.enrollment e
        WHERE e.student.studentId = :studentId
        ORDER BY s.sessionDate DESC, c.courseCode ASC
        """)
    List<AttendanceRecord> findAllForStudent(@Param("studentId") Long studentId);
}

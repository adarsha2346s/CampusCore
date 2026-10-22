package com.campuscore.backend.service;

import com.campuscore.backend.entity.AttendanceRecord;
import com.campuscore.backend.entity.AttendanceSession;
import com.campuscore.backend.entity.Enrollment;
import com.campuscore.backend.repository.AttendanceRecordRepository;
import com.campuscore.backend.repository.AttendanceSessionRepository;
import com.campuscore.backend.repository.EnrollmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Service
public class AttendanceRecordService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AccessScope accessScope;

    private final AuditLogService auditLogService;

    public AttendanceRecordService(
            AttendanceRecordRepository attendanceRecordRepository,
            AttendanceSessionRepository attendanceSessionRepository,
            EnrollmentRepository enrollmentRepository,
            AuditLogService auditLogService) {

        this(attendanceRecordRepository, attendanceSessionRepository, enrollmentRepository, null, auditLogService);
    }

        @Autowired
    public AttendanceRecordService(
            AttendanceRecordRepository attendanceRecordRepository,
            AttendanceSessionRepository attendanceSessionRepository,
            EnrollmentRepository enrollmentRepository,
            AccessScope accessScope,
            AuditLogService auditLogService) {

        this.attendanceRecordRepository = attendanceRecordRepository;
        this.attendanceSessionRepository = attendanceSessionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.accessScope = accessScope;
        this.auditLogService = auditLogService;
    }

    /**
     * Every attendance row belonging to one student, for the signed-in student's
     * own view.
     *
     * <p>Ownership is re-checked here rather than trusting the controller: the
     * controller derives the id from the verified session, and this guard makes
     * sure the service cannot be reached with someone else's id. Fails closed when
     * the scope service is absent.
     */
    public List<AttendanceRecord> getRecordsForStudent(Long studentId) {
        if (accessScope == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access scope is unavailable");
        }

        accessScope.requireStudentAccess(studentId);

        return attendanceRecordRepository.findAllForStudent(studentId);
    }

    public List<AttendanceRecord> getAllRecords() {
        requireAdminForGlobalListing();
        return attendanceRecordRepository.findAll();
    }

    public Page<AttendanceRecord> getRecordsPage(Pageable pageable) {
        requireAdminForGlobalListing();
        return attendanceRecordRepository.findAll(pageable);
    }

    public AttendanceRecord getRecordById(Long id) {
        AttendanceRecord record = attendanceRecordRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Attendance record not found"));
        requireEnrollmentAccess(record.getEnrollment().getEnrollmentId());
        return record;
    }

    public List<AttendanceRecord> getRecordsBySession(
            Long attendanceSessionId) {

        requireSessionAccess(attendanceSessionId);
        return attendanceRecordRepository
                .findByAttendanceSessionAttendanceSessionId(
                        attendanceSessionId
                );
    }

    public Page<AttendanceRecord> getRecordsBySessionPage(Long sessionId, Pageable pageable) {
        requireSessionAccess(sessionId);
        return attendanceRecordRepository.findByAttendanceSessionAttendanceSessionId(sessionId, pageable);
    }

    public List<AttendanceRecord> getRecordsByEnrollment(
            Long enrollmentId) {

        requireEnrollmentAccess(enrollmentId);
        return attendanceRecordRepository
                .findByEnrollmentEnrollmentId(enrollmentId);
    }

    public Page<AttendanceRecord> getRecordsByEnrollmentPage(Long enrollmentId, Pageable pageable) {
        requireEnrollmentAccess(enrollmentId);
        return attendanceRecordRepository.findByEnrollmentEnrollmentId(enrollmentId, pageable);
    }

    public AttendanceRecord createRecord(
            Long attendanceSessionId,
            Long enrollmentId,
            AttendanceRecord.Status status) {

        requireSessionAccess(attendanceSessionId);
        requireEnrollmentAccess(enrollmentId);

        if (attendanceRecordRepository
                .existsByAttendanceSessionAttendanceSessionIdAndEnrollmentEnrollmentId(
                        attendanceSessionId,
                        enrollmentId)) {

            throw new RuntimeException(
                    "Attendance already marked for this student in this session"
            );
        }

        AttendanceSession session =
                attendanceSessionRepository.findById(
                        attendanceSessionId
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Attendance session not found"
                        ));

        Enrollment enrollment =
                enrollmentRepository.findById(
                        enrollmentId
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Enrollment not found"
                        ));

        AttendanceRecord record = new AttendanceRecord();

        record.setAttendanceSession(session);
        record.setEnrollment(enrollment);
        record.setStatus(status);

        AttendanceRecord saved = attendanceRecordRepository.save(record);

        auditLogService.record(
                "CREATE_ATTENDANCE_RECORD",
                "AttendanceRecord",
                saved.getAttendanceRecordId(),
                "Marked attendance " + status.name() + " for enrollment "
                        + enrollmentId + " in session " + attendanceSessionId
        );

        return saved;
    }

    /**
     * An attendance record belongs to a session, so session scope decides
     * visibility. Fails closed when the scope service is not wired.
     */
    private void requireSessionAccess(Long sessionId) {
        if (accessScope == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access scope is unavailable");
        }

        AttendanceSession session = attendanceSessionRepository.findById(sessionId).orElse(null);

        if (session == null) {
            throw new com.campuscore.backend.exception.ResourceNotFoundException(
                    "Attendance session not found with ID: " + sessionId);
        }

        accessScope.requireCourseAccess(session.getCourse().getCourseId());
    }

    private void requireEnrollmentAccess(Long enrollmentId) {
        if (accessScope == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access scope is unavailable");
        }
        accessScope.requireEnrollmentAccess(enrollmentId);
    }

    /**
     * Guards the institution-wide listings.
     *
     * These getters are not scope filtered, so they are reserved for
     * administrators. Requiring the role here as well as in the request matcher
     * means a future route that calls one of them cannot silently expose every
     * record in the institution to a faculty member or a student.
     */
    private void requireAdminForGlobalListing() {
        if (accessScope == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access scope is unavailable");
        }
        if (!accessScope.isAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "This listing is restricted to administrators");
        }
    }

}

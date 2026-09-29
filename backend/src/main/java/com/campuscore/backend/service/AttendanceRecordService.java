package com.campuscore.backend.service;

import com.campuscore.backend.entity.AttendanceRecord;
import com.campuscore.backend.entity.AttendanceSession;
import com.campuscore.backend.entity.Enrollment;
import com.campuscore.backend.repository.AttendanceRecordRepository;
import com.campuscore.backend.repository.AttendanceSessionRepository;
import com.campuscore.backend.repository.EnrollmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Service
public class AttendanceRecordService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final EnrollmentRepository enrollmentRepository;

    public AttendanceRecordService(
            AttendanceRecordRepository attendanceRecordRepository,
            AttendanceSessionRepository attendanceSessionRepository,
            EnrollmentRepository enrollmentRepository) {

        this.attendanceRecordRepository = attendanceRecordRepository;
        this.attendanceSessionRepository = attendanceSessionRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    public List<AttendanceRecord> getAllRecords() {
        return attendanceRecordRepository.findAll();
    }

    public Page<AttendanceRecord> getRecordsPage(Pageable pageable) {
        return attendanceRecordRepository.findAll(pageable);
    }

    public AttendanceRecord getRecordById(Long id) {
        return attendanceRecordRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Attendance record not found"));
    }

    public List<AttendanceRecord> getRecordsBySession(
            Long attendanceSessionId) {

        return attendanceRecordRepository
                .findByAttendanceSessionAttendanceSessionId(
                        attendanceSessionId
                );
    }

    public Page<AttendanceRecord> getRecordsBySessionPage(Long sessionId, Pageable pageable) {
        return attendanceRecordRepository.findByAttendanceSessionAttendanceSessionId(sessionId, pageable);
    }

    public List<AttendanceRecord> getRecordsByEnrollment(
            Long enrollmentId) {

        return attendanceRecordRepository
                .findByEnrollmentEnrollmentId(enrollmentId);
    }

    public Page<AttendanceRecord> getRecordsByEnrollmentPage(Long enrollmentId, Pageable pageable) {
        return attendanceRecordRepository.findByEnrollmentEnrollmentId(enrollmentId, pageable);
    }

    public AttendanceRecord createRecord(
            Long attendanceSessionId,
            Long enrollmentId,
            AttendanceRecord.Status status) {

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

        return attendanceRecordRepository.save(record);
    }
}

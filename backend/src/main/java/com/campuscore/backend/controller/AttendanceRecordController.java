package com.campuscore.backend.controller;

import com.campuscore.backend.dto.AttendanceRecordResponse;
import com.campuscore.backend.dto.StudentAttendanceResponse;
import com.campuscore.backend.dto.PageResponse;
import com.campuscore.backend.lib.PageParameters;
import com.campuscore.backend.entity.AttendanceRecord;
import com.campuscore.backend.entity.Student;
import com.campuscore.backend.service.AttendanceRecordService;
import com.campuscore.backend.service.StudentService;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance-records")
public class AttendanceRecordController {

    private final AttendanceRecordService attendanceRecordService;
    private final StudentService studentService;

    public AttendanceRecordController(
            AttendanceRecordService attendanceRecordService) {
        this(attendanceRecordService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AttendanceRecordController(
            AttendanceRecordService attendanceRecordService,
            StudentService studentService) {
        this.attendanceRecordService = attendanceRecordService;
        this.studentService = studentService;
    }

    /**
     * The signed-in student's own attendance, with each row's course and session
     * already joined in.
     *
     * <p>This route replaces the workspace's previous approach of calling the
     * institution-wide session listing, which is administrator-only and therefore
     * refused every student. Taking no identifier, ownership is resolved from the
     * verified session rather than the request.
     */
    @GetMapping("/me")
    public ResponseEntity<List<StudentAttendanceResponse>> getMyAttendance(
            Authentication authentication) {

        if (studentService == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Student student = studentService.getStudentForUsername(authentication.getName());

        List<StudentAttendanceResponse> response =
                attendanceRecordService.getRecordsForStudent(student.getStudentId())
                        .stream()
                        .map(record -> new StudentAttendanceResponse(
                                record.getAttendanceRecordId(),
                                record.getAttendanceSession().getAttendanceSessionId(),
                                record.getAttendanceSession().getCourse().getCourseCode(),
                                record.getAttendanceSession().getCourse().getCourseName(),
                                record.getAttendanceSession().getSessionDate().toString(),
                                record.getAttendanceSession().getTopic(),
                                record.getStatus().name(),
                                record.getMarkedAt() == null ? null : record.getMarkedAt().toString()))
                        .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<?> getAllRecords(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        Pageable pageable = PageParameters.optional(page, size, "attendanceRecordId");
        if (pageable != null) {
            return ResponseEntity.ok(PageResponse.from(
                    attendanceRecordService.getRecordsPage(pageable), this::toResponse));
        }

        List<AttendanceRecordResponse> records =
                attendanceRecordService.getAllRecords()
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(records);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AttendanceRecordResponse> getRecordById(
            @PathVariable Long id) {

        AttendanceRecord record =
                attendanceRecordService.getRecordById(id);

        return ResponseEntity.ok(toResponse(record));
    }

    @GetMapping("/session/{attendanceSessionId}")
    public ResponseEntity<?> getRecordsBySession(
            @PathVariable Long attendanceSessionId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        Pageable pageable = PageParameters.optional(page, size, "attendanceRecordId");
        if (pageable != null) {
            return ResponseEntity.ok(PageResponse.from(
                    attendanceRecordService.getRecordsBySessionPage(attendanceSessionId, pageable), this::toResponse));
        }

        List<AttendanceRecordResponse> records =
                attendanceRecordService
                        .getRecordsBySession(attendanceSessionId)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(records);
    }

    @GetMapping("/enrollment/{enrollmentId}")
    public ResponseEntity<?> getRecordsByEnrollment(
            @PathVariable Long enrollmentId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        Pageable pageable = PageParameters.optional(page, size, "attendanceRecordId");
        if (pageable != null) {
            return ResponseEntity.ok(PageResponse.from(
                    attendanceRecordService.getRecordsByEnrollmentPage(enrollmentId, pageable), this::toResponse));
        }

        List<AttendanceRecordResponse> records =
                attendanceRecordService
                        .getRecordsByEnrollment(enrollmentId)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(records);
    }

    @PostMapping
    public ResponseEntity<AttendanceRecordResponse> createRecord(
            @RequestParam Long attendanceSessionId,
            @RequestParam Long enrollmentId,
            @RequestParam AttendanceRecord.Status status) {

        AttendanceRecord record =
                attendanceRecordService.createRecord(
                        attendanceSessionId,
                        enrollmentId,
                        status
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(record));
    }

    private AttendanceRecordResponse toResponse(
            AttendanceRecord record) {

        return new AttendanceRecordResponse(
                record.getAttendanceRecordId(),
                record.getAttendanceSession().getAttendanceSessionId(),
                record.getEnrollment().getEnrollmentId(),
                record.getStatus().name()
        );
    }
}

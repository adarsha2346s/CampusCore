package com.campuscore.backend.controller;

import com.campuscore.backend.dto.AttendanceRecordResponse;
import com.campuscore.backend.dto.PageResponse;
import com.campuscore.backend.lib.PageParameters;
import com.campuscore.backend.entity.AttendanceRecord;
import com.campuscore.backend.service.AttendanceRecordService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance-records")
public class AttendanceRecordController {

    private final AttendanceRecordService attendanceRecordService;

    public AttendanceRecordController(
            AttendanceRecordService attendanceRecordService) {
        this.attendanceRecordService = attendanceRecordService;
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

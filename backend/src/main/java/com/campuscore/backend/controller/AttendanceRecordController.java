package com.campuscore.backend.controller;

import com.campuscore.backend.dto.AttendanceRecordResponse;
import com.campuscore.backend.entity.AttendanceRecord;
import com.campuscore.backend.service.AttendanceRecordService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<AttendanceRecordResponse>> getAllRecords() {

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
    public ResponseEntity<List<AttendanceRecordResponse>> getRecordsBySession(
            @PathVariable Long attendanceSessionId) {

        List<AttendanceRecordResponse> records =
                attendanceRecordService
                        .getRecordsBySession(attendanceSessionId)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(records);
    }

    @GetMapping("/enrollment/{enrollmentId}")
    public ResponseEntity<List<AttendanceRecordResponse>> getRecordsByEnrollment(
            @PathVariable Long enrollmentId) {

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
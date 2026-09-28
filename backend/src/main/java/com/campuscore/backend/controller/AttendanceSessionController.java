package com.campuscore.backend.controller;

import com.campuscore.backend.dto.AttendanceSessionResponse;
import com.campuscore.backend.entity.AttendanceSession;
import com.campuscore.backend.service.AttendanceSessionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance-sessions")
public class AttendanceSessionController {

    private final AttendanceSessionService attendanceSessionService;

    public AttendanceSessionController(
            AttendanceSessionService attendanceSessionService) {
        this.attendanceSessionService = attendanceSessionService;
    }

    @GetMapping
    public ResponseEntity<List<AttendanceSessionResponse>> getAllSessions() {

        List<AttendanceSessionResponse> response =
                attendanceSessionService.getAllSessions()
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AttendanceSessionResponse> getSessionById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                toResponse(
                        attendanceSessionService.getSessionById(id)
                )
        );
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<AttendanceSessionResponse>> getSessionsByCourse(
            @PathVariable Long courseId) {

        List<AttendanceSessionResponse> response =
                attendanceSessionService.getSessionsByCourse(courseId)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/faculty/{facultyId}")
    public ResponseEntity<List<AttendanceSessionResponse>> getSessionsByFaculty(
            @PathVariable Long facultyId) {

        List<AttendanceSessionResponse> response =
                attendanceSessionService.getSessionsByFaculty(facultyId)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<AttendanceSessionResponse> createSession(
            @RequestParam Long courseId,
            @RequestParam Long facultyId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate sessionDate,
            @RequestParam String topic) {

        AttendanceSession session =
                attendanceSessionService.createSession(
                        courseId,
                        facultyId,
                        sessionDate,
                        topic
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(session));
    }

    private AttendanceSessionResponse toResponse(
            AttendanceSession session) {

        String sessionDate = null;

        if (session.getSessionDate() != null) {
            sessionDate = session.getSessionDate().toString();
        }

        return new AttendanceSessionResponse(
                session.getAttendanceSessionId(),
                session.getCourse().getCourseId(),
                session.getFaculty().getFacultyId(),
                sessionDate,
                session.getTopic()
        );
    }
}
package com.campuscore.backend.controller;

import com.campuscore.backend.dto.EnrollmentResponse;
import com.campuscore.backend.entity.Enrollment;
import com.campuscore.backend.service.EnrollmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @GetMapping
    public ResponseEntity<List<EnrollmentResponse>> getAllEnrollments() {

        List<EnrollmentResponse> response =
                enrollmentService.getAllEnrollments()
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EnrollmentResponse> getEnrollmentById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                toResponse(
                        enrollmentService.getEnrollmentById(id)
                )
        );
    }

    @PostMapping
    public ResponseEntity<EnrollmentResponse> createEnrollment(
            @RequestParam Long studentId,
            @RequestParam Long courseId,
            @RequestParam String semester,
            @RequestParam String academicYear) {

        Enrollment enrollment =
                enrollmentService.createEnrollment(
                        studentId,
                        courseId,
                        semester,
                        academicYear
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(enrollment));
    }

    private EnrollmentResponse toResponse(Enrollment enrollment) {

        String enrollmentDate = null;

        if (enrollment.getEnrollmentDate() != null) {
            enrollmentDate =
                    enrollment.getEnrollmentDate().toString();
        }

        return new EnrollmentResponse(
                enrollment.getEnrollmentId(),
                enrollment.getStudent().getStudentId(),
                enrollment.getCourse().getCourseId(),
                enrollment.getSemester(),
                enrollment.getAcademicYear(),
                enrollmentDate,
                enrollment.getStatus().name()
        );
    }
}
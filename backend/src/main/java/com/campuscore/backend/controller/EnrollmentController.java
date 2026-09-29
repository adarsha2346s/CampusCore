package com.campuscore.backend.controller;

import com.campuscore.backend.dto.EnrollmentResponse;
import com.campuscore.backend.dto.PageResponse;
import com.campuscore.backend.lib.PageParameters;
import com.campuscore.backend.entity.Enrollment;
import com.campuscore.backend.entity.Student;
import com.campuscore.backend.service.EnrollmentService;
import com.campuscore.backend.service.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final StudentService studentService;

    public EnrollmentController(
            EnrollmentService enrollmentService,
            StudentService studentService) {
        this.enrollmentService = enrollmentService;
        this.studentService = studentService;
    }

    @GetMapping("/me")
    public ResponseEntity<List<EnrollmentResponse>> getMyEnrollments(
            Authentication authentication) {

        boolean isStudent = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_STUDENT"));
        if (!isStudent) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Student student = studentService.getStudentForUsername(authentication.getName());
        List<EnrollmentResponse> response = enrollmentService
                .getEnrollmentsForStudent(student.getStudentId())
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<?> getAllEnrollments(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        Pageable pageable = PageParameters.optional(page, size, "enrollmentId");
        if (pageable != null) {
            return ResponseEntity.ok(PageResponse.from(
                    enrollmentService.getEnrollmentsPage(pageable), this::toResponse));
        }

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

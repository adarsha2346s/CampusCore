package com.campuscore.backend.controller;

import com.campuscore.backend.service.GpaCalculationService;
import com.campuscore.backend.entity.Enrollment;
import com.campuscore.backend.entity.Student;
import com.campuscore.backend.entity.User;
import com.campuscore.backend.repository.EnrollmentRepository;
import com.campuscore.backend.repository.StudentRepository;
import com.campuscore.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/gpa")
public class GpaController {

    private final GpaCalculationService gpaCalculationService;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public GpaController(
            GpaCalculationService gpaCalculationService,
            EnrollmentRepository enrollmentRepository,
            StudentRepository studentRepository,
            UserRepository userRepository) {
        this.gpaCalculationService = gpaCalculationService;
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/enrollment/{enrollmentId}")
    public ResponseEntity<GpaCalculationService.CourseGpaResult> getCourseGpa(
            @PathVariable Long enrollmentId,
            @RequestParam(defaultValue = "DEFAULT_10_POINT") String policyName,
            Authentication authentication) {

        boolean isStudent = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_STUDENT"));

        if (isStudent && !isOwnEnrollment(authentication, enrollmentId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(
                gpaCalculationService.calculateCourseGpa(
                        enrollmentId,
                        policyName
                )
        );
    }

    private boolean isOwnEnrollment(
            Authentication authentication,
            Long enrollmentId) {

        User user = userRepository
                .findByUsername(authentication.getName())
                .orElse(null);

        if (user == null) {
            return false;
        }

        Student student = studentRepository
                .findByUserUserId(user.getUserId())
                .orElse(null);

        if (student == null) {
            return false;
        }

        Enrollment enrollment = enrollmentRepository
                .findById(enrollmentId)
                .orElse(null);

        return enrollment != null
                && enrollment.getStudent().getStudentId()
                .equals(student.getStudentId());
    }
}

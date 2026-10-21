package com.campuscore.backend.controller;

import com.campuscore.backend.service.AccessScope;
import com.campuscore.backend.service.GpaCalculationService;
import com.campuscore.backend.entity.Enrollment;
import com.campuscore.backend.entity.Student;
import com.campuscore.backend.entity.User;
import com.campuscore.backend.repository.EnrollmentRepository;
import com.campuscore.backend.repository.StudentRepository;
import com.campuscore.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final AccessScope accessScope;

    public GpaController(
            GpaCalculationService gpaCalculationService,
            EnrollmentRepository enrollmentRepository,
            StudentRepository studentRepository,
            UserRepository userRepository) {
        this(gpaCalculationService, enrollmentRepository, studentRepository, userRepository, null);
    }

        @Autowired
    public GpaController(
            GpaCalculationService gpaCalculationService,
            EnrollmentRepository enrollmentRepository,
            StudentRepository studentRepository,
            UserRepository userRepository,
            AccessScope accessScope) {
        this.gpaCalculationService = gpaCalculationService;
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.accessScope = accessScope;
    }

    @GetMapping("/enrollment/{enrollmentId}")
    public ResponseEntity<GpaCalculationService.CourseGpaResult> getCourseGpa(
            @PathVariable Long enrollmentId,
            @RequestParam(defaultValue = "DEFAULT_10_POINT") String policyName) {

        // A student may only read their own enrollment GPA; faculty may read the
        // GPA of enrollments in courses assigned to them; administrators any.
        accessScope.requireEnrollmentAccess(enrollmentId);

        return ResponseEntity.ok(
                gpaCalculationService.calculateCourseGpa(
                        enrollmentId,
                        policyName
                )
        );
    }

}

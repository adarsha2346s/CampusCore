package com.campuscore.backend.controller;

import com.campuscore.backend.dto.StudentDashboardResponse;
import com.campuscore.backend.entity.Student;
import com.campuscore.backend.entity.User;
import com.campuscore.backend.repository.StudentRepository;
import com.campuscore.backend.repository.UserRepository;
import com.campuscore.backend.service.StudentDashboardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard/student")
public class StudentDashboardController {

    private final StudentDashboardService studentDashboardService;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    public StudentDashboardController(
            StudentDashboardService studentDashboardService,
            UserRepository userRepository,
            StudentRepository studentRepository) {

        this.studentDashboardService = studentDashboardService;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
    }

    @GetMapping("/{studentId}")
    public ResponseEntity<StudentDashboardResponse> getStudentDashboard(
            @PathVariable Long studentId,
            Authentication authentication) {

        /*
         * Get the currently authenticated username
         * from the JWT/SecurityContext.
         */
        String username = authentication.getName();

        /*
         * Only students should use this endpoint.
         */
        boolean isStudent = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_STUDENT"));

        if (!isStudent) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .build();
        }

        /*
         * Find the logged-in user's database record.
         */
        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("Authenticated user not found")
                );

        /*
         * Find the student record linked to this user.
         */
        Student student = studentRepository
                .findByUserUserId(user.getUserId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Student record not found for authenticated user"
                        )
                );

        /*
         * Prevent a student from requesting
         * another student's dashboard.
         */
        if (!student.getStudentId().equals(studentId)) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .build();
        }

        return ResponseEntity.ok(
                studentDashboardService.getStudentDashboard(studentId)
        );
    }
}
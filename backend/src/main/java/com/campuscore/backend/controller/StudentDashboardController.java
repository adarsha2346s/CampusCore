package com.campuscore.backend.controller;

import com.campuscore.backend.dto.StudentDashboardResponse;
import com.campuscore.backend.entity.Student;
import com.campuscore.backend.entity.User;
import com.campuscore.backend.repository.StudentRepository;
import com.campuscore.backend.repository.UserRepository;
import com.campuscore.backend.service.AccessScope;
import com.campuscore.backend.service.StudentDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final AccessScope accessScope;

    public StudentDashboardController(
            StudentDashboardService studentDashboardService,
            UserRepository userRepository,
            StudentRepository studentRepository) {

        this(studentDashboardService, userRepository, studentRepository, null);
    }

        @Autowired
    public StudentDashboardController(
            StudentDashboardService studentDashboardService,
            UserRepository userRepository,
            StudentRepository studentRepository,
            AccessScope accessScope) {

        this.studentDashboardService = studentDashboardService;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.accessScope = accessScope;
    }

    @GetMapping("/{studentId}")
    public ResponseEntity<StudentDashboardResponse> getStudentDashboard(
            @PathVariable Long studentId) {

        // ADMIN, FACULTY and STUDENT may reach the dashboard route, so the role is
        // enforced by the request matcher and the ownership by AccessScope: an
        // administrator sees any student, faculty only students they teach, and a
        // student only their own dashboard.
        accessScope.requireStudentAccess(studentId);

        return ResponseEntity.ok(
                studentDashboardService.getStudentDashboard(studentId)
        );
    }
}
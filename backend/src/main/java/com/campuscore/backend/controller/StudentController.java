package com.campuscore.backend.controller;

import com.campuscore.backend.dto.StudentResponse;
import com.campuscore.backend.entity.Student;
import com.campuscore.backend.entity.User;
import com.campuscore.backend.repository.StudentRepository;
import com.campuscore.backend.repository.UserRepository;
import com.campuscore.backend.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentService studentService;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public StudentController(
            StudentService studentService,
            StudentRepository studentRepository,
            UserRepository userRepository) {
        this.studentService = studentService;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<StudentResponse>> getAllStudents() {

        List<StudentResponse> students =
                studentService.getAllStudents()
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(students);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> getStudentById(
            @PathVariable Long id,
            Authentication authentication) {

        Student student = studentService.getStudentById(id);

        boolean isStudent = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_STUDENT"));

        if (isStudent && !isOwnStudent(authentication, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(toResponse(student));
    }

    private boolean isOwnStudent(
            Authentication authentication,
            Long studentId) {

        User user = userRepository
                .findByUsername(authentication.getName())
                .orElse(null);

        if (user == null) {
            return false;
        }

        return studentRepository
                .findByUserUserId(user.getUserId())
                .map(student -> student.getStudentId().equals(studentId))
                .orElse(false);
    }

    @PostMapping
    public ResponseEntity<?> createStudent(
            @Valid @RequestBody com.campuscore.backend.dto.StudentRequest request,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {

            Map<String, String> errors = new HashMap<>();

            bindingResult.getFieldErrors().forEach(error ->
                    errors.put(
                            error.getField(),
                            error.getDefaultMessage()
                    )
            );

            return ResponseEntity
                    .badRequest()
                    .body(errors);
        }

        Student savedStudent =
                studentService.createStudent(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(savedStudent));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody com.campuscore.backend.dto.StudentRequest request,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {

            Map<String, String> errors = new HashMap<>();

            bindingResult.getFieldErrors().forEach(error ->
                    errors.put(
                            error.getField(),
                            error.getDefaultMessage()
                    )
            );

            return ResponseEntity
                    .badRequest()
                    .body(errors);
        }

        Student updatedStudent =
                studentService.updateStudent(id, request);

        return ResponseEntity.ok(toResponse(updatedStudent));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateStudent(
            @PathVariable Long id) {

        studentService.deactivateStudent(id);

        return ResponseEntity.noContent().build();
    }

    private StudentResponse toResponse(Student student) {

        return new StudentResponse(
                student.getStudentId(),
                student.getUser().getUserId(),
                student.getDepartment().getDepartmentId(),
                student.getEnrollmentNumber(),
                student.getFirstName(),
                student.getLastName(),
                student.getDateOfBirth() != null
                        ? student.getDateOfBirth().toString()
                        : null,
                student.getPhone(),
                student.getAdmissionYear(),
                student.getStatus().name()
        );
    }
}

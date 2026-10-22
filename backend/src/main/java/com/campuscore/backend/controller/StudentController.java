package com.campuscore.backend.controller;

import com.campuscore.backend.dto.StudentResponse;
import com.campuscore.backend.dto.EnrollmentResponse;
import com.campuscore.backend.dto.PageResponse;
import com.campuscore.backend.lib.PageParameters;
import com.campuscore.backend.dto.StudentSelfResponse;
import com.campuscore.backend.entity.Enrollment;
import com.campuscore.backend.entity.Student;
import com.campuscore.backend.entity.User;
import com.campuscore.backend.repository.StudentRepository;
import com.campuscore.backend.repository.UserRepository;
import com.campuscore.backend.service.AccessScope;
import com.campuscore.backend.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
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
    private final AccessScope accessScope;

    public StudentController(
            StudentService studentService,
            StudentRepository studentRepository,
            UserRepository userRepository) {
        this(studentService, studentRepository, userRepository, null);
    }

        @Autowired
    public StudentController(
            StudentService studentService,
            StudentRepository studentRepository,
            UserRepository userRepository,
            AccessScope accessScope) {
        this.studentService = studentService;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.accessScope = accessScope;
    }

    @GetMapping("/me")
    public ResponseEntity<StudentSelfResponse> getMyStudentProfile(
            Authentication authentication) {

        if (!hasRole(authentication, "ROLE_STUDENT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Student student = studentService.getStudentForUsername(authentication.getName());
        StudentSelfResponse response = new StudentSelfResponse(
                student.getStudentId(),
                student.getEnrollmentNumber(),
                student.getFirstName(),
                student.getLastName(),
                student.getDepartment().getName(),
                student.getAdmissionYear(),
                student.getStatus().name()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<?> getAllStudents(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        Pageable pageable = PageParameters.optional(page, size, "studentId");
        if (pageable != null) {
            // The paged listing is scoped exactly like the unpaged one below:
            // administrators see the whole directory and a faculty member sees
            // only the students they teach.
            return ResponseEntity.ok(PageResponse.from(
                    studentService.getStudentsPage(pageable), this::toResponse));
        }

        // Administrators see the whole directory. Faculty only see the students
        // they actually teach, so the directory cannot be used to browse every
        // student in the institution.
        List<Student> visible = accessScope.isAdmin()
                ? studentService.getAllStudents()
                : studentService.getStudentsForAssignedCourses();

        List<StudentResponse> students =
                visible.stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(students);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> getStudentById(
            @PathVariable Long id) {

        accessScope.requireStudentAccess(id);

        Student student = studentService.getStudentById(id);

        return ResponseEntity.ok(toResponse(student));
    }

    @GetMapping("/{id}/dependencies")
    public ResponseEntity<com.campuscore.backend.dto.ResourceDependencySummary> getStudentDependencies(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                studentService.getStudentDependencies(id)
        );
    }

    /**
     * A student's enrollments, scoped to the caller.
     *
     * <p>Ownership is checked here, exactly as it is for the student record
     * itself: an administrator may read any student's enrollments, and a faculty
     * member only a student they actually teach. The service then narrows the
     * result to the courses the caller teaches, so the response never carries
     * the student's courses that are outside the caller's teaching scope.
     */
    @GetMapping("/{id}/enrollments")
    public ResponseEntity<List<EnrollmentResponse>> getStudentEnrollments(
            @PathVariable Long id) {

        accessScope.requireStudentAccess(id);

        List<EnrollmentResponse> enrollments =
                studentService.getEnrollmentsForStudent(id)
                        .stream()
                        .map(EnrollmentResponse::from)
                        .toList();

        return ResponseEntity.ok(enrollments);
    }


    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(role));
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

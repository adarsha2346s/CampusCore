package com.campuscore.backend.controller;

import com.campuscore.backend.dto.FacultyAssignmentRequest;
import com.campuscore.backend.dto.FacultyAssignmentResponse;
import com.campuscore.backend.service.FacultyCourseAssignmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Faculty-to-course assignment endpoints.
 *
 * <p>Reads follow the same rule as the other course reads (any authenticated
 * user); writes are restricted to ADMIN by SecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/courses/{courseId}/faculty-assignments")
public class FacultyAssignmentController {

    private final FacultyCourseAssignmentService assignmentService;

    public FacultyAssignmentController(FacultyCourseAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    // GET current assignments
    @GetMapping
    public ResponseEntity<List<FacultyAssignmentResponse>> getActiveAssignments(
            @PathVariable Long courseId) {

        return ResponseEntity.ok(assignmentService.getActiveAssignments(courseId));
    }

    // GET assignment history, including closed assignments
    @GetMapping("/history")
    public ResponseEntity<List<FacultyAssignmentResponse>> getAssignmentHistory(
            @PathVariable Long courseId) {

        return ResponseEntity.ok(assignmentService.getAssignmentHistory(courseId));
    }

    // ASSIGN a faculty member to a course
    @PostMapping
    public ResponseEntity<?> assignFaculty(
            @PathVariable Long courseId,
            @Valid @RequestBody FacultyAssignmentRequest request,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return ResponseEntity
                    .badRequest()
                    .body(fieldErrors(bindingResult));
        }

        FacultyAssignmentResponse assigned =
                assignmentService.assignFaculty(courseId, request.getFacultyId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(assigned);
    }

    // REASSIGN the course to a different faculty member
    @PutMapping
    public ResponseEntity<?> reassignFaculty(
            @PathVariable Long courseId,
            @Valid @RequestBody FacultyAssignmentRequest request,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return ResponseEntity
                    .badRequest()
                    .body(fieldErrors(bindingResult));
        }

        return ResponseEntity.ok(
                assignmentService.reassignFaculty(courseId, request.getFacultyId()));
    }

    // REMOVE an assignment
    @DeleteMapping("/{facultyId}")
    public ResponseEntity<Void> removeAssignment(
            @PathVariable Long courseId,
            @PathVariable Long facultyId) {

        assignmentService.removeAssignment(courseId, facultyId);

        return ResponseEntity.noContent().build();
    }

    private Map<String, String> fieldErrors(BindingResult bindingResult) {
        Map<String, String> errors = new HashMap<>();

        bindingResult.getFieldErrors().forEach(error ->
                errors.put(
                        error.getField(),
                        error.getDefaultMessage()
                )
        );

        return errors;
    }
}

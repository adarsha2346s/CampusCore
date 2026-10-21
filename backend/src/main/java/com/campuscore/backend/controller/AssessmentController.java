package com.campuscore.backend.controller;

import com.campuscore.backend.dto.AssessmentRequest;
import com.campuscore.backend.dto.AssessmentResponse;
import com.campuscore.backend.dto.AssessmentSelfResponse;
import com.campuscore.backend.dto.PageResponse;
import com.campuscore.backend.lib.PageParameters;
import com.campuscore.backend.entity.Assessment;
import com.campuscore.backend.entity.Student;
import com.campuscore.backend.service.AssessmentService;
import com.campuscore.backend.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/assessments")
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final StudentService studentService;

    public AssessmentController(AssessmentService assessmentService) {
        this(assessmentService, null);
    }

    @Autowired
    public AssessmentController(
            AssessmentService assessmentService,
            StudentService studentService) {
        this.assessmentService = assessmentService;
        this.studentService = studentService;
    }

    // =========================
    // GET ALL ASSESSMENTS
    // =========================
    @GetMapping
    public ResponseEntity<?> getAllAssessments(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        Pageable pageable = PageParameters.optional(page, size, "assessmentId");
        if (pageable != null) {
            return ResponseEntity.ok(PageResponse.from(
                    assessmentService.getAssessmentsPage(pageable), this::toResponse));
        }

        List<AssessmentResponse> response =
                assessmentService.getAllAssessments()
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    // =========================
    // GET ASSESSMENT BY ID
    // =========================
    @GetMapping("/{id}")
    public ResponseEntity<AssessmentResponse> getAssessmentById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                toResponse(
                        assessmentService.getAssessmentById(id)
                )
        );
    }

    // =========================
    // GET ASSESSMENTS BY COURSE
    // =========================
    /**
     * Assessment metadata for the calling student's own marks.
     *
     * <p>The student is resolved from the verified session, so the route takes no
     * identifier and there is nothing for a caller to tamper with. The general
     * assessment routes below stay administrator/faculty only.
     */
    @GetMapping("/me")
    public ResponseEntity<List<AssessmentSelfResponse>> getMyAssessments(
            Authentication authentication) {

        boolean isStudent = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_STUDENT"));
        if (!isStudent) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Student student = studentService.getStudentForUsername(authentication.getName());

        List<AssessmentSelfResponse> response =
                assessmentService.getAssessmentsForStudent(student.getStudentId())
                        .stream()
                        .map(assessment -> new AssessmentSelfResponse(
                                assessment.getAssessmentId(),
                                assessment.getName(),
                                assessment.getAssessmentType().name(),
                                assessment.getMaxMarks().toPlainString()))
                        .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<?> getAssessmentsByCourse(
            @PathVariable Long courseId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        Pageable pageable = PageParameters.optional(page, size, "assessmentId");
        if (pageable != null) {
            return ResponseEntity.ok(PageResponse.from(
                    assessmentService.getAssessmentsByCoursePage(courseId, pageable), this::toResponse));
        }

        List<AssessmentResponse> response =
                assessmentService.getAssessmentsByCourse(courseId)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    // =========================
    // CREATE ASSESSMENT
    // =========================
    @PostMapping
    public ResponseEntity<AssessmentResponse> createAssessment(
            @RequestParam Long courseId,
            @RequestParam String name,
            @RequestParam Assessment.AssessmentType assessmentType,
            @RequestParam BigDecimal maxMarks,
            @RequestParam BigDecimal weight) {

        Assessment assessment =
                assessmentService.createAssessment(
                        courseId,
                        name,
                        assessmentType,
                        maxMarks,
                        weight
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(assessment));
    }

    // =========================
    // UPDATE ASSESSMENT
    // =========================

    /**
     * Edit an assessment's details.
     *
     * <p>Maximum marks is rejected once marks reference the assessment, because
     * rescaling it would reinterpret every grade already recorded against it.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAssessment(
            @PathVariable Long id,
            @Valid @RequestBody AssessmentRequest request,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message", "Validation failed",
                            "errors", bindingResult.getFieldErrors().stream()
                                    .collect(Collectors.toMap(
                                            FieldError::getField,
                                            FieldError::getDefaultMessage,
                                            (first, second) -> first))));
        }

        Assessment assessment = assessmentService.updateAssessment(id, request);

        return ResponseEntity.ok(toResponse(assessment));
    }

    // =========================
    // DELETE ASSESSMENT
    // =========================

    /**
     * Delete an assessment that no marks reference.
     *
     * <p>Returns 409 while marks exist, since an assessment is the parent record
     * those marks hang from.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAssessment(@PathVariable Long id) {
        assessmentService.deleteAssessment(id);
        return ResponseEntity.noContent().build();
    }

    // =========================
    // ENTITY → RESPONSE DTO
    // =========================
    private AssessmentResponse toResponse(Assessment assessment) {

        String assessmentDate = null;

        if (assessment.getAssessmentDate() != null) {
            assessmentDate =
                    assessment.getAssessmentDate().toString();
        }

        return new AssessmentResponse(
                assessment.getAssessmentId(),
                assessment.getCourse().getCourseId(),
                assessment.getName(),
                assessment.getAssessmentType().name(),
                assessment.getMaxMarks(),
                assessment.getWeight(),
                assessmentDate
        );
    }
}

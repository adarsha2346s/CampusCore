package com.campuscore.backend.controller;

import com.campuscore.backend.dto.AssessmentResponse;
import com.campuscore.backend.dto.PageResponse;
import com.campuscore.backend.lib.PageParameters;
import com.campuscore.backend.entity.Assessment;
import com.campuscore.backend.service.AssessmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/assessments")
public class AssessmentController {

    private final AssessmentService assessmentService;

    public AssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
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

package com.campuscore.backend.controller;

import com.campuscore.backend.dto.MarkResponse;
import com.campuscore.backend.entity.Mark;
import com.campuscore.backend.service.MarkService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/marks")
public class MarkController {

    private final MarkService markService;

    public MarkController(MarkService markService) {
        this.markService = markService;
    }

    @GetMapping
    public ResponseEntity<List<MarkResponse>> getAllMarks() {

        List<MarkResponse> response = markService.getAllMarks()
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MarkResponse> getMarkById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                toResponse(markService.getMarkById(id))
        );
    }

    @GetMapping("/enrollment/{enrollmentId}")
    public ResponseEntity<List<MarkResponse>> getMarksByEnrollment(
            @PathVariable Long enrollmentId) {

        List<MarkResponse> response =
                markService.getMarksByEnrollment(enrollmentId)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/assessment/{assessmentId}")
    public ResponseEntity<List<MarkResponse>> getMarksByAssessment(
            @PathVariable Long assessmentId) {

        List<MarkResponse> response =
                markService.getMarksByAssessment(assessmentId)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<MarkResponse> createMark(
            @RequestParam Long enrollmentId,
            @RequestParam Long assessmentId,
            @RequestParam BigDecimal marksObtained) {

        Mark mark = markService.createMark(
                enrollmentId,
                assessmentId,
                marksObtained
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(mark));
    }

    private MarkResponse toResponse(Mark mark) {

        String enteredAt = null;

        if (mark.getEnteredAt() != null) {
            enteredAt = mark.getEnteredAt().toString();
        }

        return new MarkResponse(
                mark.getMarkId(),
                mark.getEnrollment().getEnrollmentId(),
                mark.getAssessment().getAssessmentId(),
                mark.getMarksObtained().toString(),
                enteredAt
        );
    }
}
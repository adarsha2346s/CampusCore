package com.campuscore.backend.controller;

import com.campuscore.backend.dto.MarkRequest;
import com.campuscore.backend.dto.MarkResponse;
import com.campuscore.backend.dto.StudentMarkResponse;
import com.campuscore.backend.dto.PageResponse;
import com.campuscore.backend.lib.PageParameters;
import com.campuscore.backend.entity.Mark;
import com.campuscore.backend.entity.Student;
import org.springframework.security.core.Authentication;
import com.campuscore.backend.service.MarkService;
import com.campuscore.backend.service.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;
import java.util.stream.Collectors;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/marks")
public class MarkController {

    private final MarkService markService;
    private final StudentService studentService;

    public MarkController(MarkService markService) {
        this(markService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public MarkController(MarkService markService, StudentService studentService) {
        this.markService = markService;
        this.studentService = studentService;
    }

    /**
     * The signed-in student's own marks, with each row's course and assessment
     * already joined in.
     *
     * <p>This route replaces the workspace's previous fan-out of one request per
     * enrollment plus separate lookups for course and assessment names. Taking no
     * identifier, ownership is resolved from the verified session rather than the
     * request, so a student cannot ask for anyone else's marks.
     */
    @GetMapping("/me")
    public ResponseEntity<List<StudentMarkResponse>> getMyMarks(
            Authentication authentication) {

        if (studentService == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Student student = studentService.getStudentForUsername(authentication.getName());

        List<StudentMarkResponse> response =
                markService.getMarksForStudent(student.getStudentId())
                        .stream()
                        .map(mark -> new StudentMarkResponse(
                                mark.getMarkId(),
                                mark.getEnrollment().getCourse().getCourseCode(),
                                mark.getEnrollment().getCourse().getCourseName(),
                                mark.getEnrollment().getSemester(),
                                mark.getEnrollment().getAcademicYear(),
                                mark.getAssessment().getName(),
                                mark.getAssessment().getAssessmentType().name(),
                                mark.getMarksObtained().toPlainString(),
                                mark.getAssessment().getMaxMarks().toPlainString(),
                                mark.getAssessment().getWeight().toPlainString(),
                                mark.getEnteredAt() == null ? null : mark.getEnteredAt().toString()))
                        .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<?> getAllMarks(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        Pageable pageable = PageParameters.optional(page, size, "markId");
        if (pageable != null) {
            return ResponseEntity.ok(PageResponse.from(markService.getMarksPage(pageable), this::toResponse));
        }

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
    public ResponseEntity<?> getMarksByEnrollment(
            @PathVariable Long enrollmentId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        Pageable pageable = PageParameters.optional(page, size, "markId");
        if (pageable != null) {
            return ResponseEntity.ok(PageResponse.from(
                    markService.getMarksByEnrollmentPage(enrollmentId, pageable), this::toResponse));
        }

        List<MarkResponse> response =
                markService.getMarksByEnrollment(enrollmentId)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/assessment/{assessmentId}")
    public ResponseEntity<?> getMarksByAssessment(
            @PathVariable Long assessmentId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        Pageable pageable = PageParameters.optional(page, size, "markId");
        if (pageable != null) {
            return ResponseEntity.ok(PageResponse.from(
                    markService.getMarksByAssessmentPage(assessmentId, pageable), this::toResponse));
        }

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

    // =========================
    // UPDATE MARK
    // =========================

    /**
     * Correct the value awarded on a recorded mark.
     *
     * <p>The enrollment and assessment are not editable here, and the value is
     * rejected when it would exceed the assessment's maximum.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateMark(
            @PathVariable Long id,
            @Valid @RequestBody MarkRequest request,
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

        Mark mark = markService.updateMark(id, request);

        return ResponseEntity.ok(toResponse(mark));
    }

    // =========================
    // DELETE MARK
    // =========================

    /**
     * Remove a recorded mark.
     *
     * <p>Safe with respect to referential integrity: a mark is a leaf record and
     * nothing references it. Reported as 404 when the mark does not exist.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMark(@PathVariable Long id) {
        markService.deleteMark(id);
        return ResponseEntity.noContent().build();
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

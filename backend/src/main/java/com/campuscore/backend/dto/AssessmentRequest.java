package com.campuscore.backend.dto;

import com.campuscore.backend.entity.Assessment;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload for changing an existing assessment.
 *
 * <p>Maximum marks is only editable while no marks reference the assessment,
 * because rescaling it would silently reinterpret every grade already recorded
 * against it. The course is deliberately not editable: an assessment belongs to
 * its course offering, and moving it would strand those marks.
 */
public class AssessmentRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    @NotNull(message = "Assessment type is required")
    private Assessment.AssessmentType assessmentType;

    @NotNull(message = "Maximum marks is required")
    @DecimalMin(value = "0.01", message = "Maximum marks must be greater than zero")
    @Digits(integer = 4, fraction = 2, message = "Maximum marks allows up to two decimals")
    private BigDecimal maxMarks;

    @NotNull(message = "Weight is required")
    @DecimalMin(value = "0.00", message = "Weight cannot be negative")
    @DecimalMax(value = "100.00", message = "Weight cannot exceed 100")
    @Digits(integer = 3, fraction = 2, message = "Weight allows up to two decimals")
    private BigDecimal weight;

    private LocalDate assessmentDate;

    public AssessmentRequest() {
    }

    public AssessmentRequest(
            String name,
            Assessment.AssessmentType assessmentType,
            BigDecimal maxMarks,
            BigDecimal weight,
            LocalDate assessmentDate) {

        this.name = name;
        this.assessmentType = assessmentType;
        this.maxMarks = maxMarks;
        this.weight = weight;
        this.assessmentDate = assessmentDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Assessment.AssessmentType getAssessmentType() {
        return assessmentType;
    }

    public void setAssessmentType(Assessment.AssessmentType assessmentType) {
        this.assessmentType = assessmentType;
    }

    public BigDecimal getMaxMarks() {
        return maxMarks;
    }

    public void setMaxMarks(BigDecimal maxMarks) {
        this.maxMarks = maxMarks;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public LocalDate getAssessmentDate() {
        return assessmentDate;
    }

    public void setAssessmentDate(LocalDate assessmentDate) {
        this.assessmentDate = assessmentDate;
    }
}

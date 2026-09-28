package com.campuscore.backend.dto;

import java.math.BigDecimal;

public class AssessmentResponse {

    private Long assessmentId;
    private Long courseId;
    private String name;
    private String assessmentType;
    private BigDecimal maxMarks;
    private BigDecimal weight;
    private String assessmentDate;

    public AssessmentResponse() {
    }

    public AssessmentResponse(
            Long assessmentId,
            Long courseId,
            String name,
            String assessmentType,
            BigDecimal maxMarks,
            BigDecimal weight,
            String assessmentDate) {

        this.assessmentId = assessmentId;
        this.courseId = courseId;
        this.name = name;
        this.assessmentType = assessmentType;
        this.maxMarks = maxMarks;
        this.weight = weight;
        this.assessmentDate = assessmentDate;
    }

    public Long getAssessmentId() {
        return assessmentId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public String getName() {
        return name;
    }

    public String getAssessmentType() {
        return assessmentType;
    }

    public BigDecimal getMaxMarks() {
        return maxMarks;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public String getAssessmentDate() {
        return assessmentDate;
    }
}
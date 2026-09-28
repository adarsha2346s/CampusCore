package com.campuscore.backend.dto;

public class MarkResponse {

    private Long markId;
    private Long enrollmentId;
    private Long assessmentId;
    private String marksObtained;
    private String enteredAt;

    public MarkResponse() {
    }

    public MarkResponse(
            Long markId,
            Long enrollmentId,
            Long assessmentId,
            String marksObtained,
            String enteredAt) {

        this.markId = markId;
        this.enrollmentId = enrollmentId;
        this.assessmentId = assessmentId;
        this.marksObtained = marksObtained;
        this.enteredAt = enteredAt;
    }

    public Long getMarkId() {
        return markId;
    }

    public Long getEnrollmentId() {
        return enrollmentId;
    }

    public Long getAssessmentId() {
        return assessmentId;
    }

    public String getMarksObtained() {
        return marksObtained;
    }

    public String getEnteredAt() {
        return enteredAt;
    }
}
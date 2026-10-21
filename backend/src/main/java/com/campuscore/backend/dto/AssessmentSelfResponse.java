package com.campuscore.backend.dto;

/**
 * Assessment metadata a student is allowed to see.
 *
 * <p>Deliberately narrower than {@code AssessmentResponse}: it omits course and
 * administrative fields and carries only what a student needs to interpret their
 * own mark. The general assessment routes stay administrator/faculty only.
 */
public class AssessmentSelfResponse {

    private Long assessmentId;
    private String name;
    private String assessmentType;
    private String maxMarks;

    public AssessmentSelfResponse() {
    }

    public AssessmentSelfResponse(
            Long assessmentId,
            String name,
            String assessmentType,
            String maxMarks) {

        this.assessmentId = assessmentId;
        this.name = name;
        this.assessmentType = assessmentType;
        this.maxMarks = maxMarks;
    }

    public Long getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(Long assessmentId) {
        this.assessmentId = assessmentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAssessmentType() {
        return assessmentType;
    }

    public void setAssessmentType(String assessmentType) {
        this.assessmentType = assessmentType;
    }

    public String getMaxMarks() {
        return maxMarks;
    }

    public void setMaxMarks(String maxMarks) {
        this.maxMarks = maxMarks;
    }
}

package com.campuscore.backend.dto;

/**
 * One of the signed-in student's own marks, with the course and assessment it
 * belongs to already joined in.
 *
 * <p>The workspace previously assembled this by calling the marks of every one of
 * the student's enrollments and then looking the course and assessment names up
 * from separate listings, which is one request per enrollment and silently showed
 * a bare identifier whenever the lookup listing was paged past. Returning the
 * display fields here makes the workspace a single request.
 *
 * <p>Ownership is decided by the caller: the id comes from the verified session,
 * never from the request.
 */
public class StudentMarkResponse {

    private Long markId;
    private String courseCode;
    private String courseName;
    private String semester;
    private String academicYear;
    private String assessmentName;
    private String assessmentType;
    private String marksObtained;
    private String maxMarks;
    private String weight;
    private String enteredAt;

    public StudentMarkResponse() {
    }

    public StudentMarkResponse(
            Long markId,
            String courseCode,
            String courseName,
            String semester,
            String academicYear,
            String assessmentName,
            String assessmentType,
            String marksObtained,
            String maxMarks,
            String weight,
            String enteredAt) {

        this.markId = markId;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.semester = semester;
        this.academicYear = academicYear;
        this.assessmentName = assessmentName;
        this.assessmentType = assessmentType;
        this.marksObtained = marksObtained;
        this.maxMarks = maxMarks;
        this.weight = weight;
        this.enteredAt = enteredAt;
    }

    public Long getMarkId() {
        return markId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getSemester() {
        return semester;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public String getAssessmentName() {
        return assessmentName;
    }

    public String getAssessmentType() {
        return assessmentType;
    }

    public String getMarksObtained() {
        return marksObtained;
    }

    public String getMaxMarks() {
        return maxMarks;
    }

    public String getWeight() {
        return weight;
    }

    public String getEnteredAt() {
        return enteredAt;
    }
}

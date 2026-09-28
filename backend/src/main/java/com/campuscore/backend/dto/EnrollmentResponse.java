package com.campuscore.backend.dto;

public class EnrollmentResponse {

    private Long enrollmentId;
    private Long studentId;
    private Long courseId;
    private String semester;
    private String academicYear;
    private String enrollmentDate;
    private String status;

    public EnrollmentResponse() {
    }

    public EnrollmentResponse(
            Long enrollmentId,
            Long studentId,
            Long courseId,
            String semester,
            String academicYear,
            String enrollmentDate,
            String status) {

        this.enrollmentId = enrollmentId;
        this.studentId = studentId;
        this.courseId = courseId;
        this.semester = semester;
        this.academicYear = academicYear;
        this.enrollmentDate = enrollmentDate;
        this.status = status;
    }

    public Long getEnrollmentId() {
        return enrollmentId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public String getSemester() {
        return semester;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public String getEnrollmentDate() {
        return enrollmentDate;
    }

    public String getStatus() {
        return status;
    }
}

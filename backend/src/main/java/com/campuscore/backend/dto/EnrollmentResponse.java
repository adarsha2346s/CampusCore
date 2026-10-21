package com.campuscore.backend.dto;

public class EnrollmentResponse {

    private Long enrollmentId;
    private Long studentId;
    private Long courseId;
    private String semester;
    private String academicYear;
    private String enrollmentDate;
    private String status;

    /**
     * Identity of the enrolled student.
     *
     * <p>Included so a faculty member reading the enrollments of a course they
     * teach sees who is enrolled rather than a bare identifier. Every route that
     * returns this DTO is either administrator-only or already scoped to the
     * caller's own records, so this adds no disclosure.
     */
    private String enrollmentNumber;
    private String studentFirstName;
    private String studentLastName;

    public EnrollmentResponse() {
    }

    public EnrollmentResponse(
            Long enrollmentId,
            Long studentId,
            Long courseId,
            String semester,
            String academicYear,
            String enrollmentDate,
            String status,
            String enrollmentNumber,
            String studentFirstName,
            String studentLastName) {

        this.enrollmentId = enrollmentId;
        this.studentId = studentId;
        this.courseId = courseId;
        this.semester = semester;
        this.academicYear = academicYear;
        this.enrollmentDate = enrollmentDate;
        this.status = status;
        this.enrollmentNumber = enrollmentNumber;
        this.studentFirstName = studentFirstName;
        this.studentLastName = studentLastName;
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

    public String getEnrollmentNumber() {
        return enrollmentNumber;
    }

    public String getStudentFirstName() {
        return studentFirstName;
    }

    public String getStudentLastName() {
        return studentLastName;
    }
}

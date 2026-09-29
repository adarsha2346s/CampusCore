package com.campuscore.backend.dto;

public class StudentSelfResponse {

    private Long studentId;
    private String enrollmentNumber;
    private String firstName;
    private String lastName;
    private String departmentName;
    private Integer admissionYear;
    private String status;

    public StudentSelfResponse() {
    }

    public StudentSelfResponse(
            Long studentId,
            String enrollmentNumber,
            String firstName,
            String lastName,
            String departmentName,
            Integer admissionYear,
            String status) {
        this.studentId = studentId;
        this.enrollmentNumber = enrollmentNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.departmentName = departmentName;
        this.admissionYear = admissionYear;
        this.status = status;
    }

    public Long getStudentId() { return studentId; }
    public String getEnrollmentNumber() { return enrollmentNumber; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getDepartmentName() { return departmentName; }
    public Integer getAdmissionYear() { return admissionYear; }
    public String getStatus() { return status; }
}

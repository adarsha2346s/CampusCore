package com.campuscore.backend.dto;

public class StudentResponse {

    private Long studentId;
    private Long userId;
    private Long departmentId;
    private String enrollmentNumber;
    private String firstName;
    private String lastName;
    private String dateOfBirth;
    private String phone;
    private Integer admissionYear;
    private String status;

    public StudentResponse() {
    }

    public StudentResponse(
            Long studentId,
            Long userId,
            Long departmentId,
            String enrollmentNumber,
            String firstName,
            String lastName,
            String dateOfBirth,
            String phone,
            Integer admissionYear,
            String status) {

        this.studentId = studentId;
        this.userId = userId;
        this.departmentId = departmentId;
        this.enrollmentNumber = enrollmentNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.phone = phone;
        this.admissionYear = admissionYear;
        this.status = status;
    }

    public Long getStudentId() {
        return studentId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public String getEnrollmentNumber() {
        return enrollmentNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getPhone() {
        return phone;
    }

    public Integer getAdmissionYear() {
        return admissionYear;
    }

    public String getStatus() {
        return status;
    }
}
package com.campuscore.backend.dto;

public class FacultyResponse {

    private Long facultyId;
    private Long userId;
    private Long departmentId;
    private String employeeNumber;
    private String firstName;
    private String lastName;
    private String phone;
    private String status;

    public FacultyResponse() {
    }

    public FacultyResponse(
            Long facultyId,
            Long userId,
            Long departmentId,
            String employeeNumber,
            String firstName,
            String lastName,
            String phone,
            String status) {

        this.facultyId = facultyId;
        this.userId = userId;
        this.departmentId = departmentId;
        this.employeeNumber = employeeNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.status = status;
    }

    public Long getFacultyId() {
        return facultyId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhone() {
        return phone;
    }

    public String getStatus() {
        return status;
    }
}
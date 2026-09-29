package com.campuscore.backend.dto;

public class FacultySelfResponse {

    private String employeeNumber;
    private String firstName;
    private String lastName;
    private String departmentName;
    private String status;

    public FacultySelfResponse() {
    }

    public FacultySelfResponse(
            String employeeNumber,
            String firstName,
            String lastName,
            String departmentName,
            String status) {
        this.employeeNumber = employeeNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.departmentName = departmentName;
        this.status = status;
    }

    public String getEmployeeNumber() { return employeeNumber; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getDepartmentName() { return departmentName; }
    public String getStatus() { return status; }
}

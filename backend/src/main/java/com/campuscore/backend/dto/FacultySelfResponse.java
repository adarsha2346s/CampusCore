package com.campuscore.backend.dto;

public class FacultySelfResponse {

    /**
     * The caller's own faculty identifier.
     *
     * <p>Included so a faculty member can address their own scope-enforced
     * routes (for example GET /api/v1/faculty/{id}/courses) without guessing an
     * identifier. It reveals nothing beyond what the caller already owns, and it
     * grants no additional permission: those routes still reject any other
     * faculty member's id.
     */
    private Long facultyId;

    private String employeeNumber;
    private String firstName;
    private String lastName;
    private String departmentName;
    private String status;

    public FacultySelfResponse() {
    }

    public FacultySelfResponse(
            Long facultyId,
            String employeeNumber,
            String firstName,
            String lastName,
            String departmentName,
            String status) {
        this.facultyId = facultyId;
        this.employeeNumber = employeeNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.departmentName = departmentName;
        this.status = status;
    }

    public Long getFacultyId() { return facultyId; }

    public void setFacultyId(Long facultyId) { this.facultyId = facultyId; }

    public String getEmployeeNumber() { return employeeNumber; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getDepartmentName() { return departmentName; }
    public String getStatus() { return status; }
}

package com.campuscore.backend.dto;

import jakarta.validation.constraints.NotNull;

public class FacultyAssignmentRequest {

    @NotNull(message = "Faculty ID is required")
    private Long facultyId;

    public FacultyAssignmentRequest() {
    }

    public Long getFacultyId() {
        return facultyId;
    }

    public void setFacultyId(Long facultyId) {
        this.facultyId = facultyId;
    }
}

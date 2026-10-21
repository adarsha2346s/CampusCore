package com.campuscore.backend.dto;

import com.campuscore.backend.entity.Enrollment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload for changing an existing enrollment.
 *
 * <p>Semester and academic year are editable only while the enrollment carries no
 * academic records; the service enforces that, because silently moving graded work
 * to a different term would corrupt reporting. Status is always editable and is
 * the intended way to archive an enrollment.
 */
public class EnrollmentRequest {

    @NotBlank(message = "Semester is required")
    @Size(max = 30, message = "Semester cannot exceed 30 characters")
    private String semester;

    @NotBlank(message = "Academic year is required")
    @Size(max = 20, message = "Academic year cannot exceed 20 characters")
    private String academicYear;

    @NotNull(message = "Status is required")
    private Enrollment.Status status;

    public EnrollmentRequest() {
    }

    public EnrollmentRequest(String semester, String academicYear, Enrollment.Status status) {
        this.semester = semester;
        this.academicYear = academicYear;
        this.status = status;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public Enrollment.Status getStatus() {
        return status;
    }

    public void setStatus(Enrollment.Status status) {
        this.status = status;
    }
}

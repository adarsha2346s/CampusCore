package com.campuscore.backend.dto;

import java.time.LocalDateTime;

public class FacultyAssignmentResponse {

    private Long assignmentId;
    private Long courseId;
    private String courseCode;
    private String courseName;
    private Long facultyId;
    private String employeeNumber;
    private String facultyName;
    private LocalDateTime assignedAt;
    private LocalDateTime endedAt;
    private Boolean active;

    public FacultyAssignmentResponse() {
    }

    public FacultyAssignmentResponse(
            Long assignmentId,
            Long courseId,
            String courseCode,
            String courseName,
            Long facultyId,
            String employeeNumber,
            String facultyName,
            LocalDateTime assignedAt,
            LocalDateTime endedAt,
            Boolean active) {

        this.assignmentId = assignmentId;
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.facultyId = facultyId;
        this.employeeNumber = employeeNumber;
        this.facultyName = facultyName;
        this.assignedAt = assignedAt;
        this.endedAt = endedAt;
        this.active = active;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public Long getFacultyId() {
        return facultyId;
    }

    public void setFacultyId(Long facultyId) {
        this.facultyId = facultyId;
    }

    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public void setEmployeeNumber(String employeeNumber) {
        this.employeeNumber = employeeNumber;
    }

    public String getFacultyName() {
        return facultyName;
    }

    public void setFacultyName(String facultyName) {
        this.facultyName = facultyName;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}

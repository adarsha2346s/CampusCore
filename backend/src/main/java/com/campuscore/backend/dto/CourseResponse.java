package com.campuscore.backend.dto;

public class CourseResponse {

    private Long courseId;
    private Long departmentId;
    private String courseCode;
    private String courseName;
    private Integer credits;
    private Integer capacity;
    private String status;

    public CourseResponse() {
    }

    public CourseResponse(
            Long courseId,
            Long departmentId,
            String courseCode,
            String courseName,
            Integer credits,
            Integer capacity,
            String status) {

        this.courseId = courseId;
        this.departmentId = departmentId;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.credits = credits;
        this.capacity = capacity;
        this.status = status;
    }

    public Long getCourseId() {
        return courseId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public Integer getCredits() {
        return credits;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public String getStatus() {
        return status;
    }
}
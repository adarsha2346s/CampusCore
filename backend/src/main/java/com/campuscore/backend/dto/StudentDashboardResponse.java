package com.campuscore.backend.dto;

import java.util.List;

public class StudentDashboardResponse {

    private Long studentId;
    private String studentName;
    private String enrollmentNumber;
    private Double gpa;
    private List<CourseSummary> courses;
    private List<String> alerts;

    public StudentDashboardResponse() {
    }

    public StudentDashboardResponse(
            Long studentId,
            String studentName,
            String enrollmentNumber,
            Double gpa,
            List<CourseSummary> courses,
            List<String> alerts) {

        this.studentId = studentId;
        this.studentName = studentName;
        this.enrollmentNumber = enrollmentNumber;
        this.gpa = gpa;
        this.courses = courses;
        this.alerts = alerts;
    }

    public Long getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getEnrollmentNumber() {
        return enrollmentNumber;
    }

    public Double getGpa() {
        return gpa;
    }

    public List<CourseSummary> getCourses() {
        return courses;
    }

    public List<String> getAlerts() {
        return alerts;
    }

    public static class CourseSummary {

        private Long courseId;
        private String courseCode;
        private String courseName;
        private Integer credits;
        private Double attendancePercentage;

        public CourseSummary() {
        }

        public CourseSummary(
                Long courseId,
                String courseCode,
                String courseName,
                Integer credits,
                Double attendancePercentage) {

            this.courseId = courseId;
            this.courseCode = courseCode;
            this.courseName = courseName;
            this.credits = credits;
            this.attendancePercentage = attendancePercentage;
        }

        public Long getCourseId() {
            return courseId;
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

        public Double getAttendancePercentage() {
            return attendancePercentage;
        }
    }
}
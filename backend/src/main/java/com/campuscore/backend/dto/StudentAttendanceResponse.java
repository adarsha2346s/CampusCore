package com.campuscore.backend.dto;

/**
 * One of the signed-in student's own attendance rows, with the course and session
 * already joined in.
 *
 * <p>The workspace previously called the institution-wide session listing, which is
 * administrator-only, so a student asking for their own attendance was refused.
 * This route is scoped to the caller's enrollments instead, so the session's date,
 * topic and course travel with the record and the workspace needs one request.
 */
public class StudentAttendanceResponse {

    private Long attendanceRecordId;
    private Long attendanceSessionId;
    private String courseCode;
    private String courseName;
    private String sessionDate;
    private String topic;
    private String status;
    private String markedAt;

    public StudentAttendanceResponse() {
    }

    public StudentAttendanceResponse(
            Long attendanceRecordId,
            Long attendanceSessionId,
            String courseCode,
            String courseName,
            String sessionDate,
            String topic,
            String status,
            String markedAt) {

        this.attendanceRecordId = attendanceRecordId;
        this.attendanceSessionId = attendanceSessionId;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.sessionDate = sessionDate;
        this.topic = topic;
        this.status = status;
        this.markedAt = markedAt;
    }

    public Long getAttendanceRecordId() {
        return attendanceRecordId;
    }

    public Long getAttendanceSessionId() {
        return attendanceSessionId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getSessionDate() {
        return sessionDate;
    }

    public String getTopic() {
        return topic;
    }

    public String getStatus() {
        return status;
    }

    public String getMarkedAt() {
        return markedAt;
    }
}

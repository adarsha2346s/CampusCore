package com.campuscore.backend.dto;

public class AttendanceSessionResponse {

    private Long attendanceSessionId;
    private Long courseId;
    private Long facultyId;
    private String sessionDate;
    private String topic;

    public AttendanceSessionResponse() {
    }

    public AttendanceSessionResponse(
            Long attendanceSessionId,
            Long courseId,
            Long facultyId,
            String sessionDate,
            String topic) {

        this.attendanceSessionId = attendanceSessionId;
        this.courseId = courseId;
        this.facultyId = facultyId;
        this.sessionDate = sessionDate;
        this.topic = topic;
    }

    public Long getAttendanceSessionId() {
        return attendanceSessionId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public Long getFacultyId() {
        return facultyId;
    }

    public String getSessionDate() {
        return sessionDate;
    }

    public String getTopic() {
        return topic;
    }
}
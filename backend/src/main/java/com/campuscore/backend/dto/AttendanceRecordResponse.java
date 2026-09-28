package com.campuscore.backend.dto;

public class AttendanceRecordResponse {

    private Long attendanceRecordId;
    private Long attendanceSessionId;
    private Long enrollmentId;
    private String status;

    public AttendanceRecordResponse() {
    }

    public AttendanceRecordResponse(
            Long attendanceRecordId,
            Long attendanceSessionId,
            Long enrollmentId,
            String status) {

        this.attendanceRecordId = attendanceRecordId;
        this.attendanceSessionId = attendanceSessionId;
        this.enrollmentId = enrollmentId;
        this.status = status;
    }

    public Long getAttendanceRecordId() {
        return attendanceRecordId;
    }

    public Long getAttendanceSessionId() {
        return attendanceSessionId;
    }

    public Long getEnrollmentId() {
        return enrollmentId;
    }

    public String getStatus() {
        return status;
    }
}
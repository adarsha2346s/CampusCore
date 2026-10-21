package com.campuscore.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Payload for changing an attendance session.
 *
 * <p>The course and the faculty member are not editable: they identify who was
 * teaching and which offering the session belongs to, and attendance records are
 * filed against the session itself. Re-pointing it would file existing attendance
 * under a different class or lecturer, so a session filed against the wrong course
 * or lecturer is deleted and re-entered.
 */
public class AttendanceSessionRequest {

    @NotNull(message = "Session date is required")
    private LocalDate sessionDate;

    @Size(max = 255, message = "Topic cannot exceed 255 characters")
    private String topic;

    public AttendanceSessionRequest() {
    }

    public AttendanceSessionRequest(LocalDate sessionDate, String topic) {
        this.sessionDate = sessionDate;
        this.topic = topic;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(LocalDate sessionDate) {
        this.sessionDate = sessionDate;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}
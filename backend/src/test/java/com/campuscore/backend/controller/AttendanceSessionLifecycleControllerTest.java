package com.campuscore.backend.controller;

import com.campuscore.backend.entity.AttendanceSession;
import com.campuscore.backend.entity.Course;
import com.campuscore.backend.entity.Faculty;
import com.campuscore.backend.exception.GlobalExceptionHandler;
import com.campuscore.backend.exception.ResourceConflictException;
import com.campuscore.backend.exception.ResourceNotFoundException;
import com.campuscore.backend.service.AttendanceSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP contract for the attendance session update and delete endpoints, plus a
 * regression check that the V1 create route still takes query parameters. The
 * service is stubbed; the dependency rules are covered by
 * AttendanceSessionLifecycleTest.
 */
class AttendanceSessionLifecycleControllerTest {

    private static final Long SESSION_ID = 500L;

    private AttendanceSessionService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(AttendanceSessionService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AttendanceSessionController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private AttendanceSession stored() {
        Course course = new Course();
        course.setCourseId(7L);

        Faculty faculty = new Faculty();
        faculty.setFacultyId(3L);

        AttendanceSession session = new AttendanceSession();
        session.setAttendanceSessionId(SESSION_ID);
        session.setCourse(course);
        session.setFaculty(faculty);
        session.setSessionDate(LocalDate.of(2026, 2, 9));
        session.setTopic("Matrices");
        session.setCreatedAt(LocalDateTime.of(2026, 2, 2, 8, 0));
        return session;
    }

    @Test
    @DisplayName("Update returns the changed session")
    void updateReturnsSession() throws Exception {
        when(service.updateSession(eq(SESSION_ID), any())).thenReturn(stored());

        mockMvc.perform(put("/api/v1/attendance-sessions/{id}", SESSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "sessionDate": "2026-02-09", "topic": "Matrices" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendanceSessionId").value(SESSION_ID))
                .andExpect(jsonPath("$.sessionDate").value("2026-02-09"))
                .andExpect(jsonPath("$.topic").value("Matrices"))
                .andExpect(jsonPath("$.courseId").value(7))
                .andExpect(jsonPath("$.facultyId").value(3));
    }

    @Test
    @DisplayName("Update rejects a missing session date")
    void updateRejectsMissingDate() throws Exception {
        mockMvc.perform(put("/api/v1/attendance-sessions/{id}", SESSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "topic": "Matrices" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.sessionDate").exists());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Update rejects an unparseable date")
    void updateRejectsBadDate() throws Exception {
        mockMvc.perform(put("/api/v1/attendance-sessions/{id}", SESSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "sessionDate": "09-02-2026" }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Update rejects an oversized topic")
    void updateRejectsOversizedTopic() throws Exception {
        mockMvc.perform(put("/api/v1/attendance-sessions/{id}", SESSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "sessionDate": "2026-02-09", "topic": "%s" }
                                """.formatted("x".repeat(256))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.topic").exists());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("A date correction is allowed even when attendance exists")
    void dateCorrectionAllowedWithAttendance() throws Exception {
        when(service.updateSession(eq(SESSION_ID), any())).thenReturn(stored());

        mockMvc.perform(put("/api/v1/attendance-sessions/{id}", SESSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "sessionDate": "2026-02-10", "topic": "Matrices" }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Delete returns no content when no attendance exists")
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/attendance-sessions/{id}", SESSION_ID))
                .andExpect(status().isNoContent());

        verify(service).deleteSession(SESSION_ID);
    }

    @Test
    @DisplayName("Delete is reported as a conflict when attendance exists")
    void deleteConflictIsReported() throws Exception {
        doThrow(new ResourceConflictException(
                "Attendance session cannot be deleted while attendance records exist for it"))
                .when(service).deleteSession(SESSION_ID);

        mockMvc.perform(delete("/api/v1/attendance-sessions/{id}", SESSION_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Attendance session cannot be deleted while attendance records exist for it"));
    }

    @Test
    @DisplayName("Deleting an unknown session reports not found")
    void deleteUnknownIsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Attendance session not found"))
                .when(service).deleteSession(404L);

        mockMvc.perform(delete("/api/v1/attendance-sessions/{id}", 404L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("V1 create route still accepts query parameters")
    void createRouteUnchanged() throws Exception {
        when(service.createSession(7L, 3L, LocalDate.of(2026, 2, 9), "Matrices"))
                .thenReturn(stored());

        mockMvc.perform(post("/api/v1/attendance-sessions")
                        .param("courseId", "7")
                        .param("facultyId", "3")
                        .param("sessionDate", "2026-02-09")
                        .param("topic", "Matrices"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attendanceSessionId").value(SESSION_ID));
    }
}
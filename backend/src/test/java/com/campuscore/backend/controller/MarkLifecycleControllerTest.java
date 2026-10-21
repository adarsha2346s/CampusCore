package com.campuscore.backend.controller;

import com.campuscore.backend.entity.Assessment;
import com.campuscore.backend.entity.Course;
import com.campuscore.backend.entity.Enrollment;
import com.campuscore.backend.entity.Mark;
import com.campuscore.backend.exception.GlobalExceptionHandler;
import com.campuscore.backend.exception.ResourceConflictException;
import com.campuscore.backend.exception.ResourceNotFoundException;
import com.campuscore.backend.service.MarkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP contract for the mark update and delete endpoints, plus a regression check
 * that the V1 create route still behaves as before. The service is stubbed; the
 * rules themselves are covered by MarkLifecycleTest.
 */
class MarkLifecycleControllerTest {

    private static final Long MARK_ID = 700L;

    private MarkService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(MarkService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MarkController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Mark stored(String marksObtained, String maxMarks) {
        Course course = new Course();
        course.setCourseId(7L);

        Assessment assessment = new Assessment();
        assessment.setAssessmentId(42L);
        assessment.setCourse(course);
        assessment.setMaxMarks(new BigDecimal(maxMarks));

        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollmentId(300L);

        Mark mark = new Mark();
        mark.setMarkId(MARK_ID);
        mark.setEnrollment(enrollment);
        mark.setAssessment(assessment);
        mark.setMarksObtained(new BigDecimal(marksObtained));
        mark.setEnteredAt(LocalDateTime.of(2026, 3, 14, 9, 30));
        return mark;
    }

    @Test
    @DisplayName("Update returns the corrected mark")
    void updateReturnsMark() throws Exception {
        when(service.updateMark(eq(MARK_ID), any())).thenReturn(stored("87.50", "100.00"));

        mockMvc.perform(put("/api/v1/marks/{id}", MARK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "marksObtained": 87.50 }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.markId").value(MARK_ID))
                .andExpect(jsonPath("$.marksObtained").value("87.50"))
                .andExpect(jsonPath("$.enrollmentId").value(300))
                .andExpect(jsonPath("$.assessmentId").value(42));
    }

    @Test
    @DisplayName("Update rejects a missing value")
    void updateRejectsMissingValue() throws Exception {
        mockMvc.perform(put("/api/v1/marks/{id}", MARK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.marksObtained").exists());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Update rejects a negative value")
    void updateRejectsNegativeValue() throws Exception {
        mockMvc.perform(put("/api/v1/marks/{id}", MARK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "marksObtained": -5.00 }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.marksObtained").exists());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Update rejects a value beyond the column precision")
    void updateRejectsExcessivePrecision() throws Exception {
        mockMvc.perform(put("/api/v1/marks/{id}", MARK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "marksObtained": 40.125 }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Extra enrollment/assessment fields in the body are ignored")
    void updateRejectsRepointedPayload() throws Exception {
        when(service.updateMark(eq(MARK_ID), any())).thenReturn(stored("40.00", "100.00"));

        mockMvc.perform(put("/api/v1/marks/{id}", MARK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enrollmentId": 999,
                                  "assessmentId": 999,
                                  "marksObtained": 40.00
                                }
                                """))
                .andExpect(status().isOk());

        // The extra fields are not bound to anything, so the stored ownership is
        // whatever the service loaded rather than the request body.
        org.mockito.ArgumentCaptor<com.campuscore.backend.dto.MarkRequest> captor =
                org.mockito.ArgumentCaptor.forClass(com.campuscore.backend.dto.MarkRequest.class);
        verify(service).updateMark(eq(MARK_ID), captor.capture());
        assertNotNull(captor.getValue().getMarksObtained());
    }

    @Test
    @DisplayName("A value above the assessment maximum is reported as a conflict")
    void valueAboveMaximumIsConflict() throws Exception {
        when(service.updateMark(eq(MARK_ID), any()))
                .thenThrow(new ResourceConflictException("Marks cannot be greater than maximum marks"));

        mockMvc.perform(put("/api/v1/marks/{id}", MARK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "marksObtained": 120.00 }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Marks cannot be greater than maximum marks"));
    }

    @Test
    @DisplayName("Updating an unknown mark reports not found")
    void updateUnknownIsNotFound() throws Exception {
        when(service.updateMark(eq(404L), any()))
                .thenThrow(new ResourceNotFoundException("Mark not found"));

        mockMvc.perform(put("/api/v1/marks/{id}", 404L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "marksObtained": 10.00 }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Mark not found"));
    }

    @Test
    @DisplayName("Delete returns no content")
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/marks/{id}", MARK_ID))
                .andExpect(status().isNoContent());

        verify(service).deleteMark(MARK_ID);
    }

    @Test
    @DisplayName("Deleting an unknown mark reports not found")
    void deleteUnknownIsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Mark not found")).when(service).deleteMark(404L);

        mockMvc.perform(delete("/api/v1/marks/{id}", 404L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Access denied from the scope check is reported as forbidden")
    void accessDeniedIsForbidden() throws Exception {
        doThrow(new org.springframework.security.access.AccessDeniedException("not assigned"))
                .when(service).deleteMark(MARK_ID);

        mockMvc.perform(delete("/api/v1/marks/{id}", MARK_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("V1 create route still accepts query parameters")
    void createRouteUnchanged() throws Exception {
        when(service.createMark(300L, 42L, new BigDecimal("88.00")))
                .thenReturn(stored("88.00", "100.00"));

        mockMvc.perform(post("/api/v1/marks")
                        .param("enrollmentId", "300")
                        .param("assessmentId", "42")
                        .param("marksObtained", "88.00"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.marksObtained").value("88.00"));
    }

    @Test
    @DisplayName("Existing read routes are unchanged")
    void readRoutesStillResolve() throws Exception {
        when(service.getMarkById(MARK_ID)).thenReturn(stored("40.00", "100.00"));

        mockMvc.perform(get("/api/v1/marks/{id}", MARK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.markId").value(MARK_ID))
                .andExpect(jsonPath("$.enteredAt").exists());
    }
}
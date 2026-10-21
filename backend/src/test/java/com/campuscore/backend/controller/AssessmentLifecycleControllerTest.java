package com.campuscore.backend.controller;

import com.campuscore.backend.entity.Assessment;
import com.campuscore.backend.entity.Course;
import com.campuscore.backend.exception.GlobalExceptionHandler;
import com.campuscore.backend.exception.ResourceConflictException;
import com.campuscore.backend.exception.ResourceNotFoundException;
import com.campuscore.backend.service.AssessmentService;
import com.campuscore.backend.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP contract for the assessment update and delete endpoints: verbs, validation
 * and status codes. The service is stubbed; the dependency rules themselves are
 * covered by AssessmentLifecycleTest.
 */
class AssessmentLifecycleControllerTest {

    private static final Long ASSESSMENT_ID = 42L;

    private AssessmentService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(AssessmentService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AssessmentController(service, mock(StudentService.class)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Assessment stored(BigDecimal maxMarks) {
        Course course = new Course();
        course.setCourseId(7L);

        Assessment assessment = new Assessment();
        assessment.setAssessmentId(ASSESSMENT_ID);
        assessment.setCourse(course);
        assessment.setName("Midterm Exam");
        assessment.setAssessmentType(Assessment.AssessmentType.MIDTERM);
        assessment.setMaxMarks(maxMarks);
        assessment.setWeight(new BigDecimal("30.00"));
        return assessment;
    }

    private String validBody() {
        return """
                {
                  "name": "Final Exam",
                  "assessmentType": "FINAL",
                  "maxMarks": 100.00,
                  "weight": 40.00,
                  "assessmentDate": "2026-05-04"
                }
                """;
    }

    @Test
    @DisplayName("Update returns the stored assessment")
    void updateReturnsAssessment() throws Exception {
        when(service.updateAssessment(eq(ASSESSMENT_ID), any()))
                .thenReturn(stored(new BigDecimal("100.00")));

        mockMvc.perform(put("/api/v1/assessments/{id}", ASSESSMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assessmentId").value(ASSESSMENT_ID))
                .andExpect(jsonPath("$.name").value("Midterm Exam"))
                .andExpect(jsonPath("$.assessmentType").value("MIDTERM"));
    }

    @Test
    @DisplayName("Update rejects a missing name")
    void updateRejectsMissingName() throws Exception {
        mockMvc.perform(put("/api/v1/assessments/{id}", ASSESSMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assessmentType": "FINAL", "maxMarks": 100.00, "weight": 40.00}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Update rejects maximum marks of zero")
    void updateRejectsZeroMaxMarks() throws Exception {
        mockMvc.perform(put("/api/v1/assessments/{id}", ASSESSMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Final Exam",
                                  "assessmentType": "FINAL",
                                  "maxMarks": 0,
                                  "weight": 40.00
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.maxMarks").exists());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Update rejects a weight above 100")
    void updateRejectsWeightAboveHundred() throws Exception {
        mockMvc.perform(put("/api/v1/assessments/{id}", ASSESSMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Final Exam",
                                  "assessmentType": "FINAL",
                                  "maxMarks": 100.00,
                                  "weight": 140.00
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.weight").exists());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Update rejects an unknown assessment type")
    void updateRejectsUnknownType() throws Exception {
        mockMvc.perform(put("/api/v1/assessments/{id}", ASSESSMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Final Exam",
                                  "assessmentType": "THESIS",
                                  "maxMarks": 100.00,
                                  "weight": 40.00
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Rescaling maximum marks with marks present is reported as a conflict")
    void rescaleConflictIsReported() throws Exception {
        when(service.updateAssessment(eq(ASSESSMENT_ID), any()))
                .thenThrow(new ResourceConflictException(
                        "Maximum marks cannot be changed once marks reference this assessment"));

        mockMvc.perform(put("/api/v1/assessments/{id}", ASSESSMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Maximum marks cannot be changed once marks reference this assessment"));
    }

    @Test
    @DisplayName("Updating an unknown assessment reports not found")
    void updateUnknownIsNotFound() throws Exception {
        when(service.updateAssessment(eq(404L), any()))
                .thenThrow(new ResourceNotFoundException("Assessment not found"));

        mockMvc.perform(put("/api/v1/assessments/{id}", 404L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Assessment not found"));
    }

    @Test
    @DisplayName("Delete returns no content when no marks reference the assessment")
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/assessments/{id}", ASSESSMENT_ID))
                .andExpect(status().isNoContent());

        verify(service).deleteAssessment(ASSESSMENT_ID);
    }

    @Test
    @DisplayName("Delete is reported as a conflict when marks reference the assessment")
    void deleteConflictIsReported() throws Exception {
        doThrow(new ResourceConflictException(
                "Assessment cannot be deleted while marks reference it"))
                .when(service).deleteAssessment(ASSESSMENT_ID);

        mockMvc.perform(delete("/api/v1/assessments/{id}", ASSESSMENT_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Assessment cannot be deleted while marks reference it"));
    }

    @Test
    @DisplayName("Deleting an unknown assessment reports not found")
    void deleteUnknownIsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Assessment not found"))
                .when(service).deleteAssessment(404L);

        mockMvc.perform(delete("/api/v1/assessments/{id}", 404L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Existing read routes are unchanged")
    void readRoutesStillResolve() throws Exception {
        when(service.getAssessmentById(ASSESSMENT_ID)).thenReturn(stored(new BigDecimal("100.00")));

        mockMvc.perform(get("/api/v1/assessments/{id}", ASSESSMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assessmentId").value(ASSESSMENT_ID));
    }
}
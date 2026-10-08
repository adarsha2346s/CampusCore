package com.campuscore.backend.controller;

import com.campuscore.backend.dto.FacultyAssignmentResponse;
import com.campuscore.backend.service.FacultyCourseAssignmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

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
 * HTTP contract for the assignment endpoints: routing, verbs, validation and
 * response shape. The service is stubbed; behaviour is covered by
 * FacultyCourseAssignmentServiceTest.
 */
class FacultyAssignmentControllerTest {

    private FacultyCourseAssignmentService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(FacultyCourseAssignmentService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FacultyAssignmentController(service))
                .setControllerAdvice(new com.campuscore.backend.exception.GlobalExceptionHandler())
                .build();
    }

    private FacultyAssignmentResponse response() {
        return new FacultyAssignmentResponse(
                900L, 12L, "CSE101", "Data Structures", 21L,
                "FAC-001", "Jamie Patel",
                LocalDateTime.of(2026, 1, 1, 9, 0), null, true);
    }

    @Test
    void listsActiveAssignments() throws Exception {
        when(service.getActiveAssignments(12L)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/v1/courses/12/faculty-assignments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courseId").value(12))
                .andExpect(jsonPath("$[0].courseCode").value("CSE101"))
                .andExpect(jsonPath("$[0].facultyId").value(21))
                .andExpect(jsonPath("$[0].facultyName").value("Jamie Patel"))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void listsAssignmentHistory() throws Exception {
        FacultyAssignmentResponse closed = new FacultyAssignmentResponse(
                901L, 12L, "CSE101", "Data Structures", 22L,
                "FAC-002", "Morgan Rivera",
                LocalDateTime.of(2025, 1, 1, 9, 0),
                LocalDateTime.of(2026, 1, 1, 9, 0), false);

        when(service.getAssignmentHistory(12L)).thenReturn(List.of(closed));

        mockMvc.perform(get("/api/v1/courses/12/faculty-assignments/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].active").value(false))
                .andExpect(jsonPath("$[0].endedAt").isNotEmpty());
    }

    @Test
    void assignsFaculty() throws Exception {
        when(service.assignFaculty(eq(12L), any())).thenReturn(response());

        mockMvc.perform(post("/api/v1/courses/12/faculty-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"facultyId\":21}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.facultyId").value(21));

        verify(service).assignFaculty(12L, 21L);
    }

    @Test
    void reassignsFaculty() throws Exception {
        when(service.reassignFaculty(eq(12L), any())).thenReturn(response());

        mockMvc.perform(put("/api/v1/courses/12/faculty-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"facultyId\":22}"))
                .andExpect(status().isOk());

        verify(service).reassignFaculty(12L, 22L);
    }

    @Test
    void removesAssignment() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/12/faculty-assignments/21"))
                .andExpect(status().isNoContent());

        verify(service).removeAssignment(12L, 21L);
    }

    @Test
    void rejectsMissingFacultyIdOnAssign() throws Exception {
        mockMvc.perform(post("/api/v1/courses/12/faculty-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void rejectsMissingFacultyIdOnReassign() throws Exception {
        mockMvc.perform(put("/api/v1/courses/12/faculty-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void surfacesServiceConflicts() throws Exception {
        doThrow(new com.campuscore.backend.exception.ResourceConflictException("already assigned"))
                .when(service).assignFaculty(eq(12L), any());

        mockMvc.perform(post("/api/v1/courses/12/faculty-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"facultyId\":21}"))
                .andExpect(status().isConflict());
    }

}

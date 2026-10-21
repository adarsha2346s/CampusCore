package com.campuscore.backend.controller;

import com.campuscore.backend.entity.Assessment;
import com.campuscore.backend.entity.Student;
import com.campuscore.backend.entity.User;
import com.campuscore.backend.repository.CourseRepository;
import com.campuscore.backend.repository.EnrollmentRepository;
import com.campuscore.backend.repository.FacultyCourseAssignmentRepository;
import com.campuscore.backend.repository.FacultyRepository;
import com.campuscore.backend.repository.StudentRepository;
import com.campuscore.backend.repository.UserRepository;
import com.campuscore.backend.service.AccessScope;
import com.campuscore.backend.service.AssessmentService;
import com.campuscore.backend.service.StudentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /api/v1/assessments/me - assessment metadata for the calling student's own
 * marks.
 *
 * <p>The route deliberately carries no identifier, so the only way to reach
 * another student's data would be for the service to accept a caller-supplied id.
 * These tests pin that it does not, and that the pre-existing assessment routes
 * are unchanged.
 */
class StudentAssessmentSelfTest {

    private static final Long STUDENT_ID = 7L;
    private static final Long OTHER_STUDENT_ID = 900L;

    private com.campuscore.backend.repository.AssessmentRepository assessments;
    private UserRepository users;
    private StudentRepository students;
    private AssessmentService assessmentService;
    private StudentService studentService;
    private AssessmentController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        assessments = mock(com.campuscore.backend.repository.AssessmentRepository.class);
        users = mock(UserRepository.class);
        students = mock(StudentRepository.class);

        AccessScope accessScope = new AccessScope(
                users,
                students,
                mock(FacultyRepository.class),
                mock(FacultyCourseAssignmentRepository.class),
                mock(EnrollmentRepository.class));

        assessmentService = new AssessmentService(
                assessments, mock(CourseRepository.class), accessScope);
        studentService = mock(StudentService.class);
        controller = new AssessmentController(assessmentService, studentService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    /**
     * Wires both identity resolutions the request depends on:
     * the controller asks StudentService, and AccessScope independently resolves
     * the caller through UserRepository -> StudentRepository. They must agree.
     */
    private void sessionResolvesTo(Long studentId) {
        Student student = new Student();
        student.setStudentId(studentId);
        student.setEnrollmentNumber("DEMO-" + studentId);
        when(studentService.getStudentForUsername("student-account")).thenReturn(student);

        User account = new User();
        account.setUserId(1L);
        account.setUsername("student-account");
        account.setRole(User.Role.STUDENT);
        account.setActive(true);
        when(users.findByUsername("student-account")).thenReturn(Optional.of(account));
        when(students.findByUserUserId(1L)).thenReturn(Optional.of(student));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Builds the authentication and publishes it to the SecurityContextHolder.
     *
     * AccessScope deliberately reads the verified security context rather than
     * the request principal, so both must be present for the ownership check to
     * behave as it does in production.
     */
    private static UsernamePasswordAuthenticationToken auth(String username, String... roles) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        username,
                        "n/a",
                        java.util.Arrays.stream(roles)
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                                .toList());

        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        return authentication;
    }

    private static Assessment assessment(long id, String name) {
        Assessment assessment = new Assessment();
        assessment.setAssessmentId(id);
        assessment.setName(name);
        assessment.setAssessmentType(Assessment.AssessmentType.MIDTERM);
        assessment.setMaxMarks(new BigDecimal("100.00"));
        assessment.setWeight(new BigDecimal("25.00"));
        return assessment;
    }

    // ------------------------------------------------------------------
    // Student receives their own assessment metadata
    // ------------------------------------------------------------------

    @Test
    void studentReceivesAssessmentMetadataForTheirOwnMarks() throws Exception {
        sessionResolvesTo(STUDENT_ID);
        when(assessments.findDistinctForStudent(STUDENT_ID))
                .thenReturn(List.of(assessment(1L, "Midterm"), assessment(2L, "Final")));

        mockMvc.perform(get("/api/v1/assessments/me")
                        .principal(auth("student-account", "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].assessmentId").value(1))
                .andExpect(jsonPath("$[0].name").value("Midterm"))
                .andExpect(jsonPath("$[0].assessmentType").value("MIDTERM"))
                .andExpect(jsonPath("$[0].maxMarks").value("100.00"))
                .andExpect(jsonPath("$[1].name").value("Final"));

        verify(assessments).findDistinctForStudent(STUDENT_ID);
    }

    @Test
    void assessmentPayloadOmitsAdministrativeFields() throws Exception {
        sessionResolvesTo(STUDENT_ID);
        when(assessments.findDistinctForStudent(STUDENT_ID))
                .thenReturn(List.of(assessment(1L, "Midterm")));

        String body = mockMvc.perform(get("/api/v1/assessments/me")
                        .principal(auth("student-account", "STUDENT")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Course linkage and weighting stay on the administrator/faculty DTO.
        assertTrue(!body.contains("courseId"), body);
        assertTrue(!body.contains("weight"), body);
    }

    @Test
    void aStudentWithNoMarksReceivesAnEmptyList() throws Exception {
        sessionResolvesTo(STUDENT_ID);
        when(assessments.findDistinctForStudent(STUDENT_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/assessments/me")
                        .principal(auth("student-account", "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    // ------------------------------------------------------------------
    // Another student's data is unreachable
    // ------------------------------------------------------------------

    @Test
    void theRouteAcceptsNoStudentIdentifier() throws Exception {
        sessionResolvesTo(STUDENT_ID);
        when(assessments.findDistinctForStudent(anyLong())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/assessments/me/900")
                        .principal(auth("student-account", "STUDENT")))
                .andExpect(status().isNotFound());

        verify(assessments, never()).findDistinctForStudent(OTHER_STUDENT_ID);
    }

    @Test
    void aQueryParameterCannotSubstituteForTheSessionIdentity() throws Exception {
        sessionResolvesTo(STUDENT_ID);
        when(assessments.findDistinctForStudent(anyLong())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/assessments/me")
                        .param("studentId", String.valueOf(OTHER_STUDENT_ID))
                        .param("student_id", String.valueOf(OTHER_STUDENT_ID))
                        .principal(auth("student-account", "STUDENT")))
                .andExpect(status().isOk());

        verify(assessments).findDistinctForStudent(STUDENT_ID);
        verify(assessments, never()).findDistinctForStudent(OTHER_STUDENT_ID);
    }

    @Test
    void theServiceRefusesAnAssessmentLookupForAnotherStudent() {
        // Defence in depth: even if a future caller passed another id, the service
        // re-checks ownership against the security context.
        Student caller = new Student();
        caller.setStudentId(STUDENT_ID);
        when(students.findByUserUserId(anyLong())).thenReturn(Optional.of(caller));

        assertThrows(AccessDeniedException.class,
                () -> assessmentService.getAssessmentsForStudent(OTHER_STUDENT_ID));

        verify(assessments, never()).findDistinctForStudent(OTHER_STUDENT_ID);
    }

    @Test
    void theServiceFailsClosedWhenScopeIsUnavailable() {
        AssessmentService unscoped = new AssessmentService(
                assessments, mock(CourseRepository.class));

        assertThrows(AccessDeniedException.class,
                () -> unscoped.getAssessmentsForStudent(STUDENT_ID));
    }

    // ------------------------------------------------------------------
    // Other roles are refused
    // ------------------------------------------------------------------

    @Test
    void facultyAndAdminAreRefusedByTheController() {
        for (String role : List.of("FACULTY", "ADMIN")) {
            var response = controller.getMyAssessments(auth(role.toLowerCase() + "-account", role));

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), role);
        }

        verify(studentService, never()).getStudentForUsername(any());
    }

    @Test
    void aPrincipalWithNoRoleIsRefused() {
        var response = controller.getMyAssessments(
                new UsernamePasswordAuthenticationToken("anonymous", "n/a", List.of()));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(studentService, never()).getStudentForUsername(any());
    }
}

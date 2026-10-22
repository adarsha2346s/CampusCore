package com.campuscore.backend.controller;

import com.campuscore.backend.entity.AttendanceSession;
import com.campuscore.backend.entity.Course;
import com.campuscore.backend.entity.Faculty;
import com.campuscore.backend.entity.User;
import com.campuscore.backend.exception.GlobalExceptionHandler;
import com.campuscore.backend.repository.AttendanceSessionRepository;
import com.campuscore.backend.repository.CourseRepository;
import com.campuscore.backend.repository.FacultyCourseAssignmentRepository;
import com.campuscore.backend.repository.FacultyRepository;
import com.campuscore.backend.repository.StudentRepository;
import com.campuscore.backend.repository.UserRepository;
import com.campuscore.backend.service.AccessScope;
import com.campuscore.backend.service.AuditLogService;
import com.campuscore.backend.service.AttendanceSessionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A faculty member may only list their own attendance sessions.
 *
 * <p>The unpaged listing enforced this and the paginated listing did not, so
 * appending {@code ?page=0&size=50} to another faculty member's session URL
 * returned their sessions. Both routes now go through the same ownership
 * check, and these tests hold them to it.
 */
class FacultyAttendanceSessionScopeTest {

    private static final Long FACULTY_ID = 21L;
    private static final Long OTHER_FACULTY_ID = 22L;
    private static final Long COURSE_ID = 11L;

    private AttendanceSessionRepository sessions;
    private FacultyRepository faculty;
    private UserRepository users;
    private AttendanceSessionService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        sessions = mock(AttendanceSessionRepository.class);
        faculty = mock(FacultyRepository.class);
        users = mock(UserRepository.class);

        AccessScope accessScope = new AccessScope(
                users,
                mock(StudentRepository.class),
                faculty,
                mock(FacultyCourseAssignmentRepository.class),
                mock(com.campuscore.backend.repository.EnrollmentRepository.class));

        service = new AttendanceSessionService(
                sessions,
                mock(CourseRepository.class),
                faculty,
                accessScope,
                null, mock(AuditLogService.class));

        AttendanceSessionController controller = new AttendanceSessionController(service);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------
    // Own sessions
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a faculty member can list their own sessions")
    void facultyCanListTheirOwnSessions() throws Exception {
        asFaculty(FACULTY_ID);
        ownSessionsArePresent();

        mockMvc.perform(get("/api/v1/attendance-sessions/faculty/{id}", FACULTY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("a faculty member can list their own sessions as a page")
    void facultyCanListTheirOwnSessionsAsAPage() throws Exception {
        asFaculty(FACULTY_ID);
        ownSessionsArePresent();

        mockMvc.perform(get("/api/v1/attendance-sessions/faculty/{id}", FACULTY_ID)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    // ------------------------------------------------------------------
    // Another faculty member's sessions
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a faculty member is refused another faculty member's sessions")
    void facultyIsRefusedAnotherFacultyMembersSessions() throws Exception {
        asFaculty(FACULTY_ID);
        ownSessionsArePresent();

        mockMvc.perform(get("/api/v1/attendance-sessions/faculty/{id}", OTHER_FACULTY_ID))
                .andExpect(status().isForbidden());

        verify(sessions, never()).findByFacultyFacultyId(eq(OTHER_FACULTY_ID));
    }

    @Test
    @DisplayName("pagination does not bypass the refusal")
    void paginationDoesNotBypassTheRefusal() throws Exception {
        asFaculty(FACULTY_ID);
        ownSessionsArePresent();

        mockMvc.perform(get("/api/v1/attendance-sessions/faculty/{id}", OTHER_FACULTY_ID)
                        .param("page", "0")
                        .param("size", "50"))
                .andExpect(status().isForbidden());

        verify(sessions, never()).findByFacultyFacultyId(eq(OTHER_FACULTY_ID), any(Pageable.class));
        verify(sessions, never()).findByFacultyFacultyId(eq(OTHER_FACULTY_ID));
    }

    // ------------------------------------------------------------------
    // Admin
    // ------------------------------------------------------------------

    @Test
    @DisplayName("an administrator may list any faculty member's sessions")
    void adminMayListAnyFacultyMembersSessions() throws Exception {
        asAdmin();
        ownSessionsArePresent();

        mockMvc.perform(get("/api/v1/attendance-sessions/faculty/{id}", OTHER_FACULTY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("an administrator may list any faculty member's sessions as a page")
    void adminMayListAnyFacultyMembersSessionsAsAPage() throws Exception {
        asAdmin();
        ownSessionsArePresent();

        mockMvc.perform(get("/api/v1/attendance-sessions/faculty/{id}", OTHER_FACULTY_ID)
                        .param("page", "0")
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ------------------------------------------------------------------
    // Fail closed
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a faculty account with no faculty profile is refused")
    void facultyWithoutProfileIsRefused() throws Exception {
        User account = new User();
        account.setUserId(1L);
        account.setUsername("faculty-account");
        when(users.findByUsername("faculty-account")).thenReturn(Optional.of(account));
        when(faculty.findByUserUserId(1L)).thenReturn(Optional.empty());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "faculty-account", "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_FACULTY"))));

        ownSessionsArePresent();

        mockMvc.perform(get("/api/v1/attendance-sessions/faculty/{id}", OTHER_FACULTY_ID)
                        .param("page", "0")
                        .param("size", "50"))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private void ownSessionsArePresent() {
        when(sessions.findByFacultyFacultyId(eq(FACULTY_ID)))
                .thenReturn(List.of(session(FACULTY_ID)));
        when(sessions.findByFacultyFacultyId(eq(OTHER_FACULTY_ID)))
                .thenReturn(List.of(session(OTHER_FACULTY_ID)));
        when(sessions.findByFacultyFacultyId(eq(FACULTY_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(session(FACULTY_ID))));
        when(sessions.findByFacultyFacultyId(eq(OTHER_FACULTY_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(session(OTHER_FACULTY_ID))));
    }

    private AttendanceSession session(Long owningFacultyId) {
        Course course = new Course();
        course.setCourseId(COURSE_ID);
        course.setCourseCode("CSE101");
        course.setCourseName("Programming");

        Faculty owner = new Faculty();
        owner.setFacultyId(owningFacultyId);

        AttendanceSession session = new AttendanceSession();
        session.setAttendanceSessionId(500L);
        session.setCourse(course);
        session.setFaculty(owner);
        session.setSessionDate(LocalDate.of(2026, 9, 1));
        session.setTopic("Lecture");
        session.setCreatedAt(LocalDateTime.of(2026, 9, 1, 9, 0));
        return session;
    }

    private void asFaculty(long facultyId) {
        User account = new User();
        account.setUserId(1L);
        account.setUsername("faculty-account");
        when(users.findByUsername("faculty-account")).thenReturn(Optional.of(account));

        Faculty me = new Faculty();
        me.setFacultyId(facultyId);
        when(faculty.findByUserUserId(1L)).thenReturn(Optional.of(me));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "faculty-account", "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_FACULTY"))));
    }

    private void asAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin-account", "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }
}

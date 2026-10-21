package com.campuscore.backend.service;

import com.campuscore.backend.dto.AttendanceSessionRequest;
import com.campuscore.backend.entity.AttendanceSession;
import com.campuscore.backend.entity.Course;
import com.campuscore.backend.entity.Faculty;
import com.campuscore.backend.exception.ResourceConflictException;
import com.campuscore.backend.exception.ResourceNotFoundException;
import com.campuscore.backend.repository.AttendanceRecordRepository;
import com.campuscore.backend.repository.AttendanceSessionRepository;
import com.campuscore.backend.repository.CourseRepository;
import com.campuscore.backend.repository.FacultyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Attendance session update and dependency-aware delete.
 *
 * <p>An attendance record is the child of a session, so the rules that matter are
 * blocking deletion once attendance exists, keeping course and faculty immutable
 * so recorded attendance cannot be filed under the wrong class, and preserving the
 * original creation timestamp.
 */
class AttendanceSessionLifecycleTest {

    private static final Long SESSION_ID = 500L;
    private static final Long COURSE_ID = 7L;
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 2, 2, 8, 0);

    private AttendanceSessionRepository sessions;
    private AttendanceRecordRepository records;
    private AccessScope accessScope;
    private AttendanceSessionService service;

    @BeforeEach
    void setUp() {
        sessions = mock(AttendanceSessionRepository.class);
        records = mock(AttendanceRecordRepository.class);
        accessScope = mock(AccessScope.class);

        service = new AttendanceSessionService(
                sessions,
                mock(CourseRepository.class),
                mock(FacultyRepository.class),
                accessScope,
                records);
    }

    private AttendanceSession existing() {
        Course course = new Course();
        course.setCourseId(COURSE_ID);

        Faculty faculty = new Faculty();
        faculty.setFacultyId(3L);

        AttendanceSession session = new AttendanceSession();
        session.setAttendanceSessionId(SESSION_ID);
        session.setCourse(course);
        session.setFaculty(faculty);
        session.setSessionDate(LocalDate.of(2026, 2, 2));
        session.setTopic("Linear algebra");
        session.setCreatedAt(CREATED_AT);
        return session;
    }

    private void savingSucceeds() {
        when(sessions.save(any(AttendanceSession.class)))
                .thenAnswer(call -> call.getArgument(0));
    }

    @Test
    @DisplayName("Update applies the corrected date and topic")
    void updateAppliesDateAndTopic() {
        AttendanceSession stored = existing();
        when(sessions.findById(SESSION_ID)).thenReturn(Optional.of(stored));
        savingSucceeds();

        AttendanceSession updated = service.updateSession(
                SESSION_ID, new AttendanceSessionRequest(LocalDate.of(2026, 2, 9), "Matrices"));

        assertEquals(LocalDate.of(2026, 2, 9), updated.getSessionDate());
        assertEquals("Matrices", updated.getTopic());
        verify(accessScope).requireCourseAccess(COURSE_ID);
    }

    @Test
    @DisplayName("Update keeps the course and faculty who ran the session")
    void updateKeepsCourseAndFaculty() {
        AttendanceSession stored = existing();
        when(sessions.findById(SESSION_ID)).thenReturn(Optional.of(stored));
        savingSucceeds();

        AttendanceSession updated = service.updateSession(
                SESSION_ID, new AttendanceSessionRequest(LocalDate.of(2026, 2, 9), "Matrices"));

        assertEquals(COURSE_ID, updated.getCourse().getCourseId());
        assertEquals(3L, updated.getFaculty().getFacultyId());
    }

    @Test
    @DisplayName("Update preserves the original creation timestamp")
    void updatePreservesCreatedAt() {
        AttendanceSession stored = existing();
        when(sessions.findById(SESSION_ID)).thenReturn(Optional.of(stored));
        savingSucceeds();

        AttendanceSession updated = service.updateSession(
                SESSION_ID, new AttendanceSessionRequest(LocalDate.of(2026, 2, 9), "Matrices"));

        assertEquals(CREATED_AT, updated.getCreatedAt());
    }

    @Test
    @DisplayName("Update allows a topic to be cleared")
    void updateAllowsClearedTopic() {
        AttendanceSession stored = existing();
        when(sessions.findById(SESSION_ID)).thenReturn(Optional.of(stored));
        savingSucceeds();

        AttendanceSession updated = service.updateSession(
                SESSION_ID, new AttendanceSessionRequest(LocalDate.of(2026, 2, 9), null));

        assertEquals(null, updated.getTopic());
    }

    @Test
    @DisplayName("Update reports not found for an unknown session")
    void updateUnknownSession() {
        when(sessions.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateSession(
                        404L, new AttendanceSessionRequest(LocalDate.of(2026, 2, 9), "Matrices")));
    }

    @Test
    @DisplayName("Update refuses a caller outside the course's scope")
    void updateRefusesOutOfScopeCaller() {
        AttendanceSession stored = existing();
        when(sessions.findById(SESSION_ID)).thenReturn(Optional.of(stored));
        org.mockito.Mockito.doThrow(new AccessDeniedException("not assigned"))
                .when(accessScope).requireCourseAccess(COURSE_ID);

        assertThrows(AccessDeniedException.class,
                () -> service.updateSession(
                        SESSION_ID, new AttendanceSessionRequest(LocalDate.of(2026, 2, 9), "Matrices")));

        verify(sessions, never()).save(any(AttendanceSession.class));
    }

    @Test
    @DisplayName("Delete succeeds when no attendance was recorded")
    void deleteSucceedsWithoutAttendance() {
        AttendanceSession stored = existing();
        when(sessions.findById(SESSION_ID)).thenReturn(Optional.of(stored));
        when(records.countByAttendanceSessionAttendanceSessionId(SESSION_ID)).thenReturn(0L);

        service.deleteSession(SESSION_ID);

        verify(sessions).delete(stored);
    }

    @Test
    @DisplayName("Delete is refused when attendance records exist")
    void deleteBlockedWhenAttendanceExists() {
        AttendanceSession stored = existing();
        when(sessions.findById(SESSION_ID)).thenReturn(Optional.of(stored));
        when(records.countByAttendanceSessionAttendanceSessionId(SESSION_ID)).thenReturn(12L);

        ResourceConflictException failure = assertThrows(
                ResourceConflictException.class,
                () -> service.deleteSession(SESSION_ID));

        assertTrue(failure.getMessage().contains("attendance records exist"));
        verify(sessions, never()).delete(any(AttendanceSession.class));
    }

    @Test
    @DisplayName("Delete reports not found for an unknown session")
    void deleteUnknownSession() {
        when(sessions.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteSession(404L));

        verify(sessions, never()).delete(any(AttendanceSession.class));
    }

    @Test
    @DisplayName("Delete refuses a caller outside the course's scope")
    void deleteRefusesOutOfScopeCaller() {
        AttendanceSession stored = existing();
        when(sessions.findById(SESSION_ID)).thenReturn(Optional.of(stored));
        org.mockito.Mockito.doThrow(new AccessDeniedException("not assigned"))
                .when(accessScope).requireCourseAccess(COURSE_ID);

        assertThrows(AccessDeniedException.class, () -> service.deleteSession(SESSION_ID));

        verify(sessions, never()).delete(any(AttendanceSession.class));
    }

    @Test
    @DisplayName("Dependency guards fail closed when the record repository is unavailable")
    void guardsFailClosedWithoutRecordRepository() {
        AttendanceSession stored = existing();
        when(sessions.findById(SESSION_ID)).thenReturn(Optional.of(stored));

        AttendanceSessionService unwired = new AttendanceSessionService(
                sessions, mock(CourseRepository.class), mock(FacultyRepository.class),
                mock(AccessScope.class), null);

        ResourceConflictException failure = assertThrows(
                ResourceConflictException.class, () -> unwired.deleteSession(SESSION_ID));

        assertTrue(failure.getMessage().contains("cannot be verified"));
        verify(sessions, never()).delete(any(AttendanceSession.class));
    }

    @Test
    @DisplayName("Scope guards fail closed when the scope service is unwired")
    void scopeFailsClosedWithoutAccessScope() {
        AttendanceSession stored = existing();
        when(sessions.findById(SESSION_ID)).thenReturn(Optional.of(stored));

        AttendanceSessionService unwired = new AttendanceSessionService(
                sessions, mock(CourseRepository.class), mock(FacultyRepository.class),
                null, records);

        assertThrows(AccessDeniedException.class,
                () -> unwired.updateSession(
                        SESSION_ID, new AttendanceSessionRequest(LocalDate.of(2026, 2, 9), "Matrices")));
        assertThrows(AccessDeniedException.class, () -> unwired.deleteSession(SESSION_ID));

        verify(sessions, never()).delete(any(AttendanceSession.class));
    }
}
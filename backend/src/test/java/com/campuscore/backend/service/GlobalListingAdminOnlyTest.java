package com.campuscore.backend.service;

import com.campuscore.backend.entity.Student;
import com.campuscore.backend.repository.AssessmentRepository;
import com.campuscore.backend.repository.AttendanceRecordRepository;
import com.campuscore.backend.repository.AttendanceSessionRepository;
import com.campuscore.backend.repository.CourseRepository;
import com.campuscore.backend.repository.EnrollmentRepository;
import com.campuscore.backend.repository.FacultyCourseAssignmentRepository;
import com.campuscore.backend.repository.FacultyRepository;
import com.campuscore.backend.repository.MarkRepository;
import com.campuscore.backend.repository.StudentRepository;
import com.campuscore.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The institution-wide listings must stay administrator-only.
 *
 * <p>These getters are not scope filtered. Before this guard the role matrix
 * shared them with FACULTY and, for marks and attendance records, with STUDENT,
 * which meant any signed-in student could read every mark and attendance record
 * in the institution. The guard lives in the service as well as the request
 * matcher so a future route cannot re-expose them.
 */
class GlobalListingAdminOnlyTest {

    private AssessmentRepository assessments = mock(AssessmentRepository.class);
    private MarkRepository marks = mock(MarkRepository.class);
    private AttendanceSessionRepository sessions = mock(AttendanceSessionRepository.class);
    private AttendanceRecordRepository records = mock(AttendanceRecordRepository.class);
    private CourseRepository courses = mock(CourseRepository.class);
    private EnrollmentRepository enrollments = mock(EnrollmentRepository.class);

    private AccessScope scope;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "account", "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private AccessScope scope() {
        UserRepository users = mock(UserRepository.class);
        StudentRepository students = mock(StudentRepository.class);
        FacultyRepository faculty = mock(FacultyRepository.class);
        FacultyCourseAssignmentRepository assignments = mock(FacultyCourseAssignmentRepository.class);
        enrollments = mock(EnrollmentRepository.class);
        scope = new AccessScope(users, students, faculty, assignments, enrollments);
        return scope;
    }

    private AssessmentService assessmentService() {
        return new AssessmentService(assessments, courses, scope());
    }

    private MarkService markService() {
        return new MarkService(marks, enrollments, assessments, scope());
    }

    private AttendanceSessionService sessionService() {
        return new AttendanceSessionService(sessions, courses, mock(FacultyRepository.class), scope());
    }

    private AttendanceRecordService recordService() {
        return new AttendanceRecordService(records, sessions, enrollments, scope());
    }

    // ------------------------------------------------------------------
    // Faculty and students are refused
    // ------------------------------------------------------------------

    @Test
    void facultyCannotReadTheGlobalAssessmentListing() {
        authenticate("FACULTY");

        assertThrows(AccessDeniedException.class, () -> assessmentService().getAllAssessments());
        assertThrows(AccessDeniedException.class,
                () -> assessmentService().getAssessmentsPage(PageRequest.of(0, 10)));

        verify(assessments, never()).findAll();
    }

    @Test
    void facultyCannotReadTheGlobalMarkListing() {
        authenticate("FACULTY");

        assertThrows(AccessDeniedException.class, () -> markService().getAllMarks());
        assertThrows(AccessDeniedException.class, () -> markService().getMarksPage(PageRequest.of(0, 10)));

        verify(marks, never()).findAll();
    }

    @Test
    void aStudentCannotReadTheGlobalMarkListing() {
        // The concrete disclosure this guard prevents: a student reading every
        // mark in the institution.
        authenticate("STUDENT");

        assertThrows(AccessDeniedException.class, () -> markService().getAllMarks());

        verify(marks, never()).findAll();
    }

    @Test
    void aStudentCannotReadTheGlobalAttendanceRecordListing() {
        authenticate("STUDENT");

        assertThrows(AccessDeniedException.class, () -> recordService().getAllRecords());

        verify(records, never()).findAll();
    }

    @Test
    void facultyCannotReadTheGlobalAttendanceListings() {
        authenticate("FACULTY");

        assertThrows(AccessDeniedException.class, () -> sessionService().getAllSessions());
        assertThrows(AccessDeniedException.class, () -> recordService().getAllRecords());

        verify(sessions, never()).findAll();
        verify(records, never()).findAll();
    }

    @Test
    void anonymousCannotReadTheGlobalListings() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken(
                        "key", "anonymous",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThrows(AccessDeniedException.class, () -> assessmentService().getAllAssessments());
        assertThrows(AccessDeniedException.class, () -> markService().getAllMarks());
    }

    // ------------------------------------------------------------------
    // Administrators keep global read
    // ------------------------------------------------------------------

    @Test
    void administratorsKeepGlobalRead() {
        authenticate("ADMIN");

        when(assessments.findAll()).thenReturn(List.of());
        when(marks.findAll()).thenReturn(List.of());
        when(sessions.findAll()).thenReturn(List.of());
        when(records.findAll()).thenReturn(List.of());

        assertTrue(assessmentService().getAllAssessments().isEmpty());
        assertTrue(markService().getAllMarks().isEmpty());
        assertTrue(sessionService().getAllSessions().isEmpty());
        assertTrue(recordService().getAllRecords().isEmpty());
    }

    @Test
    void theListingFailsClosedWhenScopeIsUnavailable() {
        SecurityContextHolder.clearContext();

        assertThrows(AccessDeniedException.class,
                () -> new AssessmentService(assessments, courses).getAllAssessments());
        assertThrows(AccessDeniedException.class,
                () -> new MarkService(marks, enrollments, assessments).getAllMarks());
    }

    // ------------------------------------------------------------------
    // Faculty enrollments by course are course-scoped
    // ------------------------------------------------------------------

    @Test
    void facultyMayReadEnrollmentsOfAnAssignedCourse() {
        authenticate("FACULTY");

        UserRepository users = scopeUsers();
        StudentRepository students = mock(StudentRepository.class);
        FacultyRepository faculty = mock(FacultyRepository.class);
        FacultyCourseAssignmentRepository assignments = mock(FacultyCourseAssignmentRepository.class);
        EnrollmentRepository enrollmentRepository = mock(EnrollmentRepository.class);

        com.campuscore.backend.entity.Faculty me = new com.campuscore.backend.entity.Faculty();
        me.setFacultyId(21L);
        when(faculty.findByUserUserId(1L)).thenReturn(Optional.of(me));
        when(assignments.existsByCourseCourseIdAndFacultyFacultyIdAndEndedAtIsNull(11L, 21L))
                .thenReturn(true);
        when(enrollmentRepository.findByCourseCourseId(11L)).thenReturn(List.of());

        EnrollmentService service = new EnrollmentService(
                enrollmentRepository, students, courses,
                new AccessScope(users, students, faculty, assignments, enrollmentRepository));

        assertTrue(service.getEnrollmentsByCourse(11L).isEmpty());
    }

    @Test
    void facultyAreRefusedEnrollmentsOfAnUnassignedCourse() {
        authenticate("FACULTY");

        UserRepository users = scopeUsers();
        StudentRepository students = mock(StudentRepository.class);
        FacultyRepository faculty = mock(FacultyRepository.class);
        FacultyCourseAssignmentRepository assignments = mock(FacultyCourseAssignmentRepository.class);
        EnrollmentRepository enrollmentRepository = mock(EnrollmentRepository.class);

        com.campuscore.backend.entity.Faculty me = new com.campuscore.backend.entity.Faculty();
        me.setFacultyId(21L);
        when(faculty.findByUserUserId(1L)).thenReturn(Optional.of(me));
        when(assignments.existsByCourseCourseIdAndFacultyFacultyIdAndEndedAtIsNull(99L, 21L))
                .thenReturn(false);

        EnrollmentService service = new EnrollmentService(
                enrollmentRepository, students, courses,
                new AccessScope(users, students, faculty, assignments, enrollmentRepository));

        assertThrows(AccessDeniedException.class, () -> service.getEnrollmentsByCourse(99L));
        verify(enrollmentRepository, never()).findByCourseCourseId(anyLong());
    }

    @Test
    void aStudentCannotListEnrollmentsByCourse() {
        authenticate("STUDENT");

        UserRepository users = scopeUsers();
        StudentRepository students = mock(StudentRepository.class);
        EnrollmentRepository enrollmentRepository = mock(EnrollmentRepository.class);

        Student me = new Student();
        me.setStudentId(7L);
        when(students.findByUserUserId(1L)).thenReturn(Optional.of(me));

        EnrollmentService service = new EnrollmentService(
                enrollmentRepository, students, courses,
                new AccessScope(users, students, mock(FacultyRepository.class),
                        mock(FacultyCourseAssignmentRepository.class), enrollmentRepository));

        assertThrows(AccessDeniedException.class, () -> service.getEnrollmentsByCourse(11L));
    }

    private UserRepository scopeUsers() {
        UserRepository users = mock(UserRepository.class);
        com.campuscore.backend.entity.User account = new com.campuscore.backend.entity.User();
        account.setUserId(1L);
        account.setUsername("account");
        when(users.findByUsername(any())).thenReturn(Optional.of(account));
        return users;
    }


}

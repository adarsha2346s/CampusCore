package com.campuscore.backend.service;

import com.campuscore.backend.entity.Course;
import com.campuscore.backend.entity.Enrollment;
import com.campuscore.backend.entity.Faculty;
import com.campuscore.backend.entity.FacultyCourseAssignment;
import com.campuscore.backend.entity.Student;
import com.campuscore.backend.entity.User;
import com.campuscore.backend.exception.ResourceNotFoundException;
import com.campuscore.backend.repository.EnrollmentRepository;
import com.campuscore.backend.repository.FacultyCourseAssignmentRepository;
import com.campuscore.backend.repository.FacultyRepository;
import com.campuscore.backend.repository.StudentRepository;
import com.campuscore.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ownership and scope matrix.
 *
 * <p>Covers the IDOR shapes Phase 2 is meant to close: one faculty member
 * reaching another's records, a faculty member reaching an unrelated course, and
 * a student reaching another student.
 */
class AccessScopeTest {

    private static final Long COURSE_ID = 11L;
    private static final Long OTHER_COURSE_ID = 99L;
    private static final Long FACULTY_ID = 21L;
    private static final Long STUDENT_ID = 7L;
    private static final Long ENROLLMENT_ID = 3000L;

    private UserRepository users;
    private StudentRepository students;
    private FacultyRepository faculty;
    private FacultyCourseAssignmentRepository assignments;
    private EnrollmentRepository enrollments;

    private AccessScope scope;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        students = mock(StudentRepository.class);
        faculty = mock(FacultyRepository.class);
        assignments = mock(FacultyCourseAssignmentRepository.class);
        enrollments = mock(EnrollmentRepository.class);

        scope = new AccessScope(users, students, faculty, assignments, enrollments);

        User user = new User();
        user.setUserId(1L);
        when(users.findByUsername(any())).thenReturn(Optional.of(user));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        username, "n/a", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private void asFacultyAssignedTo(Long courseId) {
        authenticate("faculty-account", "FACULTY");

        Faculty me = new Faculty();
        me.setFacultyId(FACULTY_ID);
        when(faculty.findByUserUserId(1L)).thenReturn(Optional.of(me));
        when(assignments
                .existsByCourseCourseIdAndFacultyFacultyIdAndEndedAtIsNull(courseId, FACULTY_ID))
                .thenReturn(courseId.equals(COURSE_ID));
    }

    private Enrollment enrollment(Long studentId, Long courseId) {
        Student owner = new Student();
        owner.setStudentId(studentId);
        Course course = new Course();
        course.setCourseId(courseId);

        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollmentId(ENROLLMENT_ID);
        enrollment.setStudent(owner);
        enrollment.setCourse(course);
        return enrollment;
    }

    // ------------------------------------------------------------------
    // Identity
    // ------------------------------------------------------------------

    @Test
    void anonymousRequestsAreNotAuthenticated() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken(
                        "key", "anonymous",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertFalse(scope.isAuthenticated());
        assertFalse(scope.isAdmin());
        assertFalse(scope.isFaculty());
        assertFalse(scope.isStudent());
    }

    @Test
    void identityComesFromTheSecurityContextNotFromParameters() {
        authenticate("admin-account", "ADMIN");

        assertTrue(scope.isAdmin());
        assertFalse(scope.isFaculty());
        assertFalse(scope.isStudent());
        assertEquals("admin-account", scope.currentAuthentication().getName());
    }

    // ------------------------------------------------------------------
    // Course scope
    // ------------------------------------------------------------------

    @Test
    void adminReachesAnyCourse() {
        authenticate("admin-account", "ADMIN");

        scope.requireCourseAccess(COURSE_ID);
        scope.requireCourseAccess(OTHER_COURSE_ID);
    }

    @Test
    void facultyReachesAnAssignedCourse() {
        asFacultyAssignedTo(COURSE_ID);

        scope.requireCourseAccess(COURSE_ID);
    }

    @Test
    void facultyIsDeniedAnUnassignedCourse() {
        asFacultyAssignedTo(COURSE_ID);

        assertThrows(AccessDeniedException.class, () -> scope.requireCourseAccess(OTHER_COURSE_ID));
    }

    @Test
    void aClosedAssignmentDoesNotGrantCourseAccess() {
        authenticate("faculty-account", "FACULTY");

        Faculty me = new Faculty();
        me.setFacultyId(FACULTY_ID);
        when(faculty.findByUserUserId(1L)).thenReturn(Optional.of(me));

        // existsBy...AndEndedAtIsNull returns false for a closed assignment.
        when(assignments
                .existsByCourseCourseIdAndFacultyFacultyIdAndEndedAtIsNull(COURSE_ID, FACULTY_ID))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> scope.requireCourseAccess(COURSE_ID));
    }

    @Test
    void studentIsDeniedEveryCourse() {
        authenticate("student-account", "STUDENT");

        assertThrows(AccessDeniedException.class, () -> scope.requireCourseAccess(COURSE_ID));
    }

    // ------------------------------------------------------------------
    // Enrollment scope (marks, attendance, GPA)
    // ------------------------------------------------------------------

    @Test
    void studentReachesOnlyTheirOwnEnrollment() {
        authenticate("student-account", "STUDENT");

        Student me = new Student();
        me.setStudentId(STUDENT_ID);
        when(students.findByUserUserId(1L)).thenReturn(Optional.of(me));
        when(enrollments.existsByStudentStudentId(STUDENT_ID, ENROLLMENT_ID)).thenReturn(true);
        when(enrollments.findById(ENROLLMENT_ID)).thenReturn(Optional.of(enrollment(STUDENT_ID, COURSE_ID)));

        scope.requireEnrollmentAccess(ENROLLMENT_ID);
    }

    @Test
    void studentIsDeniedAnotherStudentsEnrollment() {
        authenticate("student-account", "STUDENT");

        Student me = new Student();
        me.setStudentId(STUDENT_ID);
        when(students.findByUserUserId(1L)).thenReturn(Optional.of(me));
        when(enrollments.existsByStudentStudentId(STUDENT_ID, ENROLLMENT_ID)).thenReturn(false);
        when(enrollments.findById(ENROLLMENT_ID)).thenReturn(Optional.of(enrollment(900L, COURSE_ID)));

        assertThrows(AccessDeniedException.class, () -> scope.requireEnrollmentAccess(ENROLLMENT_ID));
    }

    @Test
    void facultyReachesEnrollmentsOnlyInAssignedCourses() {
        when(enrollments.findById(ENROLLMENT_ID))
                .thenReturn(Optional.of(enrollment(900L, OTHER_COURSE_ID)));

        asFacultyAssignedTo(COURSE_ID);

        assertThrows(AccessDeniedException.class, () -> scope.requireEnrollmentAccess(ENROLLMENT_ID));

        when(enrollments.findById(ENROLLMENT_ID))
                .thenReturn(Optional.of(enrollment(900L, COURSE_ID)));

        scope.requireEnrollmentAccess(ENROLLMENT_ID);
    }

    @Test
    void adminReachesAnyEnrollment() {
        authenticate("admin-account", "ADMIN");

        scope.requireEnrollmentAccess(ENROLLMENT_ID);

        verify(enrollments, never()).findById(anyLong());
    }

    @Test
    void unknownEnrollmentIsNotFoundRatherThanForbidden() {
        authenticate("student-account", "STUDENT");
        when(enrollments.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> scope.requireEnrollmentAccess(404L));
    }

    // ------------------------------------------------------------------
    // Student scope
    // ------------------------------------------------------------------

    @Test
    void studentReachesTheirOwnRecord() {
        authenticate("student-account", "STUDENT");

        Student me = new Student();
        me.setStudentId(STUDENT_ID);
        when(students.findByUserUserId(1L)).thenReturn(Optional.of(me));

        scope.requireStudentAccess(STUDENT_ID);
    }

    @Test
    void studentIsDeniedAnotherStudentRecord() {
        authenticate("student-account", "STUDENT");

        Student me = new Student();
        me.setStudentId(STUDENT_ID);
        when(students.findByUserUserId(1L)).thenReturn(Optional.of(me));

        assertThrows(AccessDeniedException.class, () -> scope.requireStudentAccess(900L));
    }

    @Test
    void facultyReachesOnlyStudentsTheyTeach() {
        asFacultyAssignedTo(COURSE_ID);

        when(assignments.findByFacultyFacultyIdAndEndedAtIsNullOrderByAssignedAtDesc(FACULTY_ID))
                .thenReturn(List.of(assignment(COURSE_ID)));
        when(enrollments.existsByStudentStudentIdAndCourseCourseIdIn(900L, List.of(COURSE_ID)))
                .thenReturn(true);

        scope.requireStudentAccess(900L);

        when(enrollments.existsByStudentStudentIdAndCourseCourseIdIn(901L, List.of(COURSE_ID)))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> scope.requireStudentAccess(901L));
    }

    @Test
    void facultyWithNoAssignmentsReachesNoStudents() {
        asFacultyAssignedTo(OTHER_COURSE_ID);

        when(assignments.findByFacultyFacultyIdAndEndedAtIsNullOrderByAssignedAtDesc(FACULTY_ID))
                .thenReturn(List.of());

        assertThrows(AccessDeniedException.class, () -> scope.requireStudentAccess(900L));
        verify(enrollments, never())
                .existsByStudentStudentIdAndCourseCourseIdIn(anyLong(), any());
    }

    @Test
    void adminReachesAnyStudent() {
        authenticate("admin-account", "ADMIN");

        scope.requireStudentAccess(900L);
    }

    @Test
    void denialDoesNotRevealWhetherTheStudentExists() {
        authenticate("student-account", "STUDENT");

        Student me = new Student();
        me.setStudentId(STUDENT_ID);
        when(students.findByUserUserId(1L)).thenReturn(Optional.of(me));

        AccessDeniedException failure =
                assertThrows(AccessDeniedException.class, () -> scope.requireStudentAccess(4242L));

        assertFalse(failure.getMessage().contains("404"));
        assertFalse(failure.getMessage().contains("not found"));
    }

    // ------------------------------------------------------------------
    // Assigned course listing
    // ------------------------------------------------------------------

    @Test
    void assignedCourseIdsAreEmptyForNonFaculty() {
        authenticate("admin-account", "ADMIN");

        assertTrue(scope.assignedCourseIds().isEmpty());
    }

    @Test
    void assignedCourseIdsComeFromCurrentAssignmentsOnly() {
        asFacultyAssignedTo(COURSE_ID);

        when(assignments.findByFacultyFacultyIdAndEndedAtIsNullOrderByAssignedAtDesc(FACULTY_ID))
                .thenReturn(List.of(assignment(COURSE_ID)));

        assertEquals(List.of(COURSE_ID), scope.assignedCourseIds());
    }

    private FacultyCourseAssignment assignment(Long courseId) {
        Course course = new Course();
        course.setCourseId(courseId);

        FacultyCourseAssignment assignment = new FacultyCourseAssignment();
        assignment.setCourse(course);
        return assignment;
    }
}

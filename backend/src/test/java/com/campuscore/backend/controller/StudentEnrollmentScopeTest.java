package com.campuscore.backend.controller;

import com.campuscore.backend.entity.Course;
import com.campuscore.backend.dto.EnrollmentResponse;
import com.campuscore.backend.entity.Enrollment;
import com.campuscore.backend.entity.Faculty;
import com.campuscore.backend.entity.FacultyCourseAssignment;
import com.campuscore.backend.entity.Student;
import com.campuscore.backend.entity.User;
import com.campuscore.backend.repository.EnrollmentRepository;
import com.campuscore.backend.repository.FacultyCourseAssignmentRepository;
import com.campuscore.backend.repository.FacultyRepository;
import com.campuscore.backend.repository.StudentRepository;
import com.campuscore.backend.repository.UserRepository;
import com.campuscore.backend.service.AccessScope;
import com.campuscore.backend.service.AuditLogService;
import com.campuscore.backend.service.StudentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The enrollments of a student, as seen by the caller.
 *
 * <p>This is the route the student 360 drawer reads. It exists because the
 * alternatives are administrator-only collections, which a faculty member
 * cannot call, and because a teacher must not receive the courses they are not
 * assigned to teach.
 *
 * <p>Two gates therefore apply, and both are checked here. The ownership gate
 * refuses a caller who does not teach the student at all. The scoping gate then
 * narrows the answer to the caller's own courses, so the response never carries
 * the student's other courses.
 */
class StudentEnrollmentScopeTest {

    private static final Long STUDENT_ID = 51L;
    private static final Long FACULTY_ID = 41L;
    private static final Long TAUGHT_COURSE_ID = 31L;
    private static final Long OTHER_COURSE_ID = 32L;

    private EnrollmentRepository enrollments;
    private StudentRepository students;
    private UserRepository users;
    private FacultyRepository faculty;
    private FacultyCourseAssignmentRepository assignments;
    private StudentService service;
    private AccessScope accessScope;
    private StudentController controller;

    @BeforeEach
    void setUp() {
        enrollments = mock(EnrollmentRepository.class);
        students = mock(StudentRepository.class);
        users = mock(UserRepository.class);
        faculty = mock(FacultyRepository.class);
        assignments = mock(FacultyCourseAssignmentRepository.class);

        accessScope = new AccessScope(users, students, faculty, assignments, enrollments);

        service = new StudentService(
                students,
                users,
                mock(com.campuscore.backend.repository.DepartmentRepository.class),
                enrollments,
                accessScope, mock(AuditLogService.class));

        controller = new StudentController(service, students, users, accessScope);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------
    // Administrator
    // ------------------------------------------------------------------

    @Test
    @DisplayName("an administrator receives every enrollment the student holds")
    void adminReceivesEveryEnrollment() {
        asAdmin();
        when(enrollments.findByStudentStudentId(STUDENT_ID))
                .thenReturn(List.of(enrollment(TAUGHT_COURSE_ID), enrollment(OTHER_COURSE_ID)));

        List<EnrollmentResponse> result = controller.getStudentEnrollments(STUDENT_ID).getBody();

        assertEquals(2, result.size());
        verify(enrollments).findByStudentStudentId(STUDENT_ID);
    }

    // ------------------------------------------------------------------
    // Faculty
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a faculty member receives only the enrollments in their own courses")
    void facultyReceivesOnlyTheirOwnCourses() {
        asFacultyAssignedTo(TAUGHT_COURSE_ID);

        when(enrollments.findByStudentStudentIdAndCourseCourseIdIn(
                eq(STUDENT_ID), eq(List.of(TAUGHT_COURSE_ID))))
                .thenReturn(List.of(enrollment(TAUGHT_COURSE_ID)));

        List<EnrollmentResponse> result = controller.getStudentEnrollments(STUDENT_ID).getBody();

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("a faculty member never receives the unscoped list of the student's enrollments")
    void facultyNeverReceivesTheUnscopedList() {
        asFacultyAssignedTo(TAUGHT_COURSE_ID);

        controller.getStudentEnrollments(STUDENT_ID).getBody();

        verify(enrollments, never()).findByStudentStudentId(STUDENT_ID);
        verify(enrollments).findByStudentStudentIdAndCourseCourseIdIn(
                eq(STUDENT_ID), eq(List.of(TAUGHT_COURSE_ID)));
    }

    @Test
    @DisplayName("a faculty member without an assignment is refused")
    void facultyWithoutAssignmentsIsRefused() {
        asFacultyWithoutAssignments();

        // A faculty member who teaches nothing does not teach this student
        // either, so the ownership gate refuses the request before the scoping
        // gate is reached. Nothing is read from the database.
        assertThrows(
                AccessDeniedException.class,
                () -> controller.getStudentEnrollments(STUDENT_ID));

        verify(enrollments, never()).findByStudentStudentId(anyLong());
        verify(enrollments, never()).findByStudentStudentIdAndCourseCourseIdIn(anyLong(), any());
    }

    @Test
    @DisplayName("a faculty member who does not teach the student is refused")
    void facultyOutsideTheScopeIsRefused() {
        asFacultyAssignedTo(TAUGHT_COURSE_ID);
        // The student is not enrolled in any course this faculty member teaches.
        when(enrollments.existsByStudentStudentIdAndCourseCourseIdIn(
                eq(STUDENT_ID), any())).thenReturn(false);

        assertThrows(
                AccessDeniedException.class,
                () -> controller.getStudentEnrollments(STUDENT_ID).getBody());

        verify(enrollments, never()).findByStudentStudentId(anyLong());
    }

    @Test
    @DisplayName("a student cannot use the route to list their own enrollments")
    void studentCannotUseTheRoute() {
        User account = new User();
        account.setUserId(1L);
        account.setUsername("student-account");
        when(users.findByUsername("student-account")).thenReturn(Optional.of(account));

        Student me = new Student();
        me.setStudentId(STUDENT_ID);
        when(students.findByUserUserId(1L)).thenReturn(Optional.of(me));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "student-account", "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));

        // The route is administrator and faculty only, so the service must fail
        // closed rather than hand back the student's records.
        List<EnrollmentResponse> result = controller.getStudentEnrollments(STUDENT_ID).getBody();

        assertTrue(result.isEmpty());
        verify(enrollments, never()).findByStudentStudentId(anyLong());
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private void asAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin-account", "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private void asFacultyAssignedTo(long courseId) {
        User account = new User();
        account.setUserId(1L);
        account.setUsername("faculty-account");
        when(users.findByUsername("faculty-account")).thenReturn(Optional.of(account));

        Faculty me = new Faculty();
        me.setFacultyId(FACULTY_ID);
        when(faculty.findByUserUserId(1L)).thenReturn(Optional.of(me));

        when(assignments.findByFacultyFacultyIdAndEndedAtIsNullOrderByAssignedAtDesc(FACULTY_ID))
                .thenReturn(List.of(assignment(courseId)));

        when(enrollments.existsByStudentStudentIdAndCourseCourseIdIn(
                eq(STUDENT_ID), any())).thenReturn(true);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "faculty-account", "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_FACULTY"))));
    }

    private void asFacultyWithoutAssignments() {
        User account = new User();
        account.setUserId(1L);
        account.setUsername("faculty-account");
        when(users.findByUsername("faculty-account")).thenReturn(Optional.of(account));

        Faculty me = new Faculty();
        me.setFacultyId(FACULTY_ID);
        when(faculty.findByUserUserId(1L)).thenReturn(Optional.of(me));

        when(assignments.findByFacultyFacultyIdAndEndedAtIsNullOrderByAssignedAtDesc(FACULTY_ID))
                .thenReturn(List.of());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "faculty-account", "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_FACULTY"))));
    }

    private FacultyCourseAssignment assignment(long courseId) {
        Course course = new Course();
        course.setCourseId(courseId);
        course.setCourseCode("CSE" + courseId);

        Faculty me = new Faculty();
        me.setFacultyId(FACULTY_ID);

        FacultyCourseAssignment assignment = new FacultyCourseAssignment();
        assignment.setCourse(course);
        assignment.setFaculty(me);
        return assignment;
    }

    private Enrollment enrollment(long courseId) {
        Course course = new Course();
        course.setCourseId(courseId);
        course.setCourseCode("CSE" + courseId);
        course.setCourseName("Course " + courseId);
        course.setCredits(3);

        Student owner = new Student();
        owner.setStudentId(STUDENT_ID);
        owner.setEnrollmentNumber("DEMO-2026-001");
        owner.setFirstName("Avery");
        owner.setLastName("Chen");

        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollmentId(3000L + courseId);
        enrollment.setStudent(owner);
        enrollment.setCourse(course);
        enrollment.setSemester("Fall");
        enrollment.setAcademicYear("2026-2027");
        enrollment.setEnrollmentDate(LocalDate.of(2026, 8, 10));
        enrollment.setStatus(Enrollment.Status.ENROLLED);
        return enrollment;
    }
}

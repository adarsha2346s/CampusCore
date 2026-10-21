package com.campuscore.backend.controller;

import com.campuscore.backend.repository.EnrollmentRepository;
import com.campuscore.backend.repository.FacultyCourseAssignmentRepository;
import com.campuscore.backend.repository.FacultyRepository;
import com.campuscore.backend.repository.StudentRepository;
import com.campuscore.backend.repository.UserRepository;
import com.campuscore.backend.service.AccessScope;
import com.campuscore.backend.service.GpaCalculationService;
import com.campuscore.backend.service.StudentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Controller contract for student and GPA routes.
 *
 * <p>Ownership itself is decided by {@link AccessScope}, which is covered by
 * AccessScopeTest. These tests prove the controllers delegate to it and do not
 * fall back to their own ad-hoc checks.
 */
class StudentOwnershipTest {

    private StudentService studentService;
    private GpaCalculationService calculation;
    private EnrollmentRepository enrollments;
    private StudentRepository students;
    private UserRepository users;

    @BeforeEach
    void setUp() {
        studentService = mock(StudentService.class);
        calculation = mock(GpaCalculationService.class);
        students = mock(StudentRepository.class);
        users = mock(UserRepository.class);
        enrollments = mock(EnrollmentRepository.class);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private AccessScope accessScope() {
        return new AccessScope(
                users,
                students,
                mock(FacultyRepository.class),
                mock(FacultyCourseAssignmentRepository.class),
                enrollments);
    }

    @Test
    void studentCannotReadAnotherStudentRecord() {
        StudentController controller =
                new StudentController(studentService, students, users, accessScope());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "student-account", "", List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));

        assertThrows(
                AccessDeniedException.class,
                () -> controller.getStudentById(900L));

        verify(studentService, never()).getStudentById(900L);
    }

    @Test
    void nonStudentCannotUseStudentSelfEndpoint() {
        StudentController controller = new StudentController(studentService, students, users);
        var authentication = new UsernamePasswordAuthenticationToken(
                "admin-account", "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        var response = controller.getMyStudentProfile(authentication);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(studentService, never()).getStudentForUsername("admin-account");
    }

    @Test
    void studentCannotCalculateGpaForAnotherStudentsEnrollment() {
        GpaController controller = new GpaController(
                calculation, enrollments, students, users, accessScope());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "student-account", "", List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));

        com.campuscore.backend.entity.Student someoneElse =
                new com.campuscore.backend.entity.Student();
        someoneElse.setStudentId(900L);

        com.campuscore.backend.entity.Course course =
                new com.campuscore.backend.entity.Course();
        course.setCourseId(11L);

        var other = new com.campuscore.backend.entity.Enrollment();
        other.setStudent(someoneElse);
        other.setCourse(course);
        when(enrollments.findById(3000L)).thenReturn(Optional.of(other));

        assertThrows(
                AccessDeniedException.class,
                () -> controller.getCourseGpa(3000L, "DEFAULT_10_POINT"));

        verify(calculation, never()).calculateCourseGpa(3000L, "DEFAULT_10_POINT");
    }

    @Test
    void studentCannotReadAnotherStudentsDashboard() {
        StudentDashboardController controller = new StudentDashboardController(
                mock(com.campuscore.backend.service.StudentDashboardService.class),
                users, students, accessScope());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "student-account", "", List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));

        assertThrows(
                AccessDeniedException.class,
                () -> controller.getStudentDashboard(900L));
    }
}

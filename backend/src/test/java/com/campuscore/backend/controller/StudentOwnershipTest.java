package com.campuscore.backend.controller;

import com.campuscore.backend.entity.Enrollment;
import com.campuscore.backend.entity.Student;
import com.campuscore.backend.entity.User;
import com.campuscore.backend.repository.EnrollmentRepository;
import com.campuscore.backend.repository.StudentRepository;
import com.campuscore.backend.repository.UserRepository;
import com.campuscore.backend.service.GpaCalculationService;
import com.campuscore.backend.service.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentOwnershipTest {

    @Test
    void studentCannotReadAnotherStudentRecord() {
        StudentService service = mock(StudentService.class);
        StudentRepository students = mock(StudentRepository.class);
        UserRepository users = mock(UserRepository.class);
        StudentController controller = new StudentController(service, students, users);
        Student requested = new Student();
        requested.setStudentId(900L);
        Student own = new Student();
        own.setStudentId(42L);
        User user = new User();
        user.setUserId(7L);
        when(service.getStudentById(900L)).thenReturn(requested);
        when(users.findByUsername("student-account")).thenReturn(Optional.of(user));
        when(students.findByUserUserId(7L)).thenReturn(Optional.of(own));

        var response = controller.getStudentById(900L, studentAuthentication());

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void nonStudentCannotUseStudentSelfEndpoint() {
        StudentService service = mock(StudentService.class);
        StudentController controller = new StudentController(
                service, mock(StudentRepository.class), mock(UserRepository.class));
        var authentication = new UsernamePasswordAuthenticationToken(
                "admin-account", "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        var response = controller.getMyStudentProfile(authentication);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(service, never()).getStudentForUsername("admin-account");
    }

    @Test
    void studentCannotCalculateGpaForAnotherStudentsEnrollment() {
        GpaCalculationService calculation = mock(GpaCalculationService.class);
        EnrollmentRepository enrollments = mock(EnrollmentRepository.class);
        StudentRepository students = mock(StudentRepository.class);
        UserRepository users = mock(UserRepository.class);
        GpaController controller = new GpaController(calculation, enrollments, students, users);
        User user = new User();
        user.setUserId(7L);
        Student own = new Student();
        own.setStudentId(42L);
        Student other = new Student();
        other.setStudentId(900L);
        Enrollment otherEnrollment = new Enrollment();
        otherEnrollment.setStudent(other);
        when(users.findByUsername("student-account")).thenReturn(Optional.of(user));
        when(students.findByUserUserId(7L)).thenReturn(Optional.of(own));
        when(enrollments.findById(3000L)).thenReturn(Optional.of(otherEnrollment));

        var response = controller.getCourseGpa(3000L, "DEFAULT_10_POINT", studentAuthentication());

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(calculation, never()).calculateCourseGpa(3000L, "DEFAULT_10_POINT");
    }

    private UsernamePasswordAuthenticationToken studentAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                "student-account", "", List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
    }
}

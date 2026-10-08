package com.campuscore.backend.service;

import com.campuscore.backend.dto.UserRequest;
import com.campuscore.backend.entity.Course;
import com.campuscore.backend.entity.Department;
import com.campuscore.backend.entity.User;
import com.campuscore.backend.exception.ResourceConflictException;
import com.campuscore.backend.exception.ResourceNotFoundException;
import com.campuscore.backend.repository.CourseRepository;
import com.campuscore.backend.repository.DepartmentRepository;
import com.campuscore.backend.repository.FacultyRepository;
import com.campuscore.backend.repository.StudentRepository;
import com.campuscore.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Lifecycle behaviour for the records that already carry a status column, plus
 * the status codes returned for the user endpoints.
 */
class LifecycleStatusMappingTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void editingACourseDoesNotReactivateIt() {
        CourseRepository courses = mock(CourseRepository.class);

        Course stored = new Course();
        stored.setCourseId(11L);
        stored.setCourseCode("CS101");
        stored.setCourseName("Intro");
        stored.setCredits(3);
        stored.setCapacity(30);
        stored.setStatus(Course.Status.INACTIVE);
        when(courses.findById(11L)).thenReturn(Optional.of(stored));
        when(courses.save(any(Course.class))).thenAnswer(call -> call.getArgument(0));

        // Mirrors CourseController.updateCourse: the payload carries no status.
        Course incoming = new Course();
        incoming.setDepartment(new Department());
        incoming.setCourseCode("CS101");
        incoming.setCourseName("Intro to Programming");
        incoming.setCredits(4);
        incoming.setCapacity(40);

        Course updated = new CourseService(courses).updateCourse(11L, incoming);

        assertSame(Course.Status.INACTIVE, updated.getStatus());
        assertEquals("Intro to Programming", updated.getCourseName());
    }

    @Test
    void deactivationStillUsesTheExistingStatusColumn() {
        CourseRepository courses = mock(CourseRepository.class);

        Course stored = new Course();
        stored.setCourseId(11L);
        stored.setStatus(Course.Status.ACTIVE);
        when(courses.findById(11L)).thenReturn(Optional.of(stored));
        when(courses.save(any(Course.class))).thenAnswer(call -> call.getArgument(0));

        CourseService service = new CourseService(courses);

        assertSame(Course.Status.INACTIVE, service.deactivateCourse(11L).getStatus());
    }

    @Test
    void unknownUserIsNotFound() {
        UserRepository users = mock(UserRepository.class);
        when(users.findById(99L)).thenReturn(Optional.empty());

        UserService service = new UserService(users, mock(AuditLogService.class));

        assertThrows(ResourceNotFoundException.class, () -> service.getUserById(99L));
    }

    @Test
    void duplicateUsernameOrEmailIsAConflict() {
        UserRepository users = mock(UserRepository.class);
        UserService service = new UserService(users, mock(AuditLogService.class));

        UserRequest duplicateUsername = new UserRequest();
        duplicateUsername.setUsername("taken");
        duplicateUsername.setEmail("free@example.com");
        when(users.existsByUsername("taken")).thenReturn(true);
        assertThrows(ResourceConflictException.class, () -> service.createUser(duplicateUsername));

        UserRequest duplicateEmail = new UserRequest();
        duplicateEmail.setUsername("free");
        duplicateEmail.setEmail("taken@example.com");
        when(users.existsByEmail("taken@example.com")).thenReturn(true);
        assertThrows(ResourceConflictException.class, () -> service.createUser(duplicateEmail));
    }

    @Test
    void updatingAnUnknownUserIsNotFound() {
        UserRepository users = mock(UserRepository.class);
        when(users.findById(99L)).thenReturn(Optional.empty());

        UserService service = new UserService(users, mock(AuditLogService.class));
        signInAs(users, 1L, "admin-account");

        UserRequest request = new UserRequest();
        request.setUsername("renamed");
        request.setEmail("renamed@example.com");

        assertThrows(ResourceNotFoundException.class, () -> service.updateUser(99L, request));
    }

    private void signInAs(UserRepository users, Long userId, String username) {
        User actor = new User();
        actor.setUserId(userId);
        actor.setUsername(username);
        actor.setRole(User.Role.ADMIN);
        when(users.findByUsername(username)).thenReturn(Optional.of(actor));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, "", List.of()));
    }

    @Test
    void departmentHasNoStatusColumnSoItCannotBeArchived() {
        DepartmentRepository departments = mock(DepartmentRepository.class);
        Department department = new Department();
        department.setDepartmentId(4L);
        when(departments.findById(4L)).thenReturn(Optional.of(department));

        var summary = new DepartmentService(departments,
                mock(StudentRepository.class),
                mock(FacultyRepository.class),
                mock(CourseRepository.class)).getDepartmentDependencies(4L);

        assertEquals(false, summary.isCanArchive());
        assertEquals(true, summary.isCanDelete());
        assertEquals(false, summary.getDependencyCounts().get("students") > 0);
    }
}

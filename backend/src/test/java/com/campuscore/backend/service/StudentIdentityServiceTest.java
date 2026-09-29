package com.campuscore.backend.service;

import com.campuscore.backend.entity.Student;
import com.campuscore.backend.entity.User;
import com.campuscore.backend.repository.DepartmentRepository;
import com.campuscore.backend.repository.StudentRepository;
import com.campuscore.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentIdentityServiceTest {

    @Test
    void resolvesProfileUsingAuthenticatedUsernameAndUserForeignKey() {
        StudentRepository students = mock(StudentRepository.class);
        UserRepository users = mock(UserRepository.class);
        StudentService service = new StudentService(students, users, mock(DepartmentRepository.class));
        User authenticatedUser = new User();
        authenticatedUser.setUserId(73L);
        Student linkedStudent = new Student();
        linkedStudent.setStudentId(214L);
        when(users.findByUsername("verified-user")).thenReturn(Optional.of(authenticatedUser));
        when(students.findByUserUserId(73L)).thenReturn(Optional.of(linkedStudent));

        assertSame(linkedStudent, service.getStudentForUsername("verified-user"));
        verify(students).findByUserUserId(73L);
    }
}

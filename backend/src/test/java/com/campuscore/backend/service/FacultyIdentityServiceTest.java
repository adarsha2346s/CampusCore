package com.campuscore.backend.service;

import com.campuscore.backend.entity.Faculty;
import com.campuscore.backend.entity.User;
import com.campuscore.backend.repository.DepartmentRepository;
import com.campuscore.backend.repository.FacultyRepository;
import com.campuscore.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FacultyIdentityServiceTest {

    @Test
    void resolvesProfileUsingAuthenticatedUsernameAndUserForeignKey() {
        FacultyRepository facultyRepository = mock(FacultyRepository.class);
        UserRepository users = mock(UserRepository.class);
        FacultyService service = new FacultyService(facultyRepository, users, mock(DepartmentRepository.class));
        User authenticatedUser = new User();
        authenticatedUser.setUserId(82L);
        Faculty linkedFaculty = new Faculty();
        linkedFaculty.setFacultyId(319L);
        when(users.findByUsername("verified-user")).thenReturn(Optional.of(authenticatedUser));
        when(facultyRepository.findByUserUserId(82L)).thenReturn(Optional.of(linkedFaculty));

        assertSame(linkedFaculty, service.getFacultyForUsername("verified-user"));
        verify(facultyRepository).findByUserUserId(82L);
    }
}

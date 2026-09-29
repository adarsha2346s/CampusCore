package com.campuscore.backend.controller;

import com.campuscore.backend.repository.FacultyRepository;
import com.campuscore.backend.repository.UserRepository;
import com.campuscore.backend.service.FacultyService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class FacultySelfAccessTest {

    @Test
    void nonFacultyCannotUseFacultySelfEndpoint() {
        FacultyService service = mock(FacultyService.class);
        FacultyController controller = new FacultyController(
                service, mock(UserRepository.class), mock(FacultyRepository.class));
        var authentication = new UsernamePasswordAuthenticationToken(
                "admin-account", "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        var response = controller.getMyFacultyProfile(authentication);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(service, never()).getFacultyForUsername("admin-account");
    }
}

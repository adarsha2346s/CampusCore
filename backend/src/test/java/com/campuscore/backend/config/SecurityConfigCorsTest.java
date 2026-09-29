package com.campuscore.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SecurityConfigCorsTest {

    @Test
    void corsOriginsCanBeConfiguredWithoutEnablingCredentials() {
        var source = new SecurityConfig(null)
                .corsConfigurationSource("http://localhost:5173, https://campus.example");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users");
        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertEquals(java.util.List.of("http://localhost:5173", "https://campus.example"),
                configuration.getAllowedOrigins());
        assertFalse(Boolean.TRUE.equals(configuration.getAllowCredentials()));
    }
}

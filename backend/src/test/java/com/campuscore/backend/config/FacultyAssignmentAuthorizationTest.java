package com.campuscore.backend.config;

import com.campuscore.backend.security.JwtAuthenticationFilter;
import com.campuscore.backend.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * Authorization for the faculty-to-course assignment endpoints.
 *
 * <p>Writes are administrative; reads follow the existing course read rule. The
 * real {@link SecurityFilterChain} is exercised, so the assertions cover the
 * shipped rules plus the real 401/403 handlers. No database is required.
 */
class FacultyAssignmentAuthorizationTest {

    private static final String BASE = "/api/v1/courses/12/faculty-assignments";

    private static final SecurityContext ADMIN = contextFor("admin-account", "ROLE_ADMIN");
    private static final SecurityContext FACULTY = contextFor("faculty-account", "ROLE_FACULTY");
    private static final SecurityContext STUDENT = contextFor("student-account", "ROLE_STUDENT");

    @Configuration(proxyBeanMethods = false)
    static class SecurityHarness {

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter() {
            return new JwtAuthenticationFilter(mock(JwtService.class), mock(UserDetailsService.class));
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        UserDetailsService userDetailsService() {
            return username -> {
                if (username != null && username.endsWith("-account")) {
                    return User.withUsername(username)
                            .password("{noop}unused")
                            .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                            .build();
                }
                throw new UsernameNotFoundException(String.valueOf(username));
            };
        }
    }

    private AnnotationConfigApplicationContext context;
    private FilterChainProxy filterChainProxy;

    @BeforeEach
    void buildFilterChain() {
        context = new AnnotationConfigApplicationContext();
        context.register(SecurityHarness.class, SecurityConfig.class);
        context.refresh();

        SecurityFilterChain filterChain = context.getBean(SecurityFilterChain.class);
        assertNotNull(filterChain);
        filterChainProxy = new FilterChainProxy(filterChain);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        if (context != null) {
            context.close();
        }
    }

    // ------------------------------------------------------------------
    // Writes: ADMIN only
    // ------------------------------------------------------------------

    @Test
    void adminCanAssignAFacultyMember() {
        assertEquals(200, dispatch("POST", BASE, ADMIN).getStatus());
    }

    @Test
    void adminCanReassignACourse() {
        assertEquals(200, dispatch("PUT", BASE, ADMIN).getStatus());
    }

    @Test
    void adminCanRemoveAnAssignment() {
        assertEquals(200, dispatch("DELETE", BASE + "/21", ADMIN).getStatus());
    }

    @Test
    void studentCannotAssignAFacultyMember() {
        assertEquals(403, dispatch("POST", BASE, STUDENT).getStatus());
    }

    @Test
    void studentCannotReassignACourse() {
        assertEquals(403, dispatch("PUT", BASE, STUDENT).getStatus());
    }

    @Test
    void studentCannotRemoveAnAssignment() {
        assertEquals(403, dispatch("DELETE", BASE + "/21", STUDENT).getStatus());
    }

    @Test
    void facultyCannotAssignReassignOrRemove() {
        assertEquals(403, dispatch("POST", BASE, FACULTY).getStatus());
        assertEquals(403, dispatch("PUT", BASE, FACULTY).getStatus());
        assertEquals(403, dispatch("DELETE", BASE + "/21", FACULTY).getStatus());
    }

    @Test
    void unauthenticatedAssignmentWritesAreRejected() {
        assertEquals(401, dispatch("POST", BASE, null).getStatus());
        assertEquals(401, dispatch("PUT", BASE, null).getStatus());
        assertEquals(401, dispatch("DELETE", BASE + "/21", null).getStatus());
    }

    // ------------------------------------------------------------------
    // Reads follow the existing course read rule
    // ------------------------------------------------------------------

    @Test
    void anyAuthenticatedUserCanViewAssignments() {
        assertEquals(200, dispatch("GET", BASE, STUDENT).getStatus());
        assertEquals(200, dispatch("GET", BASE + "/history", STUDENT).getStatus());
        assertEquals(200, dispatch("GET", BASE, FACULTY).getStatus());
    }

    @Test
    void unauthenticatedReadsAreRejected() {
        assertEquals(401, dispatch("GET", BASE, null).getStatus());
        assertEquals(401, dispatch("GET", BASE + "/history", null).getStatus());
    }

    @Test
    void facultyCourseListingKeepsTheExistingFacultyReadRule() {
        // /api/v1/faculty/** was already ADMIN or FACULTY before this change.
        assertEquals(200, dispatch("GET", "/api/v1/faculty/21/courses", FACULTY).getStatus());
        assertEquals(403, dispatch("GET", "/api/v1/faculty/21/courses", STUDENT).getStatus());
        assertEquals(401, dispatch("GET", "/api/v1/faculty/21/courses", null).getStatus());
    }

    @Test
    void theWriteRuleIsScopedToTheAssignmentPath() {
        // A POST to another course sub-path must keep the previous behaviour, so
        // the new rule is not silently widened to all of /courses/**.
        assertEquals(200, dispatch("POST", "/api/v1/courses/12/some-other-action", STUDENT).getStatus());
        assertEquals(403, dispatch("POST", "/api/v1/courses", STUDENT).getStatus());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private MockHttpServletResponse dispatch(String method, String path, SecurityContext securityContext) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRequestURI(path);
        request.setServletPath(path);
        request.setContextPath("");

        if (securityContext != null) {
            request.setAttribute(
                    RequestAttributeSecurityContextRepository.DEFAULT_REQUEST_ATTR_NAME,
                    securityContext);
        }

        MockHttpServletResponse response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
        try {
            filterChainProxy.doFilter(request, response, new MockFilterChain());
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        return response;
    }

    private static SecurityContext contextFor(String username, String role) {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(username, "n/a", authorities));
        return context;
    }
}

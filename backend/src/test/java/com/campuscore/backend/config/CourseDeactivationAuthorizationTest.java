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
 * POST /api/v1/courses/{id}/deactivate must require the same administrative
 * authorization as the other course-management operations.
 *
 * <p>These tests build the real {@link SecurityFilterChain} declared by
 * {@link SecurityConfig} and push requests through a {@link FilterChainProxy},
 * so the assertions exercise the shipped rules together with the real 401 and 403
 * handlers. Only {@link SecurityConfig} is registered, so no database, no JWT
 * signing and no application context startup is required.
 *
 * <p>An authorized request travels the whole chain and reaches the end of the
 * filter chain (reported as 200); a denied request is short-circuited by the
 * authentication entry point (401) or the access-denied handler (403).
 */
class CourseDeactivationAuthorizationTest {

    private static final String DEACTIVATE_PATH = "/api/v1/courses/12/deactivate";

    private static final SecurityContext ADMIN =
            contextFor("admin-account", "ROLE_ADMIN");

    private static final SecurityContext FACULTY =
            contextFor("faculty-account", "ROLE_FACULTY");

    private static final SecurityContext STUDENT =
            contextFor("student-account", "ROLE_STUDENT");

    @Configuration(proxyBeanMethods = false)
    static class SecurityHarness {

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter() {
            // A real filter so the chain is actually traversed; its collaborators
            // are stubs because these tests never send an Authorization header.
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
    // The reported vulnerability
    // ------------------------------------------------------------------

    @Test
    void adminCanDeactivateACourse() {
        assertEquals(200, dispatch("POST", DEACTIVATE_PATH, ADMIN).getStatus());
    }

    @Test
    void studentCannotDeactivateACourse() {
        assertEquals(403, dispatch("POST", DEACTIVATE_PATH, STUDENT).getStatus());
    }

    @Test
    void facultyCannotDeactivateACourse() {
        assertEquals(403, dispatch("POST", DEACTIVATE_PATH, FACULTY).getStatus());
    }

    @Test
    void unauthenticatedDeactivationIsRejected() {
        assertEquals(401, dispatch("POST", DEACTIVATE_PATH, null).getStatus());
    }

    @Test
    void deactivationIsRestrictedForEveryIdentifier() {
        assertEquals(403, dispatch("POST", "/api/v1/courses/1/deactivate", STUDENT).getStatus());
        assertEquals(403, dispatch("POST", "/api/v1/courses/999999/deactivate", STUDENT).getStatus());
        assertEquals(200, dispatch("POST", "/api/v1/courses/1/deactivate", ADMIN).getStatus());
        assertEquals(200, dispatch("POST", "/api/v1/courses/999999/deactivate", ADMIN).getStatus());
    }

    // ------------------------------------------------------------------
    // The fix must not change any other rule
    // ------------------------------------------------------------------

    @Test
    void otherCourseManagementRulesAreUnchanged() {
        assertEquals(200, dispatch("POST", "/api/v1/courses", ADMIN).getStatus());
        assertEquals(200, dispatch("PUT", "/api/v1/courses/12", ADMIN).getStatus());
        assertEquals(200, dispatch("DELETE", "/api/v1/courses/12", ADMIN).getStatus());

        assertEquals(403, dispatch("POST", "/api/v1/courses", STUDENT).getStatus());
        assertEquals(403, dispatch("PUT", "/api/v1/courses/12", STUDENT).getStatus());
        assertEquals(403, dispatch("DELETE", "/api/v1/courses/12", STUDENT).getStatus());
    }

    @Test
    void courseCatalogStaysReadableByAnyAuthenticatedUser() {
        assertEquals(200, dispatch("GET", "/api/v1/courses", STUDENT).getStatus());
        assertEquals(200, dispatch("GET", "/api/v1/courses/12", STUDENT).getStatus());
        assertEquals(401, dispatch("GET", "/api/v1/courses", null).getStatus());
    }

    @Test
    void courseDependencySummaryIsNowAdminOnly() {
        // Phase 2 tightened this: dependency summaries expose record counts and
        // were previously readable by any signed-in user.
        assertEquals(200, dispatch("GET", "/api/v1/courses/12/dependencies", ADMIN).getStatus());
        assertEquals(403, dispatch("GET", "/api/v1/courses/12/dependencies", STUDENT).getStatus());
        assertEquals(403, dispatch("GET", "/api/v1/courses/12/dependencies", FACULTY).getStatus());
    }

    @Test
    void courseSubPathMutationsAreNoLongerInheritedByAnyRole() {
        // These previously fell through to anyRequest().authenticated(). The
        // role matrix now lists every /api/v1 path and denies the remainder.
        assertEquals(403, dispatch("POST", "/api/v1/courses/12/not-deactivate", STUDENT).getStatus());
        assertEquals(403, dispatch("POST", "/api/v1/courses/12/deactivate/extra", STUDENT).getStatus());
        assertEquals(403, dispatch("GET", "/api/v1/courses/12/deactivate", STUDENT).getStatus());
        assertEquals(403, dispatch("PUT", "/api/v1/courses/12/deactivate", STUDENT).getStatus());
        assertEquals(403, dispatch("DELETE", "/api/v1/courses/12/deactivate", STUDENT).getStatus());
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

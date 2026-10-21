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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Authoritative role matrix.
 *
 * <p>Every route below is classified as PUBLIC, AUTHENTICATED, STUDENT,
 * FACULTY or ADMIN. The matrix only decides which roles may call a route; which
 * records a caller may touch is covered by AccessScopeTest.
 *
 * <p>Drives the real filter chain, so the real 401/403 handlers are exercised.
 */
class RoleMatrixAuthorizationTest {

    private AnnotationConfigApplicationContext context;
    private FilterChainProxy filterChainProxy;

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

    // ==================================================================
    // PUBLIC
    // ==================================================================

    @Test
    void loginIsPublic() {
        assertEquals(200, dispatch("POST", "/api/v1/auth/login", null).getStatus());
    }

    // ==================================================================
    // AUTHENTICATED - reference data
    // ==================================================================

    @Test
    void referenceDataIsReadableByEveryRole() {
        for (SecurityContext actor : List.of(ADMIN, FACULTY, STUDENT)) {
            assertEquals(200, dispatch("GET", "/api/v1/auth/me", actor).getStatus());
            assertEquals(200, dispatch("GET", "/api/v1/departments", actor).getStatus());
            assertEquals(200, dispatch("GET", "/api/v1/courses", actor).getStatus());
            assertEquals(200, dispatch("GET", "/api/v1/courses/1", actor).getStatus());
            assertEquals(200, dispatch("GET", "/api/v1/grading-policies", actor).getStatus());
        }
    }

    @Test
    void referenceDataRejectsAnonymous() {
        for (String path : List.of(
                "/api/v1/auth/me",
                "/api/v1/departments",
                "/api/v1/courses",
                "/api/v1/grading-policies")) {

            assertEquals(401, dispatch("GET", path, null).getStatus(), path);
        }
    }

    @Test
    void courseAssignmentsAreReadableByEveryRoleButNotAnonymous() {
        assertEquals(200, dispatch("GET", "/api/v1/courses/1/faculty-assignments", STUDENT).getStatus());
        assertEquals(401, dispatch("GET", "/api/v1/courses/1/faculty-assignments", null).getStatus());
    }

    // ==================================================================
    // STUDENT self service
    // ==================================================================

    @Test
    void studentSelfServiceRoutesAreNotReachableByOtherRoles() {
        assertEquals(403, dispatch("GET", "/api/v1/students/me", FACULTY).getStatus());
        assertEquals(403, dispatch("GET", "/api/v1/enrollments/me", FACULTY).getStatus());
        assertEquals(403, dispatch("GET", "/api/v1/faculty/me", STUDENT).getStatus());
    }

    @Test
    void studentReachesOwnAcademicResources() {
        assertEquals(200, dispatch("GET", "/api/v1/students/me", STUDENT).getStatus());
        assertEquals(200, dispatch("GET", "/api/v1/enrollments/me", STUDENT).getStatus());
        assertEquals(200, dispatch("GET", "/api/v1/marks/enrollment/1", STUDENT).getStatus());
        assertEquals(200, dispatch("GET", "/api/v1/attendance-records/enrollment/1", STUDENT).getStatus());
        assertEquals(200, dispatch("GET", "/api/v1/gpa/enrollment/1", STUDENT).getStatus());
        assertEquals(200, dispatch("GET", "/api/v1/dashboard/student/1", STUDENT).getStatus());
    }

    @Test
    void studentIsRejectedFromEveryAdministrativeOperation() {
        String[][] forbidden = {
                {"GET", "/api/v1/users"},
                {"POST", "/api/v1/users"},
                {"PUT", "/api/v1/users/1"},
                {"DELETE", "/api/v1/users/1"},
                {"POST", "/api/v1/students"},
                {"PUT", "/api/v1/students/1"},
                {"DELETE", "/api/v1/students/1"},
                {"POST", "/api/v1/faculty"},
                {"GET", "/api/v1/faculty"},
                {"POST", "/api/v1/departments"},
                {"PUT", "/api/v1/departments/1"},
                {"DELETE", "/api/v1/departments/1"},
                {"POST", "/api/v1/courses"},
                {"POST", "/api/v1/courses/1/deactivate"},
                {"PUT", "/api/v1/courses/1"},
                {"DELETE", "/api/v1/courses/1"},
                {"POST", "/api/v1/courses/1/faculty-assignments"},
                {"PUT", "/api/v1/courses/1/faculty-assignments"},
                {"DELETE", "/api/v1/courses/1/faculty-assignments/2"},
                {"GET", "/api/v1/enrollments"},
                {"GET", "/api/v1/enrollments/1"},
                {"POST", "/api/v1/enrollments"},
                {"POST", "/api/v1/assessments"},
                {"GET", "/api/v1/assessments"},
                {"GET", "/api/v1/assessments/1"},
                {"GET", "/api/v1/assessments/course/1"},
                {"POST", "/api/v1/marks"},
                {"POST", "/api/v1/attendance-sessions"},
                {"POST", "/api/v1/attendance-records"},
                {"POST", "/api/v1/grading-policies"},
                {"GET", "/api/v1/audit-logs"},
                {"GET", "/api/v1/audit-logs/user/1"},
                {"GET", "/api/v1/students/1/dependencies"},
                {"GET", "/api/v1/courses/1/dependencies"},
        };

        for (String[] call : forbidden) {
            assertEquals(403, dispatch(call[0], call[1], STUDENT).getStatus(),
                    "STUDENT should be denied " + call[0] + " " + call[1]);
        }
    }

    @Test
    void studentReachesOwnAssessmentMetadataButNotTheGeneralAssessmentRoutes() {
        // GET /assessments/me is the student-scoped metadata route.
        assertEquals(200, dispatch("GET", "/api/v1/assessments/me", STUDENT).getStatus());

        // The general assessment routes stay administrator/faculty only.
        assertEquals(403, dispatch("GET", "/api/v1/assessments", STUDENT).getStatus());
        assertEquals(403, dispatch("GET", "/api/v1/assessments/1", STUDENT).getStatus());
        assertEquals(403, dispatch("GET", "/api/v1/assessments/course/1", STUDENT).getStatus());
        assertEquals(401, dispatch("GET", "/api/v1/assessments/me", null).getStatus());
    }

    @Test
    void theStudentAssessmentMetadataRouteIsNotOpenToFacultyOrAdmin() {
        // Faculty and admin keep their own assessment routes; the self route is
        // reserved for the student so it cannot become a broader reader.
        assertEquals(403, dispatch("GET", "/api/v1/assessments/me", FACULTY).getStatus());
        assertEquals(403, dispatch("GET", "/api/v1/assessments/me", ADMIN).getStatus());

        // The institution-wide listing is administrator only; faculty reach
        // assessments through a course they are assigned to.
        assertEquals(403, dispatch("GET", "/api/v1/assessments", FACULTY).getStatus());
        assertEquals(200, dispatch("GET", "/api/v1/assessments", ADMIN).getStatus());
        assertEquals(200, dispatch("GET", "/api/v1/assessments/course/1", FACULTY).getStatus());
    }

    @Test
    void unscopedCollectionsAreAdministratorOnly() {
        // Regression guard. These collection routes call the service-level
        // findAll() with no ownership filter, so allowing FACULTY or STUDENT
        // through the matcher exposed every mark, assessment, attendance session
        // and attendance record in the institution. Faculty and students must use
        // the scope-checked sub-resource routes instead.
        String[] collections = {
                "/api/v1/assessments",
                "/api/v1/marks",
                "/api/v1/attendance-sessions",
                "/api/v1/attendance-records",
        };

        for (String path : collections) {
            assertEquals(403, dispatch("GET", path, STUDENT).getStatus(),
                    "STUDENT must not read the unscoped collection " + path);
            assertEquals(403, dispatch("GET", path, FACULTY).getStatus(),
                    "FACULTY must not read the unscoped collection " + path);
            assertEquals(403, dispatch("GET", path + "?page=0&size=10", STUDENT).getStatus(),
                    "STUDENT must not page the unscoped collection " + path);
            assertEquals(200, dispatch("GET", path, ADMIN).getStatus(),
                    "ADMIN keeps global read on " + path);
        }
    }

    @Test
    void facultyAndStudentKeepTheirScopedSubResourceRoutes() {
        // The collections above are closed, but the scope-checked sub-resource
        // routes remain open to the owning role.
        assertEquals(200, dispatch("GET", "/api/v1/assessments/1", FACULTY).getStatus());
        assertEquals(200, dispatch("GET", "/api/v1/assessments/course/1", FACULTY).getStatus());
        assertEquals(200, dispatch("GET", "/api/v1/marks/1", FACULTY).getStatus());
        assertEquals(200, dispatch("GET", "/api/v1/marks/enrollment/1", FACULTY).getStatus());
        assertEquals(200, dispatch("GET", "/api/v1/marks/enrollment/1", STUDENT).getStatus());
        assertEquals(200, dispatch("GET", "/api/v1/attendance-sessions/1", FACULTY).getStatus());
        assertEquals(200, dispatch("GET", "/api/v1/attendance-sessions/course/1", FACULTY).getStatus());
        assertEquals(200, dispatch("GET", "/api/v1/attendance-records/1", STUDENT).getStatus());
        assertEquals(200, dispatch("GET", "/api/v1/attendance-records/enrollment/1", STUDENT).getStatus());
    }

    @Test
    void facultyCanListEnrollmentsForAnAssignedCourseOnly() {
        assertEquals(200, dispatch("GET", "/api/v1/enrollments/course/1", FACULTY).getStatus());
        assertEquals(200, dispatch("GET", "/api/v1/enrollments/course/1", ADMIN).getStatus());
        assertEquals(403, dispatch("GET", "/api/v1/enrollments/course/1", STUDENT).getStatus());
        assertEquals(401, dispatch("GET", "/api/v1/enrollments/course/1", null).getStatus());
    }

    @Test
    void enrollmentLifecycleWritesAreAdministratorOnly() {
        // Enrollment update and delete change academic records, so they stay with
        // administrators. Faculty and students keep their scoped read routes.
        String[][] writes = {
                {"PUT", "/api/v1/enrollments/1"},
                {"DELETE", "/api/v1/enrollments/1"},
        };

        for (String[] call : writes) {
            assertEquals(200, dispatch(call[0], call[1], ADMIN).getStatus(),
                    "ADMIN should be allowed " + call[0] + " " + call[1]);
            assertEquals(403, dispatch(call[0], call[1], FACULTY).getStatus(),
                    "FACULTY should be denied " + call[0] + " " + call[1]);
            assertEquals(403, dispatch(call[0], call[1], STUDENT).getStatus(),
                    "STUDENT should be denied " + call[0] + " " + call[1]);
            assertEquals(401, dispatch(call[0], call[1], null).getStatus(),
                    "anonymous should be rejected from " + call[0] + " " + call[1]);
        }
    }

    @Test
    void facultyKeepsScopedEnrollmentReadsWhileLifecycleWritesStayAdmin() {
        assertEquals(200, dispatch("GET", "/api/v1/enrollments/course/1", FACULTY).getStatus());
        assertEquals(200, dispatch("GET", "/api/v1/enrollments/course/1", ADMIN).getStatus());
        assertEquals(403, dispatch("GET", "/api/v1/enrollments/course/1", STUDENT).getStatus());
    }

    @Test
    void assessmentLifecycleWritesAreAdministratorOnly() {
        // Faculty may create assessments for their own courses, but editing or
        // deleting one rewrites graded results, so it stays with administrators.
        String[][] writes = {
                {"PUT", "/api/v1/assessments/1"},
                {"DELETE", "/api/v1/assessments/1"},
        };

        for (String[] call : writes) {
            assertEquals(200, dispatch(call[0], call[1], ADMIN).getStatus(),
                    "ADMIN should be allowed " + call[0] + " " + call[1]);
            assertEquals(403, dispatch(call[0], call[1], FACULTY).getStatus(),
                    "FACULTY should be denied " + call[0] + " " + call[1]);
            assertEquals(403, dispatch(call[0], call[1], STUDENT).getStatus(),
                    "STUDENT should be denied " + call[0] + " " + call[1]);
            assertEquals(401, dispatch(call[0], call[1], null).getStatus(),
                    "anonymous should be rejected from " + call[0] + " " + call[1]);
        }
    }

    @Test
    void facultyKeepsAssessmentReadsAndCreateWhileLifecycleStaysAdmin() {
        assertEquals(200, dispatch("GET", "/api/v1/assessments/course/1", FACULTY).getStatus());
        assertEquals(200, dispatch("POST", "/api/v1/assessments", FACULTY).getStatus());
        assertEquals(403, dispatch("GET", "/api/v1/assessments/course/1", STUDENT).getStatus());
        assertEquals(403, dispatch("POST", "/api/v1/assessments", STUDENT).getStatus());
        assertEquals(403, dispatch("GET", "/api/v1/assessments", FACULTY).getStatus());
    }

    @Test
    void markLifecycleWritesAreAdministratorOnly() {
        // Faculty record marks on their own courses, but rewriting or removing an
        // already recorded grade stays with administrators.
        String[][] writes = {
                {"PUT", "/api/v1/marks/1"},
                {"DELETE", "/api/v1/marks/1"},
        };

        for (String[] call : writes) {
            assertEquals(200, dispatch(call[0], call[1], ADMIN).getStatus(),
                    "ADMIN should be allowed " + call[0] + " " + call[1]);
            assertEquals(403, dispatch(call[0], call[1], FACULTY).getStatus(),
                    "FACULTY should be denied " + call[0] + " " + call[1]);
            assertEquals(403, dispatch(call[0], call[1], STUDENT).getStatus(),
                    "STUDENT should be denied " + call[0] + " " + call[1]);
            assertEquals(401, dispatch(call[0], call[1], null).getStatus(),
                    "anonymous should be rejected from " + call[0] + " " + call[1]);
        }
    }

    @Test
    void markReadAndCreateRolesAreUnchanged() {
        // Reading a specific mark is open to all three roles, with the service
        // narrowing by enrollment ownership; faculty create marks, students do not.
        assertEquals(200, dispatch("GET", "/api/v1/marks/1", ADMIN).getStatus());
        assertEquals(200, dispatch("GET", "/api/v1/marks/1", FACULTY).getStatus());
        assertEquals(200, dispatch("GET", "/api/v1/marks/1", STUDENT).getStatus());
        assertEquals(403, dispatch("GET", "/api/v1/marks", FACULTY).getStatus());
        assertEquals(403, dispatch("GET", "/api/v1/marks", STUDENT).getStatus());
        assertEquals(200, dispatch("POST", "/api/v1/marks", FACULTY).getStatus());
        assertEquals(403, dispatch("POST", "/api/v1/marks", STUDENT).getStatus());
    }

    @Test
    void attendanceSessionLifecycleWritesAreAdministratorOnly() {
        // Faculty take sessions for their own courses, but editing or deleting a
        // session that attendance has been filed against stays with administrators.
        String[][] writes = {
                {"PUT", "/api/v1/attendance-sessions/1"},
                {"DELETE", "/api/v1/attendance-sessions/1"},
        };

        for (String[] call : writes) {
            assertEquals(200, dispatch(call[0], call[1], ADMIN).getStatus(),
                    "ADMIN should be allowed " + call[0] + " " + call[1]);
            assertEquals(403, dispatch(call[0], call[1], FACULTY).getStatus(),
                    "FACULTY should be denied " + call[0] + " " + call[1]);
            assertEquals(403, dispatch(call[0], call[1], STUDENT).getStatus(),
                    "STUDENT should be denied " + call[0] + " " + call[1]);
            assertEquals(401, dispatch(call[0], call[1], null).getStatus(),
                    "anonymous should be rejected from " + call[0] + " " + call[1]);
        }
    }

    @Test
    void courseDeactivateIsAdministratorOnly() {
        // The catalog is readable by any signed-in role, but retiring a course
        // changes who can enrol, so it stays with administrators.
        String path = "/api/v1/courses/1/deactivate";

        assertEquals(200, dispatch("POST", path, ADMIN).getStatus(),
                "ADMIN should be allowed to deactivate");
        assertEquals(403, dispatch("POST", path, FACULTY).getStatus(),
                "FACULTY should be denied deactivation");
        assertEquals(403, dispatch("POST", path, STUDENT).getStatus(),
                "STUDENT should be denied deactivation");
        assertEquals(401, dispatch("POST", path, null).getStatus(),
                "anonymous should be rejected");
    }

    @Test
    void courseCatalogReadsRemainOpenToEverySignedInRole() {
        for (SecurityContext role : new SecurityContext[] {ADMIN, FACULTY, STUDENT}) {
            assertEquals(200, dispatch("GET", "/api/v1/courses", role).getStatus(),
                    "signed-in roles should be able to list courses");
            assertEquals(200, dispatch("GET", "/api/v1/courses/1", role).getStatus(),
                    "signed-in roles should be able to read a course");
        }
    }

    @Test
    void studentOwnDataRoutesAreReachableDespiteTheBroaderFacultyMatchers() {
        // Regression guard. "/api/v1/marks/**" and "/api/v1/attendance-records/**"
        // are faculty-or-admin and also match "/…/me", so the student rules must be
        // declared first or every student is locked out of their own marks and
        // attendance. These paths taking no identifier is what makes that safe.
        for (String path : new String[] {"/api/v1/marks/me", "/api/v1/attendance-records/me"}) {
            assertEquals(200, dispatch("GET", path, STUDENT).getStatus(),
                    "STUDENT should reach their own " + path);
            assertEquals(401, dispatch("GET", path, null).getStatus(),
                    "anonymous should be rejected from " + path);
        }

        // Student-only, matching /api/v1/assessments/me. "My" data has no meaning
        // for another role, and administrators reach every mark through the
        // institution-wide listing instead.
        assertEquals(403, dispatch("GET", "/api/v1/marks/me", ADMIN).getStatus());
        assertEquals(403, dispatch("GET", "/api/v1/marks/me", FACULTY).getStatus());
    }

    @Test
    void theInstitutionWideSessionListingStaysAdministratorOnly() {
        // The student workspace used to call this and was refused. It must remain
        // admin-only; students read their own rows through /attendance-records/me.
        assertEquals(200, dispatch("GET", "/api/v1/attendance-sessions", ADMIN).getStatus());
        assertEquals(403, dispatch("GET", "/api/v1/attendance-sessions", FACULTY).getStatus());
        assertEquals(403, dispatch("GET", "/api/v1/attendance-sessions", STUDENT).getStatus());
    }

    @Test
    void studentIsDeniedTheStudentDirectory() {
        assertEquals(403, dispatch("GET", "/api/v1/students", STUDENT).getStatus());
        assertEquals(403, dispatch("GET", "/api/v1/students/1", STUDENT).getStatus());
    }

    // ==================================================================
    // FACULTY teaching scope
    // ==================================================================

    @Test
    void facultyReachesTeachingRoutes() {
        assertEquals(200, dispatch("GET", "/api/v1/faculty/me", FACULTY).getStatus());
        assertEquals(200, dispatch("GET", "/api/v1/faculty/1", FACULTY).getStatus());
        assertEquals(200, dispatch("GET", "/api/v1/faculty/1/courses", FACULTY).getStatus());
        assertEquals(200, dispatch("GET", "/api/v1/students", FACULTY).getStatus());
        assertEquals(200, dispatch("GET", "/api/v1/students/1", FACULTY).getStatus());
        assertEquals(200, dispatch("GET", "/api/v1/assessments/course/1", FACULTY).getStatus());
        assertEquals(200, dispatch("GET", "/api/v1/marks/enrollment/1", FACULTY).getStatus());
        assertEquals(200, dispatch("GET", "/api/v1/attendance-sessions/course/1", FACULTY).getStatus());
        assertEquals(200, dispatch("GET", "/api/v1/attendance-records/session/1", FACULTY).getStatus());
        assertEquals(200, dispatch("GET", "/api/v1/gpa/enrollment/1", FACULTY).getStatus());
        assertEquals(200, dispatch("POST", "/api/v1/assessments", FACULTY).getStatus());
        assertEquals(200, dispatch("POST", "/api/v1/marks", FACULTY).getStatus());
    }

    @Test
    void facultyIsRejectedFromEveryAdministrativeOperation() {
        String[][] forbidden = {
                {"GET", "/api/v1/users"},
                {"POST", "/api/v1/users"},
                {"PUT", "/api/v1/users/1"},
                {"DELETE", "/api/v1/users/1"},
                {"POST", "/api/v1/students"},
                {"PUT", "/api/v1/students/1"},
                {"DELETE", "/api/v1/students/1"},
                {"POST", "/api/v1/faculty"},
                {"GET", "/api/v1/faculty"},
                {"POST", "/api/v1/departments"},
                {"PUT", "/api/v1/departments/1"},
                {"DELETE", "/api/v1/departments/1"},
                {"POST", "/api/v1/courses"},
                {"POST", "/api/v1/courses/1/deactivate"},
                {"PUT", "/api/v1/courses/1"},
                {"DELETE", "/api/v1/courses/1"},
                {"POST", "/api/v1/courses/1/faculty-assignments"},
                {"PUT", "/api/v1/courses/1/faculty-assignments"},
                {"DELETE", "/api/v1/courses/1/faculty-assignments/2"},
                {"GET", "/api/v1/enrollments"},
                {"GET", "/api/v1/enrollments/1"},
                {"POST", "/api/v1/grading-policies"},
                {"GET", "/api/v1/audit-logs"},
                {"GET", "/api/v1/students/1/dependencies"},
                {"GET", "/api/v1/courses/1/dependencies"},
        };

        for (String[] call : forbidden) {
            assertEquals(403, dispatch(call[0], call[1], FACULTY).getStatus(),
                    "FACULTY should be denied " + call[0] + " " + call[1]);
        }
    }

    @Test
    void facultyCannotReadTheFacultyDirectory() {
        assertEquals(403, dispatch("GET", "/api/v1/faculty", FACULTY).getStatus());
    }

    // ==================================================================
    // ADMIN
    // ==================================================================

    @Test
    void adminReachesEveryAdministrativeRoute() {
        String[][] allowed = {
                {"GET", "/api/v1/users"},
                {"POST", "/api/v1/users"},
                {"PUT", "/api/v1/users/1"},
                {"DELETE", "/api/v1/users/1"},
                {"GET", "/api/v1/students"},
                {"GET", "/api/v1/students/1"},
                {"GET", "/api/v1/students/1/dependencies"},
                {"POST", "/api/v1/students"},
                {"PUT", "/api/v1/students/1"},
                {"DELETE", "/api/v1/students/1"},
                {"GET", "/api/v1/faculty"},
                {"GET", "/api/v1/faculty/1"},
                {"GET", "/api/v1/faculty/1/dependencies"},
                {"POST", "/api/v1/faculty"},
                {"DELETE", "/api/v1/faculty/1"},
                {"GET", "/api/v1/departments/1"},
                {"GET", "/api/v1/departments/1/dependencies"},
                {"POST", "/api/v1/departments"},
                {"PUT", "/api/v1/departments/1"},
                {"DELETE", "/api/v1/departments/1"},
                {"GET", "/api/v1/courses/1/dependencies"},
                {"POST", "/api/v1/courses"},
                {"POST", "/api/v1/courses/1/deactivate"},
                {"PUT", "/api/v1/courses/1"},
                {"DELETE", "/api/v1/courses/1"},
                {"POST", "/api/v1/courses/1/faculty-assignments"},
                {"PUT", "/api/v1/courses/1/faculty-assignments"},
                {"DELETE", "/api/v1/courses/1/faculty-assignments/2"},
                {"GET", "/api/v1/enrollments"},
                {"POST", "/api/v1/enrollments"},
                {"GET", "/api/v1/assessments"},
                {"POST", "/api/v1/assessments"},
                {"GET", "/api/v1/assessments"},
                {"GET", "/api/v1/assessments/1"},
                {"GET", "/api/v1/assessments/course/1"},
                {"GET", "/api/v1/marks"},
                {"POST", "/api/v1/marks"},
                {"GET", "/api/v1/attendance-sessions"},
                {"POST", "/api/v1/attendance-sessions"},
                {"GET", "/api/v1/attendance-records"},
                {"POST", "/api/v1/attendance-records"},
                {"POST", "/api/v1/grading-policies"},
                {"GET", "/api/v1/audit-logs"},
                {"GET", "/api/v1/dashboard/student/1"},
                {"GET", "/api/v1/gpa/enrollment/1"},
        };

        for (String[] call : allowed) {
            assertEquals(200, dispatch(call[0], call[1], ADMIN).getStatus(),
                    "ADMIN should be allowed " + call[0] + " " + call[1]);
        }
    }

    @Test
    void adminDoesNotGetTheFacultySelfEndpoint() {
        // /faculty/me is scoped to a faculty profile, which an admin does not have.
        assertEquals(403, dispatch("GET", "/api/v1/faculty/me", ADMIN).getStatus());
        assertEquals(403, dispatch("GET", "/api/v1/students/me", ADMIN).getStatus());
    }

    // ==================================================================
    // ANONYMOUS
    // ==================================================================

    @Test
    void anonymousIsRejectedFromEveryProtectedRoute() {
        String[][] protectedRoutes = {
                {"GET", "/api/v1/users"},
                {"GET", "/api/v1/students"},
                {"GET", "/api/v1/students/1"},
                {"GET", "/api/v1/faculty"},
                {"GET", "/api/v1/enrollments"},
                {"GET", "/api/v1/marks"},
                {"POST", "/api/v1/marks"},
                {"GET", "/api/v1/assessments"},
                {"POST", "/api/v1/assessments"},
                {"GET", "/api/v1/assessments"},
                {"GET", "/api/v1/assessments/1"},
                {"GET", "/api/v1/assessments/course/1"},
                {"GET", "/api/v1/attendance-sessions"},
                {"GET", "/api/v1/attendance-records"},
                {"GET", "/api/v1/audit-logs"},
                {"GET", "/api/v1/gpa/enrollment/1"},
                {"GET", "/api/v1/dashboard/student/1"},
                {"POST", "/api/v1/courses"},
                {"POST", "/api/v1/courses/1/deactivate"},
                {"POST", "/api/v1/courses/1/faculty-assignments"},
                {"GET", "/api/v1/grading-policies"},
        };

        for (String[] call : protectedRoutes) {
            assertEquals(401, dispatch(call[0], call[1], null).getStatus(),
                    "anonymous should be rejected from " + call[0] + " " + call[1]);
        }
    }

    // ==================================================================
    // Previously discovered authorization fall-through
    // ==================================================================

    @Test
    void subPathMutationsNoLongerFallThroughToAuthenticated() {
        // Each of these previously matched no rule and inherited
        // anyRequest().authenticated(), letting any signed-in role mutate it.
        String[][] fallThrough = {
                {"POST", "/api/v1/faculty/1"},
                {"PUT", "/api/v1/faculty/1"},
                {"DELETE", "/api/v1/faculty/1"},
                {"POST", "/api/v1/departments/1"},
                {"POST", "/api/v1/grading-policies/1"},
                {"PUT", "/api/v1/grading-policies/1"},
                {"DELETE", "/api/v1/grading-policies/1"},
                {"PUT", "/api/v1/courses"},
                {"POST", "/api/v1/enrollments/1"},
                {"PUT", "/api/v1/enrollments/1"},
                {"DELETE", "/api/v1/enrollments/1"},
                {"POST", "/api/v1/users/1"},
                {"PUT", "/api/v1/departments"},
                {"POST", "/api/v1/audit-logs"},
        };

        for (String[] call : fallThrough) {
            assertEquals(403, dispatch(call[0], call[1], STUDENT).getStatus(),
                    "STUDENT should be denied " + call[0] + " " + call[1]);
            assertEquals(403, dispatch(call[0], call[1], FACULTY).getStatus(),
                    "FACULTY should be denied " + call[0] + " " + call[1]);
        }
    }

    @Test
    void courseDeactivationRemainsAdminOnly() {
        assertEquals(200, dispatch("POST", "/api/v1/courses/1/deactivate", ADMIN).getStatus());
        assertEquals(403, dispatch("POST", "/api/v1/courses/1/deactivate", FACULTY).getStatus());
        assertEquals(403, dispatch("POST", "/api/v1/courses/1/deactivate", STUDENT).getStatus());
        assertEquals(401, dispatch("POST", "/api/v1/courses/1/deactivate", null).getStatus());
    }

    @Test
    void unclassifiedApiPathsAreNotInheritedByNonAdminRoles() {
        // An unlisted /api/v1 path used to inherit anyRequest().authenticated().
        // It is now ADMIN-only, so a signed-in non-admin is refused.
        for (String path : List.of(
                "/api/v1/unknown",
                "/api/v1/unknown/1",
                "/api/v1/analytics",
                "/api/v1/reports/export")) {

            assertEquals(403, dispatch("GET", path, STUDENT).getStatus(),
                    "STUDENT should be denied " + path);
            assertEquals(403, dispatch("GET", path, FACULTY).getStatus(),
                    "FACULTY should be denied " + path);
            assertEquals(401, dispatch("GET", path, null).getStatus(),
                    "anonymous should be rejected from " + path);
        }
    }

    @Test
    void pathsOutsideTheApiPrefixAreNotReachableEvenByAdmin() {
        for (String path : List.of("/api/v2/users", "/internal/metrics")) {
            int status = dispatch("GET", path, ADMIN).getStatus();
            assertTrue(status == 403 || status == 404,
                    "path " + path + " returned " + status);
        }
    }

    @Test
    void routesOutsideTheApiPrefixAreNotServed() {
        assertEquals(403, dispatch("GET", "/actuator/env", ADMIN).getStatus());
        assertEquals(403, dispatch("GET", "/", ADMIN).getStatus());
    }

    // ==================================================================
    // Login failure consistency
    // ==================================================================

    @Test
    void loginFailureReturnsTheSame401EnvelopeForEveryActor() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new com.campuscore.backend.controller.AuthController(
                        loginThatFails(), mock(com.campuscore.backend.repository.UserRepository.class)))
                .setControllerAdvice(
                        new com.campuscore.backend.exception.GlobalExceptionHandler())
                .build();

        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nobody\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // No username enumeration and no stack trace in the failure body.
        assertTrue(body.contains("Invalid username or password"), body);
        assertTrue(!body.contains("Exception"), body);
        assertTrue(!body.contains("nobody"), body);
    }

    private com.campuscore.backend.service.AuthService loginThatFails() {
        com.campuscore.backend.service.AuthService service =
                mock(com.campuscore.backend.service.AuthService.class);
        when(service.login(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("bad"));
        return service;
    }

    // ==================================================================
    // Helpers
    // ==================================================================

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

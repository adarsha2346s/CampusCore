package com.campuscore.backend.security;

import com.campuscore.backend.config.SecurityConfig;
import com.campuscore.backend.dto.LoginRequest;
import com.campuscore.backend.entity.User;
import com.campuscore.backend.repository.UserRepository;
import com.campuscore.backend.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end authentication path.
 *
 * <p>This is the smallest integration test that exercises the real chain:
 * {@code POST /api/v1/auth/login} against the real {@code AuthController} and
 * {@code AuthService}, a genuinely signed JWT from the real {@code JwtService},
 * then that token presented as a {@code Bearer} header through the real
 * {@code JwtAuthenticationFilter}, the real role matrix and the real 401/403
 * handlers.
 *
 * <p>Only the two edges are substituted: the {@link UserRepository} is mocked
 * (no database) and {@code UserDetailsService} is in-memory. Nothing about the
 * token, the filter, the role matrix or the error contract is faked.
 */
@SpringBootTest(classes = JwtAuthenticationE2ETest.TestApp.class)
@ActiveProfiles("e2e")
class JwtAuthenticationE2ETest {

    private static final String PASSWORD = "correct-horse-battery";
    private static final String ADMIN_PASSWORD = "admin-pass-1234";
    private static final String FACULTY_PASSWORD = "faculty-pass-12";
    private static final String STUDENT_PASSWORD = "student-pass-12";
    private static final String UNKNOWN_PASSWORD = "nobody-pass-123";

    @SpringBootApplication
    @Import({
            SecurityConfig.class,
            JwtAuthenticationFilter.class,
            JwtService.class,
            com.campuscore.backend.service.AuthService.class,
            com.campuscore.backend.controller.AuthController.class,
            com.campuscore.backend.exception.GlobalExceptionHandler.class
    })
    static class TestApp {

        // SecurityConfig already declares the PasswordEncoder bean, so only the
        // two genuine boundaries are substituted here.
        @Bean
        UserRepository userRepository() {
            UserRepository repository = mock(UserRepository.class);
            when(repository.findByUsername(anyString())).thenAnswer(invocation -> {
                String username = invocation.getArgument(0);
                return switch (username) {
                    case "admin-account" -> Optional.of(user(username, "ADMIN"));
                    case "faculty-account" -> Optional.of(user(username, "FACULTY"));
                    case "student-account" -> Optional.of(user(username, "STUDENT"));
                    default -> Optional.empty();
                };
            });
            return repository;
        }

        private static User user(String username, String role) {
            User user = new User();
            user.setUserId(1L);
            user.setUsername(username);
            user.setEmail(username + "@example.test");
            user.setRole(User.Role.valueOf(role));
            user.setActive(true);
            return user;
        }

        @Bean
        UserDetailsService userDetailsService() {
            // A local encoder avoids injecting the PasswordEncoder bean, which
            // lives in SecurityConfig and would close a circular reference back
            // to JwtAuthenticationFilter.
            PasswordEncoder encoder = new BCryptPasswordEncoder();
            InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
            manager.createUser(org.springframework.security.core.userdetails.User
                    .withUsername("admin-account").password(encoder.encode(ADMIN_PASSWORD))
                    .roles("ADMIN").build());
            manager.createUser(org.springframework.security.core.userdetails.User
                    .withUsername("faculty-account").password(encoder.encode(FACULTY_PASSWORD))
                    .roles("FACULTY").build());
            manager.createUser(org.springframework.security.core.userdetails.User
                    .withUsername("student-account").password(encoder.encode(STUDENT_PASSWORD))
                    .roles("STUDENT").build());
            return manager;
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // spring-security-test is deliberately not a dependency, so the real
        // FilterChainProxy bean is installed directly. Everything the filter chain
        // does - JWT parsing, authority resolution, the role matrix, the 401/403
        // handlers - is the production code path.
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .addFilters(context.getBean(FilterChainProxy.class))
                .build();
    }

    @AfterEach
    void clearContext() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------
    // Login
    // ------------------------------------------------------------------

    @Test
    void loginReturnsAUsableTokenForEachRole() throws Exception {
        for (String[] account : List.of(
                new String[]{"admin-account", ADMIN_PASSWORD},
                new String[]{"faculty-account", FACULTY_PASSWORD},
                new String[]{"student-account", STUDENT_PASSWORD})) {

            String body = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType("application/json")
                            .content("{\"username\":\"" + account[0]
                                    + "\",\"password\":\"" + account[1] + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.role").value(account[0].split("-")[0].toUpperCase()))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            String token = body.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
            assertNotNull(token);

            // The issued token must be accepted by the protected route.
            mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value(account[0]));
        }
    }

    @Test
    void loginRejectsAWrongPasswordWithoutRevealingTheAccount() throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"admin-account\",\"password\":\"" + UNKNOWN_PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(body.contains("Invalid username or password"), body);
        assertTrue(!body.contains("admin-account"), body);
        assertTrue(!body.contains("Exception"), body);
    }

    @Test
    void loginRejectsAnUnknownAccountIdentically() throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"ghost-account\",\"password\":\"" + UNKNOWN_PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(body.contains("Invalid username or password"), body);
        assertTrue(!body.contains("ghost-account"), body);
    }

    @Test
    void loginRejectsAMissingPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"admin-account\"}"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Token lifecycle against a protected route
    // ------------------------------------------------------------------

    @Test
    void aValidAdminTokenReachesAdminRoutes() throws Exception {
        // This context registers only AuthController, so an authorised request
        // reaches the dispatcher and ends in 404. What is under test is that the
        // role matrix let it through: never 401 (unauthenticated) and never 403
        // (wrong role).
        assertAuthorized(mockMvc.perform(get("/api/v1/users")
                .header("Authorization", "Bearer " + token("admin-account", "ADMIN"))));
    }

    @Test
    void aValidFacultyTokenReachesFacultyRoutesButNotAdminRoutes() throws Exception {
        String token = token("faculty-account", "FACULTY");

        assertAuthorized(mockMvc.perform(get("/api/v1/faculty/me")
                .header("Authorization", "Bearer " + token)));

        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void aValidStudentTokenReachesStudentRoutesButNotAdminRoutes() throws Exception {
        String token = token("student-account", "STUDENT");

        assertAuthorized(mockMvc.perform(get("/api/v1/students/me")
                .header("Authorization", "Bearer " + token)));

        assertAuthorized(mockMvc.perform(get("/api/v1/enrollments/me")
                .header("Authorization", "Bearer " + token)));

        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/faculty/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedRoutesRejectAMissingToken() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoutesRejectAForgedToken() throws Exception {
        String forged = "eyJhbGciOiJIUzI1NiJ9"
                + ".eyJzdWIiOiJhZG1pbi1hY2NvdW50Iiwicm9sZSI6IkFETUlOIn0"
                + ".not-a-valid-signature";

        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoutesRejectAGarbageToken() throws Exception {
        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoutesRejectATokenSignedWithAForeignSecret() throws Exception {
        JwtService foreign = new JwtService(
                "a-totally-different-signing-secret-value", 60_000L);

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + foreign.generateToken("admin-account", "ADMIN")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoutesRejectAnExpiredToken() throws Exception {
        JwtService expired = new JwtService(jwtSecret(), -1_000L);

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + expired.generateToken("admin-account", "ADMIN")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void roleComesFromTheDatabaseNotFromTheToken() throws Exception {
        // Token claims ADMIN, but the account only ever holds STUDENT.
        String token = jwtService.generateToken("student-account", "ADMIN");

        // The forged ADMIN claim must not open an admin route.
        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        // The account's real role still works.
        assertAuthorized(mockMvc.perform(get("/api/v1/students/me")
                .header("Authorization", "Bearer " + token)));
    }

    @Test
    void meReportsTheServerSideRoleNotTheTokenClaim() throws Exception {
        String token = jwtService.generateToken("faculty-account", "STUDENT");

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("FACULTY"));
    }

    @Test
    void malformedAuthorizationHeaderIsTreatedAsAnonymous() throws Exception {
        mockMvc.perform(get("/api/v1/users").header("Authorization", "Basic YWRtaW46cGFzcw=="))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void noAuthenticationMaterialIsReturnedInErrorBodies() throws Exception {
        String token = token("student-account", "STUDENT");

        String body = mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(!body.contains(token), "token must never be echoed");
        assertTrue(!body.contains(jwtSecret()), "signing secret must never be echoed");
        assertTrue(!body.contains(STUDENT_PASSWORD), "password must never be echoed");
        assertTrue(!body.toLowerCase().contains("password\":"), body);
    }

    /**
     * Asserts the request passed authentication and the role matrix. The outcome
     * itself is not the subject: this context has no controller behind these
     * paths, so an authorised call resolves as 404 at the dispatcher.
     */
    private static void assertAuthorized(ResultActions result) throws Exception {
        int status = result.andReturn().getResponse().getStatus();
        assertTrue(status != 401 && status != 403,
                "expected the role matrix to admit the request but got " + status);
    }

    private String token(String username, String role) {
        return jwtService.generateToken(username, role);
    }

    private static String jwtSecret() {
        // Matches the value supplied to the test context below.
        return "phase2-e2e-signing-secret-value-32bytes!!";
    }
}

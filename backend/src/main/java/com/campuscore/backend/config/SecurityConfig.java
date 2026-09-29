package com.campuscore.backend.config;

import com.campuscore.backend.security.JwtAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpStatus;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter) {

        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration)
            throws Exception {

        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:${CORS_ALLOWED_ORIGINS:http://localhost:5173}}") String configuredOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(configuredOrigins.split(","))
                .stream().map(String::trim).filter(origin -> !origin.isEmpty()).toList());
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")
        );
        configuration.setAllowedHeaders(
                List.of("Authorization", "Content-Type", "Accept")
        );
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)

                .cors(Customizer.withDefaults())

                .exceptionHandling(exceptionHandling ->
                        exceptionHandling.authenticationEntryPoint(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                        ).accessDeniedHandler((request, response, exception) ->
                                response.setStatus(HttpStatus.FORBIDDEN.value())
                        )
                )

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // Allow Spring MVC error dispatches to preserve the
                        // original API error status (for example, invalid
                        // pagination returning 400).
                        .dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()

                        // =========================
                        // PUBLIC LOGIN; CURRENT USER REQUIRES AUTHENTICATION
                        // =========================
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/api/v1/**"
                        )
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/login"
                        )
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/auth/me"
                        )
                        .authenticated()

                        // =========================
                        // USER MANAGEMENT
                        // ADMIN ONLY
                        // =========================
                        .requestMatchers(
                                "/api/v1/users",
                                "/api/v1/users/**"
                        )
                        .hasRole("ADMIN")

                        // =========================
                        // STUDENT DASHBOARD
                        // STUDENT ONLY
                        // =========================
                        .requestMatchers(
                                "/api/v1/dashboard/student/**"
                        )
                        .hasRole("STUDENT")

                        // Student records are readable by admin and faculty;
                        // students may request a record, with ownership checked
                        // in StudentController.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/students"
                        )
                        .hasAnyRole("ADMIN", "FACULTY")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/students/me"
                        )
                        .hasRole("STUDENT")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/students/**"
                        )
                        .hasAnyRole("ADMIN", "FACULTY", "STUDENT")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/students",
                                "/api/v1/students/**"
                        )
                        .hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/v1/students/**"
                        )
                        .hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/students/**"
                        )
                        .hasRole("ADMIN")

                        // GPA is available to administrators and students.
                        // Student requests are scoped to their own enrollment
                        // by GpaController.
                        .requestMatchers("/api/v1/gpa/**")
                        .hasAnyRole("ADMIN", "STUDENT")

                        // =========================
                        // FACULTY MANAGEMENT
                        // =========================

                        // Faculty list → ADMIN only
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/faculty"
                        )
                        .hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/faculty/me"
                        )
                        .hasRole("FACULTY")

                        // Faculty creation → ADMIN only
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/faculty"
                        )
                        .hasRole("ADMIN")

                        // Individual faculty record →
                        // ADMIN or FACULTY
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/faculty/**"
                        )
                        .hasAnyRole("ADMIN", "FACULTY")

                        // =========================
                        // DEPARTMENT MANAGEMENT
                        // =========================

                        // Authenticated users can view departments
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/departments",
                                "/api/v1/departments/**"
                        )
                        .authenticated()

                        // Only ADMIN can create departments
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/departments"
                        )
                        .hasRole("ADMIN")

                        // Only ADMIN can update departments
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/v1/departments/**"
                        )
                        .hasRole("ADMIN")

                        // Only ADMIN can delete departments
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/departments/**"
                        )
                        .hasRole("ADMIN")

                        // =========================
                        // COURSE MANAGEMENT
                        // =========================

                        // Authenticated users can view courses
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/courses",
                                "/api/v1/courses/**"
                        )
                        .authenticated()

                        // Only ADMIN can create courses
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/courses"
                        )
                        .hasRole("ADMIN")

                        // Only ADMIN can update courses
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/v1/courses/**"
                        )
                        .hasRole("ADMIN")

                        // Only ADMIN can delete courses
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/courses/**"
                        )
                        .hasRole("ADMIN")

                        // =========================
                        // ENROLLMENT MANAGEMENT
                        // =========================

                        // Students can list only their own enrollments through
                        // the self-service endpoint. Arbitrary enrollment
                        // records remain ADMIN-only.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/enrollments/me"
                        )
                        .hasRole("STUDENT")

                        .requestMatchers(
                                "/api/v1/enrollments",
                                "/api/v1/enrollments/**"
                        )
                        .hasRole("ADMIN")

                        // =========================
                        // ASSESSMENT MANAGEMENT
                        // =========================

                        // Current assessment endpoints expose
                        // arbitrary assessment records.
                        // Keep them ADMIN-only for now.
                        .requestMatchers(
                                "/api/v1/assessments",
                                "/api/v1/assessments/**"
                        )
                        .hasRole("ADMIN")

                        // =========================
                        // MARK MANAGEMENT
                        // =========================

                        // Current mark endpoints expose
                        // arbitrary student marks.
                        // Keep them ADMIN-only for now.
                        .requestMatchers(
                                "/api/v1/marks",
                                "/api/v1/marks/**"
                        )
                        .hasRole("ADMIN")

                        // =========================
                        // ATTENDANCE SESSION MANAGEMENT
                        // =========================

                        // Current attendance-session endpoints
                        // expose arbitrary attendance sessions.
                        // Keep them ADMIN-only for now.
                        .requestMatchers(
                                "/api/v1/attendance-sessions",
                                "/api/v1/attendance-sessions/**"
                        )
                        .hasRole("ADMIN")

                                // =========================
// ATTENDANCE RECORD MANAGEMENT
// =========================

// Current endpoints expose arbitrary attendance records.
// Keep them ADMIN-only until ownership-scoped endpoints exist.
                                .requestMatchers(
                                        "/api/v1/attendance-records",
                                        "/api/v1/attendance-records/**"
                                )
                                .hasRole("ADMIN")// =========================
                        // EVERYTHING ELSE
                                // =========================
// GRADING POLICY MANAGEMENT
// =========================

// Authenticated users can view grading policies
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/grading-policies",
                                        "/api/v1/grading-policies/**"
                                )
                                .authenticated()

// Only ADMIN can create grading policies
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/grading-policies"
                                )
                                .hasRole("ADMIN")

                                // =========================
// AUDIT LOG MANAGEMENT
// =========================

// Audit logs contain sensitive system activity.
// ADMIN only.
                                .requestMatchers(
                                        "/api/v1/audit-logs",
                                        "/api/v1/audit-logs/**"
                                )
                                .hasRole("ADMIN")

                                // =========================
                        .anyRequest()
                        .authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}

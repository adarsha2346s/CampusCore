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
            @Value("${app.cors.allowed-origins:${CORS_ALLOWED_ORIGINS:http://localhost:5173,http://127.0.0.1:5173}}") String configuredOrigins) {
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
                        // ROLE MATRIX
                        //
                        // Every /api/v1 path is listed explicitly below and the
                        // catch-all denies it. A route that is not classified can
                        // therefore never become reachable just by being added to
                        // a controller: it fails closed instead of inheriting
                        // "authenticated" by accident.
                        //
                        // This matrix only decides WHICH ROLES may call a route.
                        // WHICH RECORDS a caller may touch is enforced separately
                        // by AccessScope in the service layer, because a path
                        // matcher cannot express ownership.
                        // =========================

                        // ---- PUBLIC ----

                        // CORS preflight only.
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/api/v1/**"
                        )
                        .permitAll()

                        // Login is the only public operation. Failures return a
                        // generic 401 so a caller cannot probe for valid accounts.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/login"
                        )
                        .permitAll()

                        // ---- AUTHENTICATED (any role) ----

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/auth/me"
                        )
                        .authenticated()

                        // Read-only reference data every signed-in user needs.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/departments",
                                "/api/v1/departments/*"
                        )
                        .authenticated()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/courses",
                                "/api/v1/courses/*"
                        )
                        .authenticated()

                        // Who teaches a course is not sensitive and is shown to
                        // students; the write routes below are ADMIN only.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/courses/*/faculty-assignments",
                                "/api/v1/courses/*/faculty-assignments/*"
                        )
                        .authenticated()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/grading-policies",
                                "/api/v1/grading-policies/*"
                        )
                        .authenticated()

                        // ---- STUDENT (self service) ----

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/students/me",
                                "/api/v1/enrollments/me"
                        )
                        .hasRole("STUDENT")

                        // Assessment metadata for the caller's own marks only.
                        // Declared before the faculty assessment rule below, which
                        // also matches this path.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/assessments/me"
                        )
                        .hasRole("STUDENT")

                        // The caller's own marks and attendance, joined with their
                        // course and session so the student workspace needs one
                        // request instead of one per enrollment.
                        //
                        // Both are declared BEFORE the faculty rules below. The
                        // faculty matcher for "/api/v1/marks/**" and
                        // "/api/v1/attendance-records/**" would otherwise swallow
                        // these paths and lock every student out of their own data,
                        // which is exactly what happened when the workspace called
                        // the institution-wide session listing directly.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/marks/me",
                                "/api/v1/attendance-records/me"
                        )
                        .hasRole("STUDENT")

                        // Dashboard ownership is checked by AccessScope.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/dashboard/student/*"
                        )
                        .hasAnyRole("ADMIN", "FACULTY", "STUDENT")

                        // GPA ownership is checked by AccessScope.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/gpa/**"
                        )
                        .hasAnyRole("ADMIN", "FACULTY", "STUDENT")

                        // ---- FACULTY (teaching scope) ----
                        // Course scope is checked by AccessScope: a faculty member
                        // reaches a course only while assigned to it.

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/faculty/me"
                        )
                        .hasRole("FACULTY")

                        // The bare collection is an institution-wide listing and
                        // is not scope filtered, so it stays ADMIN only. Faculty
                        // reach assessments through a course they are assigned to.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/assessments"
                        )
                        .hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/assessments/*",
                                "/api/v1/assessments/**"
                        )
                        .hasAnyRole("ADMIN", "FACULTY")

                        // Institution-wide listing, not scope filtered: ADMIN only.
                        // A student reaching every mark through this route was a
                        // real disclosure, so it is no longer role-shared.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/marks"
                        )
                        .hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/marks/*",
                                "/api/v1/marks/**"
                        )
                        .hasAnyRole("ADMIN", "FACULTY", "STUDENT")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/attendance-sessions"
                        )
                        .hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/attendance-sessions/*",
                                "/api/v1/attendance-sessions/**"
                        )
                        .hasAnyRole("ADMIN", "FACULTY")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/attendance-records"
                        )
                        .hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/attendance-records/*",
                                "/api/v1/attendance-records/**"
                        )
                        .hasAnyRole("ADMIN", "FACULTY", "STUDENT")

                        // A faculty member sees only students they teach and only
                        // their own faculty record.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/students",
                                "/api/v1/students/*"
                        )
                        .hasAnyRole("ADMIN", "FACULTY")

                        // A student's enrollments, for the administrator and for
                        // the faculty members who teach that student. The
                        // ownership check is AccessScope.requireStudentAccess,
                        // and the service narrows the result to the caller's own
                        // courses, so the response never carries the courses the
                        // caller is not assigned to teach.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/students/*/enrollments"
                        )
                        .hasAnyRole("ADMIN", "FACULTY")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/faculty"
                        )
                        .hasRole("ADMIN")

                        // An individual faculty record and its teaching history are
                        // visible to administrators and to that faculty member only;
                        // FacultyController rejects any other faculty member.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/faculty/*",
                                "/api/v1/faculty/*/*"
                        )
                        .hasAnyRole("ADMIN", "FACULTY")

                        // Faculty need the enrollments of a course they teach in
                        // order to reach its marks. The service checks the course
                        // assignment, so this cannot be used to read the
                        // enrollments of a course the caller does not teach.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/enrollments/course/**"
                        )
                        .hasAnyRole("ADMIN", "FACULTY")

                        // Faculty record their own teaching data. Scope is checked
                        // against the target course or enrollment by AccessScope.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/assessments",
                                "/api/v1/assessments/**"
                        )
                        .hasAnyRole("ADMIN", "FACULTY")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/marks",
                                "/api/v1/marks/**"
                        )
                        .hasAnyRole("ADMIN", "FACULTY")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/attendance-sessions",
                                "/api/v1/attendance-sessions/**"
                        )
                        .hasAnyRole("ADMIN", "FACULTY")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/attendance-records",
                                "/api/v1/attendance-records/**"
                        )
                        .hasAnyRole("ADMIN", "FACULTY")

                        // ---- ADMIN (everything else under /api/v1) ----

                        .requestMatchers("/api/v1/**").hasRole("ADMIN")

                        // =========================
                        // NON-API PATHS
                        // =========================
                        // Deny by default. Nothing outside /api/v1 is served by
                        // this application, so an unclassified route is refused
                        // rather than opened up.
                        .anyRequest()
                        .denyAll()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}

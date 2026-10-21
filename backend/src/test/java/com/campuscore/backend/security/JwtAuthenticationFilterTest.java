package com.campuscore.backend.security;

import com.campuscore.backend.service.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Authentication hardening.
 *
 * <p>Asserts that only a token signed with the server secret, unexpired, and
 * belonging to an existing active account can produce an authentication. A
 * rejection must leave the request unauthenticated rather than partially
 * authenticated.
 */
class JwtAuthenticationFilterTest {

    private static final String SECRET = "phase2-verification-secret-value-32bytes";
    private static final String OTHER_SECRET = "a-completely-different-secret-value-32b";

    private JwtService jwtService;
    private UserDetailsService userDetailsService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 60_000L);
        userDetailsService = mock(UserDetailsService.class);
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void userExists() {
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(
                User.withUsername("account")
                        .password("{noop}unused")
                        .roles("STUDENT")
                        .build());
    }

    private Authentication authenticate(String bearerToken) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        if (bearerToken != null) {
            request.addHeader("Authorization", "Bearer " + bearerToken);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        return SecurityContextHolder.getContext().getAuthentication();
    }

    // ------------------------------------------------------------------
    // Happy path
    // ------------------------------------------------------------------

    @Test
    void validTokenAuthenticates() throws Exception {
        userExists();

        Authentication authentication = authenticate(jwtService.generateToken("account", "STUDENT"));

        assertNotNull(authentication);
        assertEquals("account", authentication.getName());
    }

    @Test
    void authoritiesComeFromTheDatabaseNotFromTheToken() throws Exception {
        // The token claims ADMIN, but the account is a STUDENT in the database.
        // Roles must never be trusted from the token itself.
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(
                User.withUsername("account")
                        .password("{noop}unused")
                        .roles("STUDENT")
                        .build());

        Authentication authentication = authenticate(jwtService.generateToken("account", "ADMIN"));

        assertNotNull(authentication);
        assertEquals(
                List.of("ROLE_STUDENT"),
                authentication.getAuthorities().stream().map(a -> a.getAuthority()).toList());
    }

    // ------------------------------------------------------------------
    // Rejections
    // ------------------------------------------------------------------

    @Test
    void missingHeaderLeavesTheRequestUnauthenticated() throws Exception {
        assertNull(authenticate(null));
        assertNull(authenticate(""));
    }

    @Test
    void malformedTokenLeavesTheRequestUnauthenticated() throws Exception {
        assertNull(authenticate("not-a-jwt"));
        assertNull(authenticate("a.b.c"));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() throws Exception {
        userExists();
        SecretKey foreignKey = Keys.hmacShaKeyFor(OTHER_SECRET.getBytes(StandardCharsets.UTF_8));
        String forged = Jwts.builder()
                .subject("account")
                .claim("role", "ADMIN")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(foreignKey)
                .compact();

        assertNull(authenticate(forged));
    }

    @Test
    void tokenWithATamperedPayloadIsRejected() throws Exception {
        userExists();
        String valid = jwtService.generateToken("account", "STUDENT");
        String tampered = valid.substring(0, valid.lastIndexOf('.') + 1)
                + "eyJyb2xlIjoiQURNSU4ifQ";

        assertNull(authenticate(tampered));
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        userExists();
        JwtService expiredService = new JwtService(SECRET, -1_000L);

        assertNull(authenticate(expiredService.generateToken("account", "STUDENT")));
    }

    @Test
    void deactivatedAccountIsRejected() throws Exception {
        when(userDetailsService.loadUserByUsername(anyString()))
                .thenThrow(new DisabledException("User is disabled"));

        assertNull(authenticate(jwtService.generateToken("account", "ADMIN")));
    }

    @Test
    void tokenForAnAccountThatNoLongerExistsIsRejected() throws Exception {
        when(userDetailsService.loadUserByUsername(anyString()))
                .thenThrow(new UsernameNotFoundException("gone"));

        assertNull(authenticate(jwtService.generateToken("account", "ADMIN")));
    }

    @Test
    void aRejectionNeverAuthenticatesPartially() throws Exception {
        when(userDetailsService.loadUserByUsername(anyString()))
                .thenThrow(new RuntimeException("database unavailable"));

        assertNull(authenticate(jwtService.generateToken("account", "ADMIN")));
    }

    // ------------------------------------------------------------------
    // Token properties
    // ------------------------------------------------------------------

    @Test
    void issuedTokensAlwaysCarryAnExpiry() {
        Instant before = Instant.now();
        String token = jwtService.generateToken("account", "STUDENT");

        Date expiry = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();

        assertNotNull(expiry, "token must not be usable forever");
        assertFalse(expiry.toInstant().isBefore(before));
    }

    @Test
    void aTokenCannotBeValidatedAgainstADifferentUsername() {
        String token = jwtService.generateToken("account", "STUDENT");

        assertFalse(jwtService.isTokenValid(token, "someone-else"));
    }

    @Test
    void secretIsNeverExposedByTheService() throws Exception {
        String token = jwtService.generateToken("account", "STUDENT");
        String claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .toString();

        assertFalse(claims.contains(SECRET));
        assertFalse(claims.toLowerCase().contains("password"));
    }
}

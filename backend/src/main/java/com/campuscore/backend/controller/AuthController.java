package com.campuscore.backend.controller;

import com.campuscore.backend.dto.LoginRequest;
import com.campuscore.backend.dto.LoginResponse;
import com.campuscore.backend.dto.MeResponse;
import com.campuscore.backend.entity.User;
import com.campuscore.backend.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.server.ResponseStatusException;
import com.campuscore.backend.repository.UserRepository;
import com.campuscore.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(
            AuthService authService,
            UserRepository userRepository) {

        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request) {

        try {

            String token = authService.login(request);

            User user = userRepository
                    .findByUsername(request.getUsername())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "User not found with username: " + request.getUsername())
                    );

            LoginResponse response = new LoginResponse(
                    token,
                    user.getUsername(),
                    user.getRole().name()
            );

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException | DisabledException e) {

            // Identical response for an unknown username, a wrong password and a
            // deactivated account, so this endpoint cannot be used to discover
            // which usernames exist or whether an account is active.
            log.debug("Login rejected for username \"{}\"", request.getUsername());

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "status", HttpStatus.UNAUTHORIZED.value(),
                            "error", "Unauthorized",
                            "message", INVALID_CREDENTIALS,
                            "errors", Map.of()
                    ));
        }
    }

    private static final String INVALID_CREDENTIALS = "Invalid username or password";

    @GetMapping("/me")
    public ResponseEntity<MeResponse> getCurrentUser(
            Authentication authentication) {

        // The identity comes from the verified security context. If the account
        // has since been removed the session is no longer valid.
        User user = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Session is no longer valid"
                        )
                );

        MeResponse response = new MeResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.getActive()
        );

        return ResponseEntity.ok(response);
    }
}
package com.campuscore.backend.controller;

import com.campuscore.backend.dto.LoginRequest;
import com.campuscore.backend.dto.LoginResponse;
import com.campuscore.backend.dto.MeResponse;
import com.campuscore.backend.entity.User;
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
                            new RuntimeException("User not found")
                    );

            LoginResponse response = new LoginResponse(
                    token,
                    user.getUsername(),
                    user.getRole().name()
            );

            return ResponseEntity.ok(response);

        } catch (AuthenticationException e) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "error",
                            "Invalid username or password"
                    ));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> getCurrentUser(
            Authentication authentication) {

        User user = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Authenticated user not found"
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
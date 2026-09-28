package com.campuscore.backend.service;

import com.campuscore.backend.dto.UserRequest;
import com.campuscore.backend.entity.User;
import com.campuscore.backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            AuditLogService auditLogService) {

        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    // =========================
    // GET ALL USERS
    // =========================
    public List<User> getAllUsers() {

        return userRepository.findAll();
    }

    // =========================
    // GET USER BY ID
    // =========================
    public User getUserById(Long id) {

        return userRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));
    }

    // =========================
    // CREATE USER
    // =========================
    public User createUser(UserRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Get the currently authenticated admin
        User actor = getCurrentUser();

        User user = new User();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());

        // Hash password before storing it
        String hashedPassword =
                passwordEncoder.encode(request.getPassword());

        user.setPasswordHash(hashedPassword);

        user.setRole(request.getRole());
        user.setActive(true);

        User savedUser = userRepository.save(user);

        // Create audit log
        auditLogService.createLog(
                actor.getUserId(),
                "CREATE_USER",
                "User",
                savedUser.getUserId(),
                "Created user account: "
                        + savedUser.getUsername()
                        + " with role "
                        + savedUser.getRole()
        );

        return savedUser;
    }

    // =========================
    // UPDATE USER
    // =========================
    public User updateUser(
            Long id,
            UserRequest request) {

        User actor = getCurrentUser();

        User existingUser = getUserById(id);

        String oldUsername = existingUser.getUsername();
        User.Role oldRole = existingUser.getRole();

        existingUser.setUsername(request.getUsername());
        existingUser.setEmail(request.getEmail());
        existingUser.setRole(request.getRole());

        if (request.getPassword() != null
                && !request.getPassword().isBlank()) {

            String hashedPassword =
                    passwordEncoder.encode(request.getPassword());

            existingUser.setPasswordHash(hashedPassword);
        }

        User updatedUser =
                userRepository.save(existingUser);

        // Create audit log
        String passwordMessage =
                (request.getPassword() != null
                        && !request.getPassword().isBlank())
                        ? " Password was also changed."
                        : "";

        auditLogService.createLog(
                actor.getUserId(),
                "UPDATE_USER",
                "User",
                updatedUser.getUserId(),
                "Updated user account from username "
                        + oldUsername
                        + " to "
                        + updatedUser.getUsername()
                        + " and role from "
                        + oldRole
                        + " to "
                        + updatedUser.getRole()
                        + "."
                        + passwordMessage
        );

        return updatedUser;
    }

    // =========================
    // DEACTIVATE USER
    // =========================
    public void deactivateUser(Long id) {

        User actor = getCurrentUser();

        User user = getUserById(id);

        user.setActive(false);

        userRepository.save(user);

        // Create audit log
        auditLogService.createLog(
                actor.getUserId(),
                "DEACTIVATE_USER",
                "User",
                user.getUserId(),
                "Deactivated user account: "
                        + user.getUsername()
        );
    }

    // =========================
    // GET CURRENT AUTHENTICATED USER
    // =========================
    private User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null) {

            throw new RuntimeException(
                    "Authenticated user not found"
            );
        }

        return userRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Authenticated user not found"
                        )
                );
    }
}
package com.campuscore.backend.dto;

import com.campuscore.backend.entity.User;

public class UserResponse {

    private Long userId;
    private String username;
    private String email;
    private User.Role role;
    private Boolean active;

    public UserResponse() {
    }

    public UserResponse(
            Long userId,
            String username,
            String email,
            User.Role role,
            Boolean active) {

        this.userId = userId;
        this.username = username;
        this.email = email;
        this.role = role;
        this.active = active;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public User.Role getRole() {
        return role;
    }

    public Boolean getActive() {
        return active;
    }
}

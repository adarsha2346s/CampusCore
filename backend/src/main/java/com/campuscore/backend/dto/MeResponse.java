package com.campuscore.backend.dto;

public class MeResponse {

    private Long userId;
    private String username;
    private String email;
    private String role;
    private Boolean active;

    public MeResponse() {
    }

    public MeResponse(
            Long userId,
            String username,
            String email,
            String role,
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

    public String getRole() {
        return role;
    }

    public Boolean getActive() {
        return active;
    }
}
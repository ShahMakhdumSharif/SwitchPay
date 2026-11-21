package com.switchpay.security.dto;

import java.time.Instant;
import java.util.List;

public class AuthResponse {

    private String token;
    private String tokenType = "Bearer";
    private Instant expiresAt;
    private UserSummary user;
    private Long userId;
    private String email;
    private String fullName;
    private List<String> roles;

    public AuthResponse() {
    }

    public AuthResponse(String token, Instant expiresAt, Long userId, String email, String fullName, List<String> roles) {
        this.token = token;
        this.expiresAt = expiresAt;
        this.user = new UserSummary(userId, email, fullName, roles);
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.roles = roles;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public UserSummary getUser() {
        return user;
    }

    public void setUser(UserSummary user) {
        this.user = user;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public static class UserSummary {
        private Long id;
        private String email;
        private String fullName;
        private List<String> roles;

        public UserSummary() {
        }

        public UserSummary(Long id, String email, String fullName, List<String> roles) {
            this.id = id;
            this.email = email;
            this.fullName = fullName;
            this.roles = roles;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }
    }
}

package com.switchpay.security.dto;

import com.switchpay.security.entity.Role;
import com.switchpay.security.entity.User;
import java.time.Instant;
import java.util.List;

public class UserResponse {

    private Long id;
    private String email;
    private String fullName;
    private boolean active;
    private List<String> roles;
    private Instant createdAt;

    public UserResponse() {
    }

    public UserResponse(Long id, String email, String fullName, boolean active, List<String> roles, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.active = active;
        this.roles = roles;
        this.createdAt = createdAt;
    }

    public static UserResponse fromEntity(User user) {
        List<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .sorted()
                .toList();
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.isActive(),
                roleNames,
                user.getCreatedAt()
        );
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

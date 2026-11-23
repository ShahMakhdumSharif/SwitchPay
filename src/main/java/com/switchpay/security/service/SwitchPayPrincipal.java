package com.switchpay.security.service;

import java.util.List;

public class SwitchPayPrincipal {

    private final Long userId;
    private final String email;
    private final List<String> roles;
    private final String tokenId;

    public SwitchPayPrincipal(Long userId, String email, List<String> roles, String tokenId) {
        this.userId = userId;
        this.email = email;
        this.roles = roles;
        this.tokenId = tokenId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public List<String> getRoles() {
        return roles;
    }

    public String getTokenId() {
        return tokenId;
    }

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}

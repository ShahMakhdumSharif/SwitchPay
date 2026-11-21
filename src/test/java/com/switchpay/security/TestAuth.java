package com.switchpay.security;

import com.switchpay.common.enums.RoleName;
import com.switchpay.security.service.JwtService;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@Component
public class TestAuth {

    private final JwtService jwtService;

    public TestAuth(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public String tokenFor(Long userId, String email, RoleName role) {
        return jwtService.generateToken(userId, email, List.of(role.name()));
    }

    public String adminToken() {
        return tokenFor(1L, "admin@switchpay.local", RoleName.ADMIN);
    }

    public String sellerToken(Long userId, String email) {
        return tokenFor(userId, email, RoleName.SELLER);
    }

    public String buyerToken(Long userId, String email) {
        return tokenFor(userId, email, RoleName.BUYER);
    }

    public RequestPostProcessor bearer(String token) {
        return request -> {
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            return request;
        };
    }

    public RequestPostProcessor asAdmin() {
        return bearer(adminToken());
    }

    public RequestPostProcessor asSeller(Long userId, String email) {
        return bearer(sellerToken(userId, email));
    }

    public RequestPostProcessor asBuyer(Long userId, String email) {
        return bearer(buyerToken(userId, email));
    }
}

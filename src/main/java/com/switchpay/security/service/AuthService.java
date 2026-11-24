package com.switchpay.security.service;

import com.switchpay.common.enums.RoleName;
import com.switchpay.common.exception.BadRequestException;
import com.switchpay.common.exception.ConflictException;
import com.switchpay.common.exception.UnauthorizedException;
import com.switchpay.common.port.AuditPort;
import com.switchpay.security.dto.AuthResponse;
import com.switchpay.security.dto.LoginRequest;
import com.switchpay.security.dto.RegisterRequest;
import com.switchpay.security.dto.UserResponse;
import com.switchpay.security.entity.RevokedToken;
import com.switchpay.security.entity.Role;
import com.switchpay.security.entity.User;
import com.switchpay.security.repository.RevokedTokenRepository;
import com.switchpay.security.repository.RoleRepository;
import com.switchpay.security.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditPort auditPort;
    private final Clock clock;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RevokedTokenRepository revokedTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditPort auditPort,
            Clock clock) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.revokedTokenRepository = revokedTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (request.getRole() == null || request.getRole() == RoleName.ADMIN) {
            throw new BadRequestException("Registration only allows BUYER or SELLER role");
        }
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("Email already registered: " + normalizedEmail);
        }

        Role role = roleRepository.findByName(request.getRole().name())
                .orElseGet(() -> roleRepository.save(new Role(request.getRole().name())));

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setActive(true);
        user.setCreatedAt(Instant.now(clock));
        user.setRoles(Set.of(role));

        User saved = userRepository.save(user);
        auditPort.record(saved.getId(), "USER_REGISTERED", saved.getEmail(), "Role=" + role.getName());
        return UserResponse.fromEntity(saved);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!user.isActive() || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .sorted()
                .toList();

        String token = jwtService.generateToken(user.getId(), user.getEmail(), roles);
        Instant expiresAt = jwtService.extractExpiration(token);
        auditPort.record(user.getId(), "USER_LOGIN", user.getEmail(), "Roles=" + String.join(",", roles));

        return new AuthResponse(token, expiresAt, user.getId(), user.getEmail(), user.getFullName(), roles);
    }

    @Transactional
    public void logout(String token, Long actorUserId) {
        if (token == null || token.isBlank() || !jwtService.isTokenValid(token)) {
            throw new UnauthorizedException("Invalid or expired token");
        }
        String jti = jwtService.extractTokenId(token);
        Instant expiresAt = jwtService.extractExpiration(token);
        if (jti != null && !revokedTokenRepository.existsByTokenId(jti)) {
            revokedTokenRepository.save(new RevokedToken(jti, expiresAt));
        }
        auditPort.record(actorUserId, "USER_LOGOUT", jti, "Token revoked");
    }
}

package com.switchpay.security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.switchpay.common.enums.RoleName;
import com.switchpay.common.exception.ConflictException;
import com.switchpay.common.exception.UnauthorizedException;
import com.switchpay.common.port.AuditPort;
import com.switchpay.security.dto.AuthResponse;
import com.switchpay.security.dto.LoginRequest;
import com.switchpay.security.dto.RegisterRequest;
import com.switchpay.security.dto.UserResponse;
import com.switchpay.security.entity.Role;
import com.switchpay.security.entity.User;
import com.switchpay.security.repository.RevokedTokenRepository;
import com.switchpay.security.repository.RoleRepository;
import com.switchpay.security.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String TEST_SECRET = "switchpay-unit-test-jwt-secret-key-at-least-32-bytes";
    private static final Instant FIXED_NOW = Instant.parse("2026-11-21T00:00:00Z");

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RevokedTokenRepository revokedTokenRepository;

    @Mock
    private AuditPort auditPort;

    private PasswordEncoder passwordEncoder;
    private Clock clock;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        jwtService = new JwtService(TEST_SECRET, 60, clock);
        authService = new AuthService(
                userRepository,
                roleRepository,
                revokedTokenRepository,
                passwordEncoder,
                jwtService,
                auditPort,
                clock
        );
    }

    @Test
    void register_encodesPasswordWithBCrypt() {
        RegisterRequest request = new RegisterRequest(
                "buyer@switchpay.local",
                "Secret123!",
                "Buyer One",
                RoleName.BUYER
        );

        when(userRepository.existsByEmailIgnoreCase("buyer@switchpay.local")).thenReturn(false);
        when(roleRepository.findByName("BUYER")).thenReturn(Optional.of(new Role(1L, "BUYER")));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        UserResponse response = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getPasswordHash()).isNotEqualTo("Secret123!");
        assertThat(savedUser.getPasswordHash()).startsWith("$2a$");
        assertThat(passwordEncoder.matches("Secret123!", savedUser.getPasswordHash())).isTrue();
        assertThat(response.getEmail()).isEqualTo("buyer@switchpay.local");
        assertThat(response.getRoles()).containsExactly("BUYER");
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        RegisterRequest request = new RegisterRequest(
                "duplicate@switchpay.local",
                "Secret123!",
                "Duplicate User",
                RoleName.SELLER
        );

        when(userRepository.existsByEmailIgnoreCase("duplicate@switchpay.local")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    void login_validCredentials_returnsToken() {
        User user = new User();
        user.setId(42L);
        user.setEmail("seller@switchpay.local");
        user.setFullName("Seller One");
        user.setPasswordHash(passwordEncoder.encode("CorrectHorseBatteryStaple"));
        user.setActive(true);
        user.setRoles(Set.of(new Role(2L, "SELLER")));

        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(new LoginRequest("seller@switchpay.local", "CorrectHorseBatteryStaple"));

        assertThat(response.getToken()).isNotBlank();
        assertThat(jwtService.isTokenValid(response.getToken())).isTrue();
        assertThat(jwtService.extractEmail(response.getToken())).isEqualTo("seller@switchpay.local");
        assertThat(jwtService.extractUserId(response.getToken())).isEqualTo(42L);
        assertThat(response.getRoles()).containsExactly("SELLER");
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getId()).isEqualTo(42L);
        assertThat(response.getUser().getEmail()).isEqualTo("seller@switchpay.local");
        assertThat(response.getUser().getRoles()).containsExactly("SELLER");
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        User user = new User();
        user.setId(42L);
        user.setEmail("seller@switchpay.local");
        user.setPasswordHash(passwordEncoder.encode("RightPassword123"));
        user.setActive(true);
        user.setRoles(Set.of(new Role(2L, "SELLER")));

        when(userRepository.findByEmailIgnoreCase("seller@switchpay.local")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("seller@switchpay.local", "WrongPassword999")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void jwt_expiredToken_isInvalid() {
        String token = jwtService.generateToken(99L, "buyer@switchpay.local", List.of("BUYER"));
        assertThat(jwtService.isTokenValid(token)).isTrue();

        Clock expiredClock = Clock.offset(clock, Duration.ofMinutes(61));
        JwtService futureJwtService = new JwtService(TEST_SECRET, 60, expiredClock);

        assertThat(futureJwtService.isTokenValid(token)).isFalse();
    }
}

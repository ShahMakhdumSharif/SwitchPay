package com.switchpay.security.config;

import com.switchpay.common.enums.RoleName;
import com.switchpay.security.entity.Role;
import com.switchpay.security.entity.User;
import com.switchpay.security.repository.RoleRepository;
import com.switchpay.security.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final String adminEmail;
    private final String adminPassword;

    public AdminBootstrap(
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            Clock clock,
            @Value("${switchpay.admin.email:}") String adminEmail,
            @Value("${switchpay.admin.password:}") String adminPassword) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (RoleName roleName : RoleName.values()) {
            roleRepository.findByName(roleName.name())
                    .orElseGet(() -> roleRepository.save(new Role(roleName.name())));
        }

        if (adminEmail != null && !adminEmail.isBlank() && adminPassword != null && !adminPassword.isBlank()) {
            String normalizedEmail = adminEmail.trim().toLowerCase();
            if (!userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
                Role adminRole = roleRepository.findByName(RoleName.ADMIN.name()).orElseThrow();
                User admin = new User();
                admin.setEmail(normalizedEmail);
                admin.setPasswordHash(passwordEncoder.encode(adminPassword));
                admin.setFullName("Platform Administrator");
                admin.setActive(true);
                admin.setCreatedAt(Instant.now(clock));
                admin.setRoles(Set.of(adminRole));
                userRepository.save(admin);
            }
        }
    }
}

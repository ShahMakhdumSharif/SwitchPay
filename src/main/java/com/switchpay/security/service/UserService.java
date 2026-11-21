package com.switchpay.security.service;

import com.switchpay.common.enums.RoleName;
import com.switchpay.common.exception.ForbiddenException;
import com.switchpay.common.exception.NotFoundException;
import com.switchpay.common.port.AuditPort;
import com.switchpay.security.dto.UpdateUserRequest;
import com.switchpay.security.dto.UserResponse;
import com.switchpay.security.entity.Role;
import com.switchpay.security.entity.User;
import com.switchpay.security.repository.RoleRepository;
import com.switchpay.security.repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditPort auditPort;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, AuditPort auditPort) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditPort = auditPort;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listAll() {
        return userRepository.findAll().stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id, SwitchPayPrincipal caller) {
        if (caller == null || (!caller.hasRole("ADMIN") && !id.equals(caller.getUserId()))) {
            throw new ForbiddenException("Not authorized to view this user profile");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
        return UserResponse.fromEntity(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request, Long actorUserId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            Set<Role> updatedRoles = new HashSet<>();
            for (RoleName roleName : request.getRoles()) {
                Role role = roleRepository.findByName(roleName.name())
                        .orElseGet(() -> roleRepository.save(new Role(roleName.name())));
                updatedRoles.add(role);
            }
            user.setRoles(updatedRoles);
            auditPort.record(actorUserId, "USER_ROLES_UPDATED", user.getEmail(), "NewRoles=" + request.getRoles());
        }

        User saved = userRepository.save(user);
        return UserResponse.fromEntity(saved);
    }

    @Transactional
    public void deactivateUser(Long id, Long actorUserId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
        user.setActive(false);
        userRepository.save(user);
        auditPort.record(actorUserId, "USER_DEACTIVATED", user.getEmail(), "UserId=" + id);
    }
}

package com.switchpay.security.service;

import com.switchpay.common.port.AuditPort;
import com.switchpay.security.entity.AuditLog;
import com.switchpay.security.repository.AuditLogRepository;
import com.switchpay.security.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService implements AuditPort {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public AuditService(
            AuditLogRepository auditLogRepository,
            UserRepository userRepository,
            Clock clock) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void record(Long actorUserId, String action, String target, String details) {
        Long validActorId = (actorUserId != null && userRepository.existsById(actorUserId))
                ? actorUserId
                : null;
        AuditLog log = new AuditLog(validActorId, action, target, details, Instant.now(clock));
        auditLogRepository.save(log);
    }
}

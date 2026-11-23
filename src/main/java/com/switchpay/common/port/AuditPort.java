package com.switchpay.common.port;

public interface AuditPort {

    void record(Long actorUserId, String action, String target, String details);

    default void record(String actor, String action, String target, String details) {
        Long actorId = null;
        if (actor != null) {
            try {
                actorId = Long.parseLong(actor);
            } catch (NumberFormatException ignored) {
                actorId = null;
            }
        }
        record(actorId, action, target, details);
    }
}

package com.switchpay.switching.auth;

public class AuthorizationDecision {

    private final boolean approved;
    private final String declineReason;

    private AuthorizationDecision(boolean approved, String declineReason) {
        this.approved = approved;
        this.declineReason = declineReason;
    }

    public static AuthorizationDecision approve() {
        return new AuthorizationDecision(true, null);
    }

    public static AuthorizationDecision decline(String reason) {
        return new AuthorizationDecision(false, reason);
    }

    public boolean isApproved() {
        return approved;
    }

    public String getDeclineReason() {
        return declineReason;
    }
}

package com.switchpay.gateway.entity;

import com.switchpay.common.exception.UnprocessableException;

/**
 * State pattern for a checkout session.
 *
 *   CREATED --pay-->    PAID
 *   CREATED --expire--> EXPIRED
 *   CREATED --cancel--> CANCELLED
 *
 * Only CREATED can move. PAID, EXPIRED and CANCELLED are final,
 * so every transition rule lives here in one place.
 */
public enum CheckoutSessionState {

    CREATED {
        @Override
        public CheckoutSessionState pay() {
            return PAID;
        }

        @Override
        public CheckoutSessionState expire() {
            return EXPIRED;
        }

        @Override
        public CheckoutSessionState cancel() {
            return CANCELLED;
        }
    },

    PAID,
    EXPIRED,
    CANCELLED;

    // default for the final states: no move allowed (422)
    public CheckoutSessionState pay() {
        throw notAllowed("pay");
    }

    public CheckoutSessionState expire() {
        throw notAllowed("expire");
    }

    public CheckoutSessionState cancel() {
        throw notAllowed("cancel");
    }

    private UnprocessableException notAllowed(String action) {
        return new UnprocessableException("Cannot " + action + " a checkout session that is " + name());
    }
}

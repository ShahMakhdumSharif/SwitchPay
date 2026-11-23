CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    reference VARCHAR(64) NOT NULL UNIQUE,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    type VARCHAR(32) NOT NULL,
    channel_id BIGINT,
    channel_code VARCHAR(32),
    status VARCHAR(32) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'BDT',
    source_account_id BIGINT,
    source_account_no VARCHAR(64),
    destination_account_id BIGINT,
    destination_account_no VARCHAR(64),
    merchant_id BIGINT,
    checkout_session_id BIGINT,
    actor_user_id BIGINT,
    decline_reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_channel_code ON transactions(channel_code);
CREATE INDEX idx_transactions_actor_user_id ON transactions(actor_user_id);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);

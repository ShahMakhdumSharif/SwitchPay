-- F1 channels, F4 merchants and checkout sessions (owner: Ruhan)

CREATE TABLE channels (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    type VARCHAR(16) NOT NULL CHECK (type IN ('ATM', 'MOBILE', 'WEB')),
    name VARCHAR(100) NOT NULL,
    location VARCHAR(255),
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- one merchant per SELLER user (1:1 with users)
CREATE TABLE merchants (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
    business_name VARCHAR(150) NOT NULL,
    settlement_account_no VARCHAR(34) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE checkout_sessions (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(64) NOT NULL UNIQUE,
    merchant_id BIGINT NOT NULL REFERENCES merchants(id),
    amount NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'BDT',
    status VARCHAR(16) NOT NULL DEFAULT 'CREATED' CHECK (status IN ('CREATED', 'PAID', 'EXPIRED', 'CANCELLED')),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_checkout_sessions_merchant_id ON checkout_sessions(merchant_id);

-- default channels (C7). V7 also seeds them, but databases that ran V7 before
-- this table existed skipped that step, so we add them here too.
INSERT INTO channels (code, type, name, location, status)
VALUES
    ('ATM', 'ATM', 'Default ATM Terminal', 'Dhaka Central Branch', 'ACTIVE'),
    ('MOBILE', 'MOBILE', 'SwitchPay Mobile Banking App', 'Bangladesh Nationwide', 'ACTIVE'),
    ('WEB', 'WEB', 'SwitchPay Web Checkout Gateway', 'Online E-Commerce', 'ACTIVE')
ON CONFLICT (code) DO NOTHING;

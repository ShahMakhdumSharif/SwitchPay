CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    account_no VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT,
    type VARCHAR(32) NOT NULL,
    balance NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    currency VARCHAR(3) NOT NULL DEFAULT 'BDT',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_accounts_account_no ON accounts(account_no);
CREATE INDEX idx_accounts_user_id ON accounts(user_id);
CREATE INDEX idx_accounts_status ON accounts(status);

-- Seed baseline test accounts for cross-module integration tests
INSERT INTO accounts (account_no, user_id, type, balance, status, currency)
VALUES 
    ('ACC-1001', 201, 'SAVINGS', 50000.00, 'ACTIVE', 'BDT'),
    ('ACC-2001', 201, 'SAVINGS', 50000.00, 'ACTIVE', 'BDT'),
    ('ACC-2002', 201, 'CURRENT', 50000.00, 'ACTIVE', 'BDT')
ON CONFLICT (account_no) DO NOTHING;

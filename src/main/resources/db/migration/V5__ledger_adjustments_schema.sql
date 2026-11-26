-- V5: Ledger and Adjustments Schema (Owner: Sakal)
-- Double-entry ledger entries and transaction adjustments (refunds and reversals)

CREATE TABLE transaction_adjustments (
    id BIGSERIAL PRIMARY KEY,
    original_transaction_id BIGINT NOT NULL REFERENCES transactions(id),
    type VARCHAR(32) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    reason VARCHAR(255),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_by BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE ledger_entries (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL REFERENCES transactions(id),
    account_id BIGINT NOT NULL REFERENCES accounts(id),
    entry_type VARCHAR(32) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    adjustment_id BIGINT REFERENCES transaction_adjustments(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ledger_entries_transaction_id ON ledger_entries(transaction_id);
CREATE INDEX idx_ledger_entries_account_id ON ledger_entries(account_id);
CREATE INDEX idx_ledger_entries_adjustment_id ON ledger_entries(adjustment_id);
CREATE INDEX idx_transaction_adjustments_original_txn_id ON transaction_adjustments(original_transaction_id);
CREATE INDEX idx_transaction_adjustments_status ON transaction_adjustments(status);

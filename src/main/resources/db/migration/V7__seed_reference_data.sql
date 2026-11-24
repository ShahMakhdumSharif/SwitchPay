-- V7: Seed reference data (Owner: Sharif)
-- Roles (ADMIN, SELLER, BUYER), system accounts (SYS-CASH, SYS-MERCHANT-PAYABLE), and default channels (ATM, MOBILE, WEB)

INSERT INTO roles (name) VALUES
    ('ADMIN'),
    ('SELLER'),
    ('BUYER')
ON CONFLICT (name) DO NOTHING;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'accounts'
    ) THEN
        INSERT INTO accounts (account_no, user_id, type, balance, status, currency)
        VALUES
            ('SYS-CASH', NULL, 'CURRENT', 10000000.00, 'ACTIVE', 'BDT'),
            ('SYS-MERCHANT-PAYABLE', NULL, 'CURRENT', 0.00, 'ACTIVE', 'BDT')
        ON CONFLICT (account_no) DO NOTHING;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'channels'
    ) THEN
        INSERT INTO channels (code, type, name, location, status)
        VALUES
            ('ATM', 'ATM', 'Default ATM Terminal', 'Dhaka Central Branch', 'ACTIVE'),
            ('MOBILE', 'MOBILE', 'SwitchPay Mobile Banking App', 'Bangladesh Nationwide', 'ACTIVE'),
            ('WEB', 'WEB', 'SwitchPay Web Checkout Gateway', 'Online E-Commerce', 'ACTIVE')
        ON CONFLICT (code) DO NOTHING;
    END IF;
END $$;

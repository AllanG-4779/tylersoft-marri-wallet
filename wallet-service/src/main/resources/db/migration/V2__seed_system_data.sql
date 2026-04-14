-- =============================================================
-- V2: Seed system reference data
-- =============================================================

-- Currencies: KES, BWP
INSERT INTO sys_currencies (currency_name, currency_code, iso_code, status, created_by)
VALUES
    ('Kenyan Shilling', 'KES', 'KES', 1, 'system'),
    ('Botswana Pula',   'BWP', 'BWP', 1, 'system');

-- Mobile channel
INSERT INTO chn_channels (channel_name, client_id, channel_key, description, status, created_by)
VALUES ('MOBILE', 'mobile-client-001', 'mobile-channel-secret', 'Mobile application channel', 1, 'system');

-- System service: Fund Transfer
INSERT INTO sys_services (transaction_type, is_bill, is_enquiry, status, description, created_by)
VALUES ('FT', FALSE, FALSE, 1, 'Fund Transfer between wallets', 'system');

-- Account type: WALLET with TA prefix
INSERT INTO acc_account_types (
    account_prefix, account_number_length,
    min_accounts, max_accounts,
    type_name, yearly_limit,
    min_balance_limit, max_balance_limit,
    can_overdraw, overdraw_limit,
    category, description,
    status, created_by, account_pan_enabled
)
VALUES (
    'TA', 12,
    1, 5,
    'Wallet Account', 10000000.0000,
    0.0000, 5000000.0000,
    FALSE, 0.0000,
    'WALLET', 'Standard mobile wallet account',
    1, 'system', FALSE
);

-- Service management: FT via Mobile channel
INSERT INTO cfg_service_management (
    service_id, service_code,
    receiver_narration, sender_narration,
    daily_limit, weekly_limit, monthly_limit,
    channel_id, description, status, created_by
)
SELECT
    s.id,  'FT',
    'Funds received via transfer', 'Funds sent via transfer',
    100000.0000, 500000.0000, 2000000.0000,
    c.id, 'FT service via Mobile channel', 1, 'system'
FROM sys_services   s
JOIN chn_channels   c ON c.channel_name      = 'MOBILE'
WHERE s.transaction_type = 'FT';

-- Charge config: flat fee of 30 on FT transactions
INSERT INTO cfg_changes (
    min_amount, max_amount,
    charge_value, value_type, charge_type,
    receiver_narration, sender_narration,
    service_management_id, status, created_by
)
SELECT
    1.0000, 1000000.0000,
    30.0000, 'FIXED', 'CHARGE',
    'Fund transfer fee credit', 'Fund transfer charge debit',
    sm.id, 1, 'system'
FROM cfg_service_management sm
JOIN sys_services           s  ON s.id = sm.service_id
WHERE s.transaction_type = 'FT';

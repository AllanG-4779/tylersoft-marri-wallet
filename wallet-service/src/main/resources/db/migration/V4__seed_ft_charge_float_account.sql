-- =============================================================
-- V4: Seed FT charge float account and link to charge config
-- =============================================================

-- 1. Account type for system charge float accounts
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
           'SA', 12,
           0, 1,
           'Charge Float Account', 999999999.0000,
           0.0000, 999999999.0000,
           FALSE, 0.0000,
           'ASSET', 'System charge',
           1, 'system', FALSE
       );

-- 2. Insert the float account, then patch the account number
WITH inserted AS (
INSERT INTO acc_accounts (
    account_number,
    opening_date, opening_balance,
    actual_balance, available_balance,
    account_type_id, currency_id,
    account_name,
    allow_dr, allow_cr,
    blocked, dormant,
    status, created_by
)
SELECT
    'PENDING',
    NOW(), 0.0000,
    0.0000, 0.0000,
    at.id,
    c.id,
    'FT Charge Float',
    FALSE, TRUE,
    FALSE, FALSE,
    1, 'system'
FROM acc_account_types at
    JOIN sys_currencies c ON c.currency_code = 'KES'
WHERE at.account_prefix = 'SA'   -- ✅ FIXED (was CF)
    RETURNING id
    )
UPDATE acc_accounts a
SET account_number = sp_generate_ac_number(a.id::INT, 'SA', 12, TRUE)
    FROM inserted i
WHERE a.id = i.id;

-- 3. Link the FT charge config row to the float account
UPDATE transaction_charges_config tc
SET    account_id = a.id
FROM   acc_accounts      a
JOIN   acc_account_types at ON at.id = a.account_type_id
WHERE  at.account_prefix = 'SA'
  AND  a.account_name    = 'FT Charge Float'
  AND  tc.service_management_id IN (
           SELECT sm.id
           FROM   cfg_service_management sm
           JOIN   sys_services           s ON s.id = sm.service_id
           WHERE  s.transaction_type = 'FT'
       );
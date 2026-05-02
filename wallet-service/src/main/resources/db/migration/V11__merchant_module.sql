-- =============================================================
-- V11: Merchant module — MA account type, MERCHANT_PAYMENT service
-- =============================================================

-- Merchant Account type with MA prefix
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
    'MA', 24,
    1, 1,
    'Merchant Account', 100000000.0000,
    0.0000, 50000000.0000,
    FALSE, 0.0000,
    'WALLET', 'Merchant wallet account',
    1, 'system', FALSE
);

-- MERCHANT_PAYMENT system service
INSERT INTO sys_services (transaction_type, is_bill, is_enquiry, status, description, created_by)
VALUES ('MERCHANT_PAYMENT', FALSE, FALSE, 1, 'QR-code merchant payment', 'system')
ON CONFLICT (transaction_type) DO NOTHING;

-- Service management: MERCHANT_PAYMENT
INSERT INTO cfg_service_management (
    service_id, service_code,
    receiver_narration, sender_narration,
    daily_limit, weekly_limit, monthly_limit,
    description, status, created_by
)
SELECT
    s.id, 'MP',
    'Payment received from customer', 'Payment made to merchant',
    200000.0000, 1000000.0000, 5000000.0000,
    'Merchant QR payment', 1, 'system'
FROM sys_services s
WHERE s.transaction_type = 'MERCHANT_PAYMENT';

-- Zero-fee charge config for MERCHANT_PAYMENT (configurable later)
INSERT INTO transaction_charges_config (
    min_amount, max_amount,
    charge_value, value_type, charge_type,
    receiver_narration, sender_narration,
    service_management_id, status, created_by
)
SELECT
    1.0000, 10000000.0000,
    0.0000, 'FIXED', 'CHARGE',
    'Merchant payment fee credit', 'Merchant payment charge debit',
    sm.id, 1, 'system'
FROM cfg_service_management sm
JOIN sys_services            s ON s.id = sm.service_id
WHERE s.transaction_type = 'MERCHANT_PAYMENT';

-- SMS templates for MERCHANT_PAYMENT
INSERT INTO cfg_sms_templates (transaction_type, transaction_code, direction, status, template) VALUES
('MERCHANT_PAYMENT', 'MP', 'SENDER',   'SUCCESS', 'Your payment of {currency} {amount} to {recipient} was successful. New balance: {currency} {balance}. Ref: {ref}.'),
('MERCHANT_PAYMENT', 'MP', 'RECEIVER', 'SUCCESS', 'You have received a payment of {currency} {amount}. New balance: {currency} {balance}. Ref: {ref}.'),
('MERCHANT_PAYMENT', 'MP', 'SENDER',   'FAILURE', 'Your payment of {currency} {amount} to {recipient} could not be processed. Ref: {ref}.');

-- =============================================================
-- V13: OTT Voucher Redemption — system service + SMS templates
-- =============================================================
-- NOTE: cfg_service_management for OTT_REDEMPTION/OTT_REDEEM must be
-- configured by an admin at runtime, pointing to an OTT GL account
-- that has allow_dr = TRUE.

INSERT INTO sys_services (transaction_type, is_bill, is_enquiry, status, description, created_by)
VALUES ('OTT_REDEMPTION', FALSE, FALSE, 1, 'OTT voucher redemption to wallet', 'system')
ON CONFLICT (transaction_type) DO NOTHING;

INSERT INTO cfg_sms_templates (transaction_type, transaction_code, direction, status, template)
VALUES
    ('OTT_REDEMPTION', 'OTT_REDEEM', 'RECEIVER', 'SUCCESS',
        'OTT voucher of {currency} {amount} redeemed to your wallet. New balance: {currency} {balance}. Ref: {ref}.'),
    ('OTT_REDEMPTION', 'OTT_REDEEM', 'RECEIVER', 'FAILURE',
        'OTT voucher redemption failed. Ref: {ref}. Please contact support.')
ON CONFLICT (transaction_type, transaction_code, direction, status) DO NOTHING;

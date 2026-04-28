-- =============================================================
-- V7: Seed sys_services — core wallet transaction types
-- FT is already seeded in V2; skip it here to avoid conflicts.
-- =============================================================

INSERT INTO sys_services (transaction_type, is_bill, is_enquiry, status, description, created_by)
VALUES
    -- Wallet core
    ('DEPOSIT',          FALSE, FALSE, 1, 'Cash deposit into wallet',                  'system'),
    ('WITHDRAWAL',       FALSE, FALSE, 1, 'Cash withdrawal from wallet',               'system'),


    -- Airtime
    ('AIRTIME',          TRUE,  FALSE, 1, 'Generic airtime purchase',                  'system'),


    -- Bills

    ('BILL',            TRUE,  FALSE, 1, 'Water utility payment',                     'system')


ON CONFLICT (transaction_type) DO NOTHING;


ALTER TABLE cfg_service_management ADD CONSTRAINT  unique_service_map UNIQUE (service_id, service_code);

-- V8: Seed enquiry service types (sys_services)
-- BI  = Balance Inquiry  → service code: BI_WALLET
-- STATEMENT              → service codes: MINI_STATEMENT, FULL_STATEMENT
INSERT INTO sys_services (transaction_type, is_bill, is_enquiry, status, description, created_by)
VALUES
    ('BI',        FALSE, TRUE, 1, 'Balance enquiry',   'system'),
    ('STATEMENT', FALSE, TRUE, 1, 'Account statement', 'system')
ON CONFLICT (transaction_type) DO NOTHING;

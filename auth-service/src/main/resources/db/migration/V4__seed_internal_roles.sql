INSERT INTO admin_roles (id, name, description, created_at) VALUES
    (gen_random_uuid(), 'SUPER_ADMIN',          'Full system access — can manage admins, roles, merchants, and customers',      CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'MERCHANT_MANAGER',     'Can onboard, approve, reject, suspend, and reactivate merchant accounts',     CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'COMPLIANCE_OFFICER',   'Can review KYC documents and approve or reject customer identity checks',     CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'CUSTOMER_SUPPORT',     'Read-only access to customer profiles and transaction history',               CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'FINANCE',              'Access to financial reports, account balances, and transaction summaries',    CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

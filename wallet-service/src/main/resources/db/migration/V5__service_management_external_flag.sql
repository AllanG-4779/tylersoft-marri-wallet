-- =============================================================
-- V5: Add is_external flag to cfg_service_management.
--     External services (e.g. card topup, MPESA checkout) must
--     have an account_id — the float/suspense account that
--     represents incoming funds from the external provider.
-- =============================================================

ALTER TABLE cfg_service_management
    ADD COLUMN is_external BOOLEAN NOT NULL DEFAULT FALSE;

-- Enforce: when is_external = TRUE, account_id must be set
ALTER TABLE cfg_service_management
    ADD CONSTRAINT chk_external_requires_account
        CHECK (is_external = FALSE OR account_id IS NOT NULL);

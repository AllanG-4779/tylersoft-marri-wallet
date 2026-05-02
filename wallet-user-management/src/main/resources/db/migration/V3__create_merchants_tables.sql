-- =============================================================
-- V3: Merchant onboarding — merchants, docs, QR codes
-- =============================================================

CREATE SEQUENCE merchant_code_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE merchants
(
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_code        VARCHAR(20)  UNIQUE,
    business_name        VARCHAR(255) NOT NULL,
    business_email       VARCHAR(255) NOT NULL UNIQUE,
    business_phone       VARCHAR(20)  NOT NULL,
    contact_person_name  VARCHAR(255) NOT NULL,
    contact_person_phone VARCHAR(20),
    business_type        VARCHAR(100),
    registration_number  VARCHAR(100),
    tax_number           VARCHAR(100),
    address              TEXT,
    status               VARCHAR(30)  NOT NULL DEFAULT 'PENDING_REVIEW',
    status_reason        TEXT,
    account_number       VARCHAR(50),
    created_by           VARCHAR(255) NOT NULL DEFAULT 'self',
    approved_by          VARCHAR(255),
    status_changed_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    approved_at          TIMESTAMPTZ,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE merchant_documents
(
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id         UUID         NOT NULL REFERENCES merchants (id) ON DELETE CASCADE,
    document_type       VARCHAR(100) NOT NULL,
    document_url        TEXT         NOT NULL,
    verification_status VARCHAR(30)  NOT NULL DEFAULT 'UPLOADED',
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_merchants_merchant_code   ON merchants (merchant_code);
CREATE INDEX idx_merchants_business_phone  ON merchants (business_phone);
CREATE INDEX idx_merchants_business_email  ON merchants (business_email);
CREATE INDEX idx_merchants_status          ON merchants (status);
CREATE INDEX idx_merchant_docs_merchant    ON merchant_documents (merchant_id);

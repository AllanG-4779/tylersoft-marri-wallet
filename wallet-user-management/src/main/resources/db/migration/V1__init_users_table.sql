CREATE TABLE customers
(
    id                UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    first_name        VARCHAR(100) NOT NULL,
    last_name         VARCHAR(100) NOT NULL,
    phone_number      VARCHAR(20)  NOT NULL UNIQUE,
    email             VARCHAR(255) NOT NULL UNIQUE,
    pin_hash          VARCHAR(255),
    status            VARCHAR      NOT NULL DEFAULT 'INITIATED',
    status_reason     TEXT,
    status_changed_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE identity_documents
(
    id                  UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    customer_id         UUID        NOT NULL REFERENCES customers (id) ON DELETE CASCADE,
    id_type             VARCHAR     NOT NULL,
    id_number           VARCHAR(100),
    front_image_url     VARCHAR(500),
    back_image_url      VARCHAR(500),
    verification_status VARCHAR     NOT NULL DEFAULT 'UPLOADED',
    rejection_reason    TEXT,
    provider_reference  VARCHAR(255),
    verified_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE otp_verifications
(
    id           UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    customer_id  UUID         NOT NULL REFERENCES customers (id) ON DELETE CASCADE,
    phone_number VARCHAR(20)  NOT NULL,
    otp_hash     VARCHAR(255) NOT NULL,
    purpose      VARCHAR      NOT NULL DEFAULT 'REGISTRATION',
    is_used      BOOLEAN      NOT NULL DEFAULT FALSE,
    attempts     INT          NOT NULL DEFAULT 0,
    max_attempts INT          NOT NULL DEFAULT 3,
    expires_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW() + INTERVAL '5 minutes',
    used_at      TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ============================================================
-- INDEXES
-- ============================================================

-- customers
CREATE INDEX idx_customers_phone    ON customers (phone_number);
CREATE INDEX idx_customers_email    ON customers (email);
CREATE INDEX idx_customers_status   ON customers (status);

-- identity_documents
CREATE INDEX idx_identity_customer  ON identity_documents (customer_id);
CREATE INDEX idx_identity_status    ON identity_documents (verification_status);

-- otp_verifications
CREATE INDEX idx_otp_customer       ON otp_verifications (customer_id);
CREATE INDEX idx_otp_purpose        ON otp_verifications (customer_id, purpose, is_used);
CREATE INDEX idx_otp_expires        ON otp_verifications (expires_at)
    WHERE is_used = FALSE;  -- partial: only active OTPs
CREATE TABLE trx_quotes (
    id               BIGSERIAL    PRIMARY KEY,
    token            VARCHAR(36)  NOT NULL UNIQUE,
    transaction_type VARCHAR(80),
    transaction_code VARCHAR(80),
    debit_account    VARCHAR(80),
    credit_account   VARCHAR(80),
    recipient_phone  VARCHAR(20),
    phone_number     VARCHAR(20),
    amount           NUMERIC(19,4),
    fee              NUMERIC(19,4),
    total_debit      NUMERIC(19,4),
    currency         VARCHAR(20),
    recipient_name   VARCHAR(80),
    status           VARCHAR(20)  DEFAULT 'PENDING',
    expires_at       TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_trx_quotes_token ON trx_quotes (token);

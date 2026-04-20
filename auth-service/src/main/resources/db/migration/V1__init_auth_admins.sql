CREATE TABLE IF NOT EXISTS auth_admins
(
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(150) NOT NULL UNIQUE,
    password_hash TEXT         NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_on    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_on    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

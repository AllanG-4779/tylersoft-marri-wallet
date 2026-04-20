CREATE TABLE IF NOT EXISTS auth_integrators
(
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(150) NOT NULL UNIQUE,
    access_key  VARCHAR(64)  NOT NULL UNIQUE,
    secret_hash TEXT         NOT NULL,
    description TEXT,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_by  VARCHAR(150) NOT NULL,
    created_on  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_on  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

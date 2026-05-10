ALTER TABLE auth_admins
    ADD COLUMN IF NOT EXISTS email                VARCHAR(255) UNIQUE,
    ADD COLUMN IF NOT EXISTS first_name           VARCHAR(255),
    ADD COLUMN IF NOT EXISTS last_name            VARCHAR(255),
    ADD COLUMN IF NOT EXISTS phone                VARCHAR(20),
    ADD COLUMN IF NOT EXISTS created_by           VARCHAR(255),
    ADD COLUMN IF NOT EXISTS enabled              BOOLEAN     NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS active               BOOLEAN     NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS first_login          BOOLEAN     NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS credentials_sent_at  TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS last_login_at        TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS failed_login_attempts INT        NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS account_locked_until TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_email ON auth_admins (email);
CREATE INDEX IF NOT EXISTS idx_phone ON auth_admins (phone);

CREATE TABLE IF NOT EXISTS admin_roles (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS admin_role_assignments (
    id       UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id UUID NOT NULL REFERENCES auth_admins(id) ON DELETE CASCADE,
    role_id  UUID NOT NULL REFERENCES admin_roles(id) ON DELETE CASCADE,
    UNIQUE (admin_id, role_id)
);

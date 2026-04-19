CREATE TABLE users.customer_devices (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id   UUID        NOT NULL REFERENCES users.customers(id) ON DELETE CASCADE,
    device_id     VARCHAR(255) NOT NULL,
    name          VARCHAR(255),
    os_version    VARCHAR(100),
    device_type   VARCHAR(20)  NOT NULL,
    app_version   VARCHAR(50),
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    registered_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_seen_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_customer_device UNIQUE (customer_id, device_id)
);

CREATE INDEX idx_customer_devices_customer_id ON users.customer_devices(customer_id);

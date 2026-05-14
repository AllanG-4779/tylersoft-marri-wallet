-- =============================================================
-- V1: Event module — events, ticket classes, planners
-- =============================================================

CREATE TABLE events
(
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_code     VARCHAR(20)  NOT NULL,
    title             VARCHAR(255) NOT NULL,
    description       TEXT,
    venue_name        VARCHAR(255),
    venue_address     TEXT,
    start_at          TIMESTAMPTZ  NOT NULL,
    end_at            TIMESTAMPTZ  NOT NULL,
    cover_image_url   TEXT,
    status            VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    status_reason     TEXT,
    status_changed_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(255) NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE event_ticket_classes
(
    id          UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id    UUID           NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    name        VARCHAR(100)   NOT NULL,
    description TEXT,
    price       NUMERIC(19, 4) NOT NULL DEFAULT 0,
    currency    VARCHAR(10)    NOT NULL DEFAULT 'ZMW',
    capacity    INT            NOT NULL,
    sold_count  INT            NOT NULL DEFAULT 0,
    status      VARCHAR(30)    NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE TABLE event_planners
(
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id    UUID         NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    customer_id UUID         NOT NULL,
    name        VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    phone       VARCHAR(20),
    role        VARCHAR(100),
    status      VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (event_id, customer_id)
);

CREATE TABLE ticket_purchases
(
    id                UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id          UUID           NOT NULL REFERENCES events (id) ON DELETE RESTRICT,
    customer_id       UUID           NOT NULL,
    total_amount      NUMERIC(19, 4) NOT NULL,
    currency          VARCHAR(10)    NOT NULL DEFAULT 'ZMW',
    status            VARCHAR(30)    NOT NULL DEFAULT 'PENDING',
    status_reason     TEXT,
    status_changed_at TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    payment_reference VARCHAR(255),
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE TABLE ticket_purchase_items
(
    id              UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    purchase_id     UUID           NOT NULL REFERENCES ticket_purchases (id) ON DELETE CASCADE,
    ticket_class_id UUID           NOT NULL REFERENCES event_ticket_classes (id) ON DELETE RESTRICT,
    quantity        INT            NOT NULL CHECK (quantity > 0),
    unit_price      NUMERIC(19, 4) NOT NULL,
    subtotal        NUMERIC(19, 4) NOT NULL,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE TABLE event_tickets
(
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    purchase_item_id UUID        NOT NULL REFERENCES ticket_purchase_items (id) ON DELETE RESTRICT,
    event_id         UUID        NOT NULL REFERENCES events (id) ON DELETE RESTRICT,
    ticket_class_id  UUID        NOT NULL REFERENCES event_ticket_classes (id) ON DELETE RESTRICT,
    customer_id      UUID        NOT NULL,
    ticket_code      VARCHAR(64) NOT NULL UNIQUE,
    status           VARCHAR(30) NOT NULL DEFAULT 'ISSUED',
    issued_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    used_at          TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE ticket_validations
(
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_code  VARCHAR(64) NOT NULL,
    event_id     UUID        NOT NULL REFERENCES events (id) ON DELETE RESTRICT,
    ticket_id    UUID        REFERENCES event_tickets (id) ON DELETE SET NULL,
    validated_by VARCHAR(255) NOT NULL,
    result       VARCHAR(30) NOT NULL,
    notes        TEXT,
    validated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_events_merchant_code        ON events (merchant_code);
CREATE INDEX idx_events_status               ON events (status);
CREATE INDEX idx_events_start_at             ON events (start_at);
CREATE INDEX idx_ticket_classes_event        ON event_ticket_classes (event_id);
CREATE INDEX idx_ticket_classes_status       ON event_ticket_classes (status);
CREATE INDEX idx_event_planners_event        ON event_planners (event_id);
CREATE INDEX idx_event_planners_customer     ON event_planners (customer_id);
CREATE INDEX idx_ticket_purchases_event       ON ticket_purchases (event_id);
CREATE INDEX idx_ticket_purchases_customer    ON ticket_purchases (customer_id);
CREATE INDEX idx_ticket_purchases_status      ON ticket_purchases (status);
CREATE INDEX idx_purchase_items_purchase      ON ticket_purchase_items (purchase_id);
CREATE INDEX idx_purchase_items_ticket_class  ON ticket_purchase_items (ticket_class_id);
CREATE INDEX idx_event_tickets_ticket_code    ON event_tickets (ticket_code);
CREATE INDEX idx_event_tickets_purchase_item  ON event_tickets (purchase_item_id);
CREATE INDEX idx_event_tickets_customer       ON event_tickets (customer_id);
CREATE INDEX idx_event_tickets_status         ON event_tickets (status);
CREATE INDEX idx_ticket_validations_code     ON ticket_validations (ticket_code);
CREATE INDEX idx_ticket_validations_event    ON ticket_validations (event_id);
CREATE INDEX idx_ticket_validations_ticket   ON ticket_validations (ticket_id);

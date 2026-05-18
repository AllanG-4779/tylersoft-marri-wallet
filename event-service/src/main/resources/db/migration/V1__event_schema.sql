-- ============================================================
-- MARRI WALLET — Event Module Migration
-- PostgreSQL 15+  |  UUID primary keys
-- User management is external; user IDs referenced as UUID
-- Enum columns stored as TEXT + CHECK constraints so that
-- Spring Data R2DBC's default Enum.name() mapping works out
-- of the box — no EnumCodec or custom R2DBC config required.
-- ============================================================


-- ============================================================
-- 0. EXTENSIONS
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";


-- ============================================================
-- 1. CATEGORIES  (self-referencing, 2-level max)
-- ============================================================

CREATE TABLE event_categories
(
    id         UUID PRIMARY KEY     DEFAULT uuid_generate_v4(),
    parent_id  UUID REFERENCES event_categories (id) ON DELETE CASCADE,
    name       VARCHAR(100) NOT NULL,
    slug       VARCHAR(100) NOT NULL,
    icon_url   TEXT,
    sort_order INTEGER      NOT NULL DEFAULT 0,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    UNIQUE (parent_id, slug)
);


-- ============================================================
-- 2. TAGS
-- ============================================================

CREATE TABLE tags
(
    id         UUID PRIMARY KEY             DEFAULT uuid_generate_v4(),
    name       VARCHAR(100) UNIQUE NOT NULL,
    slug       VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMPTZ         NOT NULL DEFAULT NOW()
);


-- ============================================================
-- 3. EVENTS
-- ============================================================

CREATE TABLE events
(
    id                      UUID PRIMARY KEY  DEFAULT uuid_generate_v4(),
    organization_id         UUID     NOT NULL,
    created_by              UUID     NOT NULL,
    approved_by             UUID,

    -- ── Basic Info ─────────────────────────────────────────
    title                   VARCHAR(500)      NOT NULL,
    slug                    VARCHAR(500)      NOT NULL,
    category_id             UUID REFERENCES event_categories (id),
    event_type              TEXT     NOT NULL DEFAULT 'PHYSICAL',
    visibility              TEXT     NOT NULL DEFAULT 'PUBLIC',
    short_description       VARCHAR(300),
    description             TEXT,

    -- ── Status ─────────────────────────────────────────────
    status                  TEXT     NOT NULL DEFAULT 'DRAFT',
    approved_at             TIMESTAMPTZ,
    approval_notes          TEXT,
    rejection_reason        TEXT,

    -- ── Venue ──────────────────────────────────────────────
    venue_name              VARCHAR(255),
    venue_address           TEXT,
    venue_city              VARCHAR(100),
    venue_country           VARCHAR(100),
    venue_latitude          DECIMAL(10, 7),
    venue_longitude         DECIMAL(10, 7),
    online_event_url        TEXT,

    -- ── Date & Time ────────────────────────────────────────
    start_date              DATE     NOT NULL,
    start_time              TIME     NOT NULL,
    end_date                DATE     NOT NULL,
    end_time                TIME     NOT NULL,
    timezone                VARCHAR(100)      NOT NULL DEFAULT 'Africa/Nairobi',
    time_display            TEXT     NOT NULL DEFAULT 'START_AND_END',
    total_capacity          INTEGER,

    -- ── Media ──────────────────────────────────────────────
    banner_url              TEXT,
    logo_url                TEXT,

    -- ── Settings ───────────────────────────────────────────
    sales_start_at          TIMESTAMPTZ,
    sales_end_at            TIMESTAMPTZ,
    close_sales_at_capacity BOOLEAN  NOT NULL DEFAULT TRUE,
    min_tickets_per_order   INTEGER  NOT NULL DEFAULT 1,
    max_tickets_per_order   INTEGER,
    allow_group_purchases   BOOLEAN  NOT NULL DEFAULT TRUE,
    show_remaining_tickets  BOOLEAN  NOT NULL DEFAULT TRUE,
    allow_multiple_entries  BOOLEAN  NOT NULL DEFAULT FALSE,
    enable_checkins_staff   BOOLEAN  NOT NULL DEFAULT TRUE,
    min_age                 INTEGER,

    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE (organization_id, slug),

    CONSTRAINT chk_event_type   CHECK (event_type IN ('PHYSICAL', 'ONLINE', 'HYBRID')),
    CONSTRAINT chk_visibility   CHECK (visibility IN ('PUBLIC', 'PRIVATE')),
    CONSTRAINT chk_event_status CHECK (status IN ('DRAFT', 'PENDING', 'APPROVED', 'ONGOING', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_time_display CHECK (time_display IN ('START_AND_END', 'START_ONLY')),
    CONSTRAINT chk_event_dates  CHECK (
        end_date > start_date
            OR (end_date = start_date AND end_time >= start_time)
        )
);

-- M2M: events <-> tags
CREATE TABLE event_tags
(
    event_id UUID NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    tag_id   UUID NOT NULL REFERENCES tags (id) ON DELETE CASCADE,
    PRIMARY KEY (event_id, tag_id)
);

-- Media gallery
CREATE TABLE event_media
(
    id         UUID PRIMARY KEY     DEFAULT uuid_generate_v4(),
    event_id   UUID        NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    media_type VARCHAR(10) NOT NULL CHECK (media_type IN ('image', 'video')),
    url        TEXT        NOT NULL,
    caption    TEXT,
    sort_order INTEGER     NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Event managers
-- role 'OWNER'   → full control, mirrors the event creator
-- role 'MANAGER' → can edit event details, ticket types, promo codes
-- role 'STAFF'   → check-in access only
CREATE TABLE event_managers
(
    id          UUID PRIMARY KEY  DEFAULT uuid_generate_v4(),
    event_id    UUID     NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    user_id     UUID     NOT NULL,
    role        TEXT     NOT NULL DEFAULT 'MANAGER',
    invited_by  UUID     NOT NULL,
    invited_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    accepted_at TIMESTAMPTZ,
    is_active   BOOLEAN  NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE (event_id, user_id),
    CONSTRAINT chk_manager_role CHECK (role IN ('OWNER', 'MANAGER', 'STAFF'))
);


-- ============================================================
-- 4. TICKET TYPES
-- ============================================================

CREATE TABLE ticket_types
(
    id                UUID PRIMARY KEY        DEFAULT uuid_generate_v4(),
    event_id          UUID           NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    name              VARCHAR(255)   NOT NULL,
    description       TEXT,
    color             VARCHAR(7),
    sort_order        INTEGER        NOT NULL DEFAULT 0,
    total_capacity    INTEGER        NOT NULL,
    quantity_sold     INTEGER        NOT NULL DEFAULT 0,
    quantity_reserved INTEGER        NOT NULL DEFAULT 0,
    base_price        DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    is_group_ticket   BOOLEAN        NOT NULL DEFAULT FALSE,
    group_size        INTEGER,
    is_active         BOOLEAN        NOT NULL DEFAULT TRUE,
    is_hidden         BOOLEAN        NOT NULL DEFAULT FALSE,
    sales_start_at    TIMESTAMPTZ,
    sales_end_at      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_tt_sold_lte_capacity CHECK (quantity_sold <= total_capacity),
    CONSTRAINT chk_group_capacity CHECK (
        (is_group_ticket = FALSE) OR
        (is_group_ticket = TRUE AND total_capacity % group_size = 0)
        ),
    CONSTRAINT chk_group_size CHECK (
        (is_group_ticket = TRUE AND group_size IS NOT NULL AND group_size > 1) OR
        (is_group_ticket = FALSE AND group_size IS NULL)
        )
);


-- ============================================================
-- 5. PRICING TIERS
-- ============================================================

CREATE TABLE pricing_tiers
(
    id             UUID PRIMARY KEY  DEFAULT uuid_generate_v4(),
    ticket_type_id UUID     NOT NULL REFERENCES ticket_types (id) ON DELETE CASCADE,
    name           VARCHAR(255)      NOT NULL,
    price          DECIMAL(12, 2)    NOT NULL,
    quantity       INTEGER  NOT NULL,
    quantity_sold  INTEGER  NOT NULL DEFAULT 0,
    starts_at      TIMESTAMPTZ NOT NULL,
    ends_at        TIMESTAMPTZ NOT NULL,
    status         TEXT     NOT NULL DEFAULT 'UPCOMING',
    sort_order     INTEGER  NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_tier_dates       CHECK (ends_at > starts_at),
    CONSTRAINT chk_tier_sold_lte_qty CHECK (quantity_sold <= quantity),
    CONSTRAINT chk_tier_status      CHECK (status IN ('UPCOMING', 'ACTIVE', 'ENDED', 'SOLD_OUT', 'PAUSED'))
);


-- ============================================================
-- 6. PROMO CODES
-- ============================================================

CREATE TABLE promo_codes
(
    id               UUID PRIMARY KEY        DEFAULT uuid_generate_v4(),
    event_id         UUID           NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    code             VARCHAR(100)   NOT NULL,
    description      TEXT,
    discount_type    TEXT           NOT NULL,
    discount_value   DECIMAL(12, 2) NOT NULL,
    max_uses         INTEGER,
    uses_count       INTEGER        NOT NULL DEFAULT 0,
    min_order_amount DECIMAL(12, 2),
    starts_at        TIMESTAMPTZ,
    expires_at       TIMESTAMPTZ,
    is_active        BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    UNIQUE (event_id, code),
    CONSTRAINT chk_discount_type CHECK (discount_type IN ('PERCENTAGE', 'FIXED'))
);


-- ============================================================
-- 7. ORDERS
-- ============================================================

CREATE SEQUENCE order_number_seq START WITH 100 INCREMENT BY 1;

CREATE TABLE orders
(
    id                  UUID PRIMARY KEY  DEFAULT uuid_generate_v4(),
    event_id            UUID     NOT NULL REFERENCES events (id),
    organization_id     UUID     NOT NULL,
    customer_id         UUID,
    customer_first_name VARCHAR(100)      NOT NULL,
    customer_last_name  VARCHAR(100)      NOT NULL,
    customer_email      VARCHAR(255)      NOT NULL,
    customer_phone      VARCHAR(50),

    order_number        VARCHAR(50) UNIQUE NOT NULL
                                          DEFAULT 'ORD-' || LPAD(nextval('order_number_seq')::TEXT, 5, '0'),

    subtotal            DECIMAL(12, 2)    NOT NULL DEFAULT 0.00,
    discount_amount     DECIMAL(12, 2)    NOT NULL DEFAULT 0.00,
    total_amount        DECIMAL(12, 2)    NOT NULL,
    currency            VARCHAR(10)       NOT NULL DEFAULT 'KES',

    status              TEXT     NOT NULL DEFAULT 'PENDING',
    payment_status      TEXT     NOT NULL DEFAULT 'PENDING',
    payment_reference   VARCHAR(255),
    paid_at             TIMESTAMPTZ,

    promo_code_id       UUID REFERENCES promo_codes (id),
    is_group_order      BOOLEAN  NOT NULL DEFAULT FALSE,
    notes               TEXT,
    refunded_at         TIMESTAMPTZ,
    refund_reason       TEXT,

    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_order_status   CHECK (status IN ('PENDING', 'COMPLETED', 'CANCELLED', 'REFUNDED', 'PARTIALLY_REFUNDED')),
    CONSTRAINT chk_payment_status CHECK (payment_status IN ('PENDING', 'PAID', 'FAILED', 'REFUNDED'))
);


-- ============================================================
-- 8. ORDER ITEMS
-- ============================================================

CREATE TABLE order_items
(
    id              UUID PRIMARY KEY        DEFAULT uuid_generate_v4(),
    order_id        UUID           NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    ticket_type_id  UUID REFERENCES ticket_types (id),
    pricing_tier_id UUID REFERENCES pricing_tiers (id),
    item_name       VARCHAR(255)   NOT NULL,
    quantity        INTEGER        NOT NULL DEFAULT 1 CHECK (quantity > 0),
    unit_price      DECIMAL(12, 2) NOT NULL,
    total_price     DECIMAL(12, 2) NOT NULL,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);


-- ============================================================
-- 9. ATTENDEES  (one row = one ticket / QR code)
-- ============================================================

CREATE SEQUENCE ticket_number_seq START WITH 10000 INCREMENT BY 1;

CREATE TABLE attendees
(
    id             UUID PRIMARY KEY             DEFAULT uuid_generate_v4(),
    order_id       UUID                NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    order_item_id  UUID                NOT NULL REFERENCES order_items (id) ON DELETE CASCADE,
    ticket_type_id UUID                NOT NULL REFERENCES ticket_types (id),
    event_id       UUID                NOT NULL REFERENCES events (id),
    first_name     VARCHAR(100),
    last_name      VARCHAR(100),
    email          VARCHAR(255),
    phone          VARCHAR(50),

    ticket_number  VARCHAR(100) UNIQUE NOT NULL
                                       DEFAULT 'TKT-' || LPAD(nextval('ticket_number_seq')::TEXT, 7, '0'),
    qr_code_data   TEXT,
    barcode        VARCHAR(255),

    checked_in_at  TIMESTAMPTZ,
    checked_in_by  UUID,
    checked_out_at TIMESTAMPTZ,
    check_in_count INTEGER             NOT NULL DEFAULT 0,

    is_cancelled   BOOLEAN             NOT NULL DEFAULT FALSE,
    cancelled_at   TIMESTAMPTZ,

    created_at     TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ         NOT NULL DEFAULT NOW()
);


-- ============================================================
-- 10. CHECK-IN LOGS
-- ============================================================

CREATE TABLE check_in_logs
(
    id            UUID PRIMARY KEY  DEFAULT uuid_generate_v4(),
    attendee_id   UUID     NOT NULL REFERENCES attendees (id) ON DELETE CASCADE,
    event_id      UUID     NOT NULL REFERENCES events (id),
    staff_id      UUID,
    status        TEXT     NOT NULL,
    device_info   JSONB    NOT NULL DEFAULT '{}',
    location_info JSONB    NOT NULL DEFAULT '{}',
    notes         TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_checkin_status CHECK (status IN ('CHECKED_IN', 'CHECKED_OUT'))
);


-- ============================================================
-- 11. EVENT ANALYTICS SNAPSHOTS
-- ============================================================

CREATE TABLE event_analytics_snapshots
(
    id                       UUID PRIMARY KEY        DEFAULT uuid_generate_v4(),
    event_id                 UUID           NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    snapshot_date            DATE           NOT NULL,
    tickets_sold             INTEGER        NOT NULL DEFAULT 0,
    total_revenue            DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_checkins           INTEGER        NOT NULL DEFAULT 0,
    total_orders             INTEGER        NOT NULL DEFAULT 0,
    conversion_rate          DECIMAL(6, 2),
    avg_ticket_value         DECIMAL(12, 2),
    breakdown_by_ticket_type JSONB          NOT NULL DEFAULT '[]',
    created_at               TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    UNIQUE (event_id, snapshot_date)
);


-- ============================================================
-- 12. INDEXES
-- ============================================================

CREATE INDEX idx_categories_parent   ON event_categories (parent_id);
CREATE INDEX idx_categories_slug     ON event_categories (slug);

CREATE INDEX idx_events_org          ON events (organization_id);
CREATE INDEX idx_events_status       ON events (status);
CREATE INDEX idx_events_category     ON events (category_id);
CREATE INDEX idx_events_start_date   ON events (start_date);
CREATE INDEX idx_events_city         ON events (venue_city);
CREATE INDEX idx_events_fts          ON events USING GIN (
    to_tsvector('english',
        title || ' ' ||
        COALESCE(short_description, '') || ' ' ||
        COALESCE(venue_name, '') || ' ' ||
        COALESCE(venue_city, '')
    )
);

CREATE INDEX idx_managers_event      ON event_managers (event_id);
CREATE INDEX idx_managers_user       ON event_managers (user_id);
CREATE INDEX idx_managers_active     ON event_managers (event_id, is_active);

CREATE INDEX idx_tt_event            ON ticket_types (event_id);
CREATE INDEX idx_tt_active           ON ticket_types (event_id, is_active);

CREATE INDEX idx_pt_ticket_type      ON pricing_tiers (ticket_type_id);
CREATE INDEX idx_pt_status           ON pricing_tiers (status);
CREATE INDEX idx_pt_dates            ON pricing_tiers (starts_at, ends_at);

CREATE INDEX idx_orders_event        ON orders (event_id);
CREATE INDEX idx_orders_org          ON orders (organization_id);
CREATE INDEX idx_orders_customer     ON orders (customer_id);
CREATE INDEX idx_orders_number       ON orders (order_number);
CREATE INDEX idx_orders_status       ON orders (status, payment_status);
CREATE INDEX idx_orders_created      ON orders (created_at DESC);
CREATE INDEX idx_orders_email        ON orders (customer_email);

CREATE INDEX idx_oi_order            ON order_items (order_id);
CREATE INDEX idx_oi_ticket_type      ON order_items (ticket_type_id);

CREATE INDEX idx_att_event           ON attendees (event_id);
CREATE INDEX idx_att_order           ON attendees (order_id);
CREATE INDEX idx_att_ticket_number   ON attendees (ticket_number);
CREATE INDEX idx_att_email           ON attendees (email);
CREATE INDEX idx_att_checked_in      ON attendees (event_id, checked_in_at)
    WHERE checked_in_at IS NOT NULL;

CREATE INDEX idx_checkin_event       ON check_in_logs (event_id);
CREATE INDEX idx_checkin_attendee    ON check_in_logs (attendee_id);
CREATE INDEX idx_checkin_created     ON check_in_logs (created_at DESC);
CREATE INDEX idx_checkin_last_scan   ON check_in_logs (attendee_id, created_at DESC);

CREATE INDEX idx_analytics_event_date ON event_analytics_snapshots (event_id, snapshot_date DESC);


-- ============================================================
-- 13. TRIGGERS
-- ============================================================

-- auto updated_at
CREATE OR REPLACE FUNCTION fn_set_updated_at()
    RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

DO $$
    DECLARE
        t TEXT;
    BEGIN
        FOREACH t IN ARRAY ARRAY[
            'events', 'ticket_types', 'pricing_tiers',
            'promo_codes', 'orders', 'attendees', 'event_managers'
            ]
            LOOP
                EXECUTE format(
                    'CREATE TRIGGER trg_%I_updated_at
                     BEFORE UPDATE ON %I
                     FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();',
                    t, t
                );
            END LOOP;
    END;
$$;

-- auto-compute pricing_tier.status on save
CREATE OR REPLACE FUNCTION fn_pricing_tier_status()
    RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.status = CASE
        WHEN NEW.quantity_sold >= NEW.quantity           THEN 'SOLD_OUT'
        WHEN NOW() < NEW.starts_at                       THEN 'UPCOMING'
        WHEN NOW() BETWEEN NEW.starts_at AND NEW.ends_at THEN 'ACTIVE'
        ELSE 'ENDED'
    END;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_pricing_tier_status
    BEFORE INSERT OR UPDATE ON pricing_tiers
    FOR EACH ROW EXECUTE FUNCTION fn_pricing_tier_status();

-- sync quantities + promo usage when order completes
CREATE OR REPLACE FUNCTION fn_sync_on_order_complete()
    RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.status = 'COMPLETED' AND OLD.status IS DISTINCT FROM 'COMPLETED' THEN

        UPDATE ticket_types tt
        SET quantity_sold = quantity_sold + agg.qty
        FROM (SELECT ticket_type_id, SUM(quantity) AS qty
              FROM order_items
              WHERE order_id = NEW.id AND ticket_type_id IS NOT NULL
              GROUP BY ticket_type_id) agg
        WHERE tt.id = agg.ticket_type_id;

        UPDATE pricing_tiers pt
        SET quantity_sold = quantity_sold + agg.qty
        FROM (SELECT pricing_tier_id, SUM(quantity) AS qty
              FROM order_items
              WHERE order_id = NEW.id AND pricing_tier_id IS NOT NULL
              GROUP BY pricing_tier_id) agg
        WHERE pt.id = agg.pricing_tier_id;

        UPDATE promo_codes SET uses_count = uses_count + 1
        WHERE id = NEW.promo_code_id AND NEW.promo_code_id IS NOT NULL;

    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_order_complete_sync
    AFTER UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION fn_sync_on_order_complete();


-- ============================================================
-- 14. VIEWS
-- ============================================================

CREATE OR REPLACE VIEW vw_event_list AS
SELECT e.id, e.organization_id, e.title, e.slug, e.status, e.event_type,
       e.start_date, e.start_time, e.end_date, e.end_time,
       e.venue_name, e.venue_city, e.total_capacity,
       COALESCE(SUM(tt.quantity_sold), 0) AS tickets_sold,
       CASE WHEN e.total_capacity > 0
            THEN ROUND(SUM(tt.quantity_sold)::DECIMAL / e.total_capacity * 100, 1)
            ELSE 0 END AS sold_pct,
       COALESCE(SUM(oi.total_price) FILTER (WHERE o.status = 'COMPLETED'), 0) AS total_revenue
FROM events e
         LEFT JOIN ticket_types tt ON tt.event_id = e.id
         LEFT JOIN order_items oi ON oi.ticket_type_id = tt.id
         LEFT JOIN orders o ON o.id = oi.order_id
GROUP BY e.id;

CREATE OR REPLACE VIEW vw_event_kpis AS
SELECT e.id AS event_id, e.title, e.status, e.total_capacity,
       COALESCE(SUM(tt.quantity_sold), 0) AS tickets_sold,
       COALESCE(SUM(oi.total_price) FILTER (WHERE o.status = 'COMPLETED'), 0) AS revenue,
       COUNT(a.id) FILTER (WHERE a.checked_in_at IS NOT NULL) AS total_checkins,
       ROUND(100.0 * COUNT(DISTINCT o.id) FILTER (WHERE o.status = 'COMPLETED')
           / NULLIF(COUNT(DISTINCT o.id), 0), 1) AS conversion_rate_pct
FROM events e
         LEFT JOIN ticket_types tt ON tt.event_id = e.id
         LEFT JOIN order_items oi ON oi.ticket_type_id = tt.id
         LEFT JOIN orders o ON o.id = oi.order_id
         LEFT JOIN attendees a ON a.event_id = e.id
GROUP BY e.id;

CREATE OR REPLACE VIEW vw_active_pricing AS
SELECT DISTINCT ON (ticket_type_id)
    ticket_type_id, id AS pricing_tier_id, name AS tier_name,
    price, quantity - quantity_sold AS remaining, starts_at, ends_at, status
FROM pricing_tiers
WHERE status = 'ACTIVE'
ORDER BY ticket_type_id, price ASC;

CREATE OR REPLACE VIEW vw_attendee_gate_status AS
SELECT a.id AS attendee_id, a.event_id, a.ticket_number, a.qr_code_data,
       a.ticket_type_id, a.is_cancelled, a.check_in_count,
       last_scan.status AS last_scan_status, last_scan.created_at AS last_scan_at,
       last_scan.staff_id AS last_scanned_by,
       CASE
           WHEN a.is_cancelled                   THEN FALSE
           WHEN last_scan.status IS NULL          THEN TRUE
           WHEN last_scan.status = 'CHECKED_OUT'  THEN TRUE
           WHEN last_scan.status = 'CHECKED_IN'   THEN FALSE
           ELSE FALSE
       END AS can_enter
FROM attendees a
         LEFT JOIN LATERAL (
    SELECT status, created_at, staff_id FROM check_in_logs
    WHERE attendee_id = a.id ORDER BY created_at DESC LIMIT 1
    ) last_scan ON TRUE;

CREATE OR REPLACE VIEW vw_attendee_checkins AS
SELECT a.id AS attendee_id, a.event_id, a.ticket_number,
       a.first_name, a.last_name, a.email,
       tt.name AS ticket_type_name, o.order_number,
       a.checked_in_at, a.check_in_count, a.checked_in_by AS checked_in_by_user_id
FROM attendees a
         JOIN ticket_types tt ON tt.id = a.ticket_type_id
         JOIN orders o ON o.id = a.order_id
WHERE a.is_cancelled = FALSE;


-- ============================================================
-- 15. SEED DATA
-- ============================================================

INSERT INTO event_categories (name, slug, sort_order)
VALUES ('Music', 'music', 1),
       ('Comedy', 'comedy', 2),
       ('Food & Drink', 'food-drink', 3),
       ('Sports', 'sports', 4),
       ('Arts', 'arts', 5),
       ('Business', 'business', 6),
       ('Technology', 'technology', 7),
       ('Education', 'education', 8),
       ('Community', 'community', 9)
ON CONFLICT (parent_id, slug) DO NOTHING;

INSERT INTO event_categories (parent_id, name, slug, sort_order)
SELECT p.id, s.name, s.slug, s.ord
FROM (VALUES ('music',      'Concert',       'concert',      1),
             ('music',      'Festival',      'festival',     2),
             ('music',      'DJ Night',      'dj-night',     3),
             ('music',      'Jazz & Blues',  'jazz-blues',   4),
             ('comedy',     'Stand-up',      'stand-up',     1),
             ('comedy',     'Comedy Show',   'comedy-show',  2),
             ('food-drink', 'Food Festival', 'food-festival',1),
             ('food-drink', 'Brunch',        'brunch',       2),
             ('food-drink', 'Wine Tasting',  'wine-tasting', 3),
             ('sports',     'Basketball',    'basketball',   1),
             ('sports',     'Football',      'football',     2),
             ('arts',       'Exhibition',    'exhibition',   1),
             ('arts',       'Theater',       'theater',      2),
             ('technology', 'Hackathon',     'hackathon',    1),
             ('technology', 'Conference',    'conference',   2)) AS s(parent_slug, name, slug, ord)
         JOIN event_categories p ON p.slug = s.parent_slug AND p.parent_id IS NULL
ON CONFLICT DO NOTHING;

-- ============================================================
-- END OF MIGRATION
-- Tables (13):
--   event_categories, tags, events, event_tags, event_media,
--   event_managers, ticket_types, pricing_tiers, promo_codes,
--   orders, order_items, attendees, check_in_logs,
--   event_analytics_snapshots
-- ============================================================
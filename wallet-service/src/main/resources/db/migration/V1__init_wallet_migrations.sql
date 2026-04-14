CREATE TABLE acc_account_types
(
    id                    SERIAL PRIMARY KEY,
    account_prefix        VARCHAR(5)     DEFAULT NULL UNIQUE,
    account_number_length INTEGER        DEFAULT NULL,
    min_accounts          INTEGER        DEFAULT 0,
    max_accounts          INTEGER        DEFAULT 1,
    type_name             VARCHAR(150)   DEFAULT NULL,
    yearly_limit          NUMERIC(19, 4) DEFAULT 0,
    min_balance_limit     NUMERIC(19, 4) DEFAULT 0,
    max_balance_limit     NUMERIC(19, 4) DEFAULT 0,
    can_overdraw          BOOLEAN        DEFAULT FALSE,
    overdraw_limit        NUMERIC(19, 4) DEFAULT 0,
    category              VARCHAR(20) CHECK (category IN ('ASSET', 'LIABILITY', 'WALLET')),
    description           VARCHAR(32)    DEFAULT NULL,
    status                SMALLINT       DEFAULT NULL,
    created_by            VARCHAR(512)   DEFAULT NULL,
    created_on            TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_on            TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    update_by             VARCHAR(512)   DEFAULT NULL,
    deleted_on            TIMESTAMP      DEFAULT NULL,
    deleted_by            VARCHAR(512)   DEFAULT NULL,
    account_pan_enabled   BOOLEAN        DEFAULT FALSE
);

-- System Currencies
CREATE TABLE sys_currencies
(
    id            SERIAL PRIMARY KEY,
    currency_name VARCHAR(100) DEFAULT NULL,
    currency_code VARCHAR(4)   DEFAULT NULL UNIQUE,
    iso_code      VARCHAR(4)   DEFAULT NULL,
    status        SMALLINT     DEFAULT NULL,
    created_by    VARCHAR(512) DEFAULT NULL,
    created_on    TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP,
    updated_on    TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP,
    update_by     VARCHAR(512) DEFAULT NULL,
    deleted_on    TIMESTAMPTZ  DEFAULT NULL,
    deleted_by    VARCHAR(512) DEFAULT NULL
);


-- Customer Accounts
CREATE TABLE acc_accounts
(
    id                BIGSERIAL PRIMARY KEY,
    account_number    VARCHAR(45)    DEFAULT NULL UNIQUE,
    phone_number      VARCHAR(15)    DEFAULT NULL,
    opening_date      TIMESTAMPTZ    DEFAULT CURRENT_TIMESTAMP,
    opening_balance   NUMERIC(19, 4) DEFAULT 10000,
    actual_balance    NUMERIC(19, 4) DEFAULT 10000,
    available_balance NUMERIC(19, 4) DEFAULT 0,
    account_type_id   INTEGER        DEFAULT NULL,
    currency_id       INTEGER        DEFAULT NULL,
    account_name      VARCHAR(45)    DEFAULT NULL,
    allow_dr          BOOLEAN        DEFAULT NULL,
    allow_cr          BOOLEAN        DEFAULT NULL,
    blocked           BOOLEAN        DEFAULT NULL,
    dormant           BOOLEAN        DEFAULT NULL,
    status            SMALLINT       DEFAULT NULL,
    created_by        VARCHAR(512)   DEFAULT NULL,
    created_on        TIMESTAMPTZ    DEFAULT CURRENT_TIMESTAMP,
    updated_on        TIMESTAMPTZ    DEFAULT CURRENT_TIMESTAMP,
    update_by         VARCHAR(512)   DEFAULT NULL,
    deleted_on        TIMESTAMPTZ    DEFAULT NULL,
    deleted_by        VARCHAR(512)   DEFAULT NULL,
    account_pan       VARCHAR(80)    DEFAULT NULL,

    CONSTRAINT fk_account_type
        FOREIGN KEY (account_type_id) REFERENCES acc_account_types (id) ON DELETE SET NULL,
    CONSTRAINT fk_currency
        FOREIGN KEY (currency_id) REFERENCES sys_currencies (id) ON DELETE SET NULL
);

-- Indexes (equivalent to MySQL KEY declarations)
CREATE INDEX idx_acc_accounts_account_type_id ON acc_accounts (account_type_id);
CREATE INDEX idx_acc_accounts_currency_id ON acc_accounts (currency_id);

-- System Services
CREATE TABLE sys_services
(
    id               SERIAL PRIMARY KEY,
    transaction_type VARCHAR(80)  DEFAULT NULL UNIQUE,
    is_bill          BOOLEAN      DEFAULT FALSE,
    is_enquiry       BOOLEAN      DEFAULT FALSE,
    status           SMALLINT     DEFAULT 1,
    description      TEXT         DEFAULT NULL,
    created_by       VARCHAR(512) DEFAULT NULL,
    created_on       TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP,
    updated_on       TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP,
    update_by        VARCHAR(512) DEFAULT NULL,
    deleted_on       TIMESTAMPTZ  DEFAULT NULL,
    deleted_by       VARCHAR(512) DEFAULT NULL
);

CREATE TABLE trx_messages
(
    id                     BIGSERIAL PRIMARY KEY,
    transaction_ref        VARCHAR(80)   DEFAULT NULL,
    channel_timestamp      VARCHAR(60)   DEFAULT NULL,
    channel_reference      VARCHAR(45)   DEFAULT NULL,
    channel_ip             VARCHAR(32)   DEFAULT NULL,
    geolocation            VARCHAR(60)   DEFAULT NULL,
    user_agent             VARCHAR(60)   DEFAULT NULL,
    user_agent_version     VARCHAR(45)   DEFAULT NULL,
    channel                VARCHAR(45)   DEFAULT NULL,
    client_id              VARCHAR(45)   DEFAULT NULL,
    transaction_code       VARCHAR(80)   DEFAULT NULL,
    transaction_type       VARCHAR(80)   DEFAULT NULL,
    host_code              VARCHAR(10)   DEFAULT NULL,
    direction              VARCHAR(80)   DEFAULT NULL,
    amount                 NUMERIC(19,4) DEFAULT 0,
    total_charge           NUMERIC(19,4) DEFAULT 0,
    phone_number           VARCHAR(20)   DEFAULT NULL,
    debit_account          VARCHAR(80)   DEFAULT NULL,
    credit_account         VARCHAR(80)   DEFAULT NULL,
    bill_account_number    VARCHAR(30)   DEFAULT NULL,
    response_code          VARCHAR(4)    DEFAULT NULL,
    response_message       TEXT          DEFAULT NULL,
    service_status         SMALLINT      DEFAULT NULL,
    recipient_phone_number VARCHAR(20)   DEFAULT NULL,
    merchant_code          VARCHAR(15)   DEFAULT NULL,
    agent_code             VARCHAR(15)   DEFAULT NULL,
    currency               VARCHAR(20)   DEFAULT NULL,
    reversed               SMALLINT      DEFAULT NULL,
    receipt_number         VARCHAR(60)   DEFAULT NULL,
    callback_address       VARCHAR(255)  DEFAULT NULL,
    reversed_at            TIMESTAMPTZ   DEFAULT NULL,
    service_management_id  INTEGER       DEFAULT NULL,
    status                 SMALLINT      DEFAULT NULL,
    created_by             VARCHAR(512)  DEFAULT NULL,
    created_on             TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP,
    updated_on             TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP,
    update_by              VARCHAR(512)  DEFAULT NULL,
    deleted_on             TIMESTAMPTZ   DEFAULT NULL,
    deleted_by             VARCHAR(512)  DEFAULT NULL,

    CONSTRAINT fk_trx_service_management
        FOREIGN KEY (service_management_id) REFERENCES cfg_service_management (id) ON DELETE SET NULL
);

CREATE INDEX idx_trx_messages_service_management_id ON trx_messages (service_management_id);


CREATE TABLE trx_transaction_charges
(
    id             SERIAL        PRIMARY KEY,
    esb_ref        BIGINT        DEFAULT NULL,
    charge_id      INTEGER       DEFAULT NULL,
    charge_type    VARCHAR(20)   DEFAULT NULL,
    charge_value   NUMERIC(19,4) DEFAULT NULL,
    amount         NUMERIC(19,4) DEFAULT NULL,
    status_code    VARCHAR(10)   DEFAULT NULL,
    status_message VARCHAR(512)  DEFAULT NULL,
    total_charge   NUMERIC(19,4) DEFAULT NULL,

    CONSTRAINT fk_trx_charges_esb_ref
        FOREIGN KEY (esb_ref) REFERENCES trx_messages (id) ON DELETE SET NULL,
    CONSTRAINT fk_trx_charges_charge_id
        FOREIGN KEY (charge_id) REFERENCES cfg_changes (id) ON DELETE SET NULL
);

CREATE INDEX idx_trx_transaction_charges_esb_ref   ON trx_transaction_charges (esb_ref);
CREATE INDEX idx_trx_transaction_charges_charge_id ON trx_transaction_charges (charge_id);

CREATE TABLE trx_transaction_entries
(
    id                       SERIAL        PRIMARY KEY,
    esb_ref                  BIGINT        DEFAULT NULL,
    charge_id                BIGINT        DEFAULT NULL,
    reversed                 BOOLEAN       DEFAULT FALSE,
    account_number           VARCHAR(80)   DEFAULT NULL,
    actual_balance_before    NUMERIC(19,4) DEFAULT NULL,
    available_balance_before NUMERIC(19,4) DEFAULT NULL,
    amount                   NUMERIC(19,4) DEFAULT NULL,
    actual_balance_after     NUMERIC(19,4) DEFAULT NULL,
    available_balance_after  NUMERIC(19,4) DEFAULT NULL,
    currency                 VARCHAR(3)    DEFAULT NULL,
    narration                VARCHAR(150)  DEFAULT NULL,
    dr_cr                    VARCHAR(10)   DEFAULT NULL,
    is_balance_updated       BOOLEAN       DEFAULT NULL,
    ledger_code              VARCHAR(80)   DEFAULT NULL,
    status                   SMALLINT      DEFAULT NULL,
    created_by               VARCHAR(512)  DEFAULT NULL,
    created_on               TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP,
    updated_on               TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP,
    update_by                VARCHAR(512)  DEFAULT NULL,
    deleted_on               TIMESTAMPTZ   DEFAULT NULL,
    deleted_by               VARCHAR(512)  DEFAULT NULL,

    CONSTRAINT fk_trx_entries_esb_ref
        FOREIGN KEY (esb_ref) REFERENCES trx_messages (id) ON DELETE SET NULL
);

CREATE INDEX idx_trx_transaction_entries_esb_ref ON trx_transaction_entries (esb_ref);

CREATE TABLE cfg_changes
(
    id                    SERIAL        PRIMARY KEY,
    min_amount            NUMERIC(19,4) DEFAULT NULL,
    max_amount            NUMERIC(19,4) DEFAULT NULL,
    charge_value          NUMERIC(19,4) DEFAULT NULL,
    value_type            VARCHAR(10)   CHECK (value_type IN ('FIXED', 'PERCENTAGE')),
    charge_type           VARCHAR(10)   CHECK (charge_type IN ('VAT', 'CHARGE', 'TAX')),
    tax_account           VARCHAR(80)   DEFAULT NULL,
    account_id            BIGINT        DEFAULT NULL,
    ledger_account_id     INTEGER       DEFAULT NULL,
    service_management_id INTEGER       DEFAULT NULL,
    receiver_narration    TEXT          DEFAULT NULL,
    sender_narration      TEXT          DEFAULT NULL,
    status                SMALLINT      DEFAULT NULL,
    created_by            VARCHAR(512)  DEFAULT NULL,
    created_on            TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP,
    updated_on            TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP,
    update_by             VARCHAR(512)  DEFAULT NULL,
    deleted_on            TIMESTAMPTZ   DEFAULT NULL,
    deleted_by            VARCHAR(512)  DEFAULT NULL,

    CONSTRAINT fk_cfg_changes_service_management
        FOREIGN KEY (service_management_id) REFERENCES cfg_service_management (id) ON DELETE SET NULL,
    CONSTRAINT fk_cfg_changes_account
        FOREIGN KEY (account_id) REFERENCES acc_accounts (id) ON DELETE SET NULL
);

CREATE INDEX idx_cfg_changes_service_management_id ON cfg_changes (service_management_id);
CREATE INDEX idx_cfg_changes_account_id            ON cfg_changes (account_id);
CREATE INDEX idx_cfg_changes_ledger_account_id     ON cfg_changes (ledger_account_id);


CREATE TABLE cfg_service_management
(
    id                    SERIAL        PRIMARY KEY,
    service_id            INTEGER       DEFAULT NULL,
    external_service_id   VARCHAR(15)   DEFAULT NULL,
    ledger_account_id     INTEGER       DEFAULT NULL,
    account_id            BIGINT        DEFAULT NULL,
    channel_id            INTEGER       DEFAULT NULL,
    request_direction_id  INTEGER       DEFAULT NULL,
    service_code          VARCHAR(80)   DEFAULT NULL,
    receiver_narration    VARCHAR(80)   DEFAULT NULL,
    sender_narration      VARCHAR(80)   DEFAULT NULL,
    last_hour             NUMERIC(19,4) DEFAULT NULL,
    daily_limit           NUMERIC(19,4) DEFAULT 0,
    weekly_limit          NUMERIC(19,4) DEFAULT 0,
    monthly_limit         NUMERIC(19,4) DEFAULT 0,
    description           TEXT          DEFAULT NULL,
    status                SMALLINT      DEFAULT NULL,
    created_by            VARCHAR(512)  DEFAULT NULL,
    created_on            TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP,
    updated_on            TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP,
    update_by             VARCHAR(512)  DEFAULT NULL,
    deleted_on            TIMESTAMPTZ   DEFAULT NULL,
    deleted_by            VARCHAR(512)  DEFAULT NULL,

    CONSTRAINT fk_cfg_svc_mgmt_service
        FOREIGN KEY (service_id) REFERENCES sys_services (id) ON DELETE SET NULL,
    CONSTRAINT fk_cfg_svc_mgmt_account
        FOREIGN KEY (account_id) REFERENCES acc_accounts (id) ON DELETE SET NULL,
    CONSTRAINT fk_cfg_svc_mgmt_channel
        FOREIGN KEY (channel_id) REFERENCES chn_channels (id) ON DELETE SET NULL
);

CREATE INDEX idx_cfg_svc_mgmt_service_id           ON cfg_service_management (service_id);
CREATE INDEX idx_cfg_svc_mgmt_account_id           ON cfg_service_management (account_id);
CREATE INDEX idx_cfg_svc_mgmt_channel_id           ON cfg_service_management (channel_id);
CREATE INDEX idx_cfg_svc_mgmt_ledger_account_id    ON cfg_service_management (ledger_account_id);
CREATE INDEX idx_cfg_svc_mgmt_request_direction_id ON cfg_service_management (request_direction_id);



CREATE TABLE tb_gl_service_mapping
(
    id               SERIAL       PRIMARY KEY,
    account_number   VARCHAR(20)  DEFAULT NULL,
    branch_code      VARCHAR(10)  DEFAULT NULL,
    bank_code        VARCHAR(20)  DEFAULT NULL,
    beneficiary_name VARCHAR(30)  DEFAULT NULL,
    service_id       INTEGER      DEFAULT NULL,
    created_on       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       VARCHAR(45)  NOT NULL
);

CREATE TABLE chn_channels
(
    id           SERIAL       PRIMARY KEY,
    channel_name VARCHAR(512) DEFAULT NULL UNIQUE,
    client_id    VARCHAR(512) DEFAULT NULL,
    channel_key  TEXT         DEFAULT NULL,
    host_name    VARCHAR(150) DEFAULT NULL,
    host_ip      VARCHAR(32)  DEFAULT NULL,
    description  TEXT         DEFAULT NULL,
    status       SMALLINT     DEFAULT 1,
    created_by   VARCHAR(512) DEFAULT NULL,
    created_on   TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP,
    updated_on   TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP,
    update_by    VARCHAR(512) DEFAULT NULL,
    deleted_on   TIMESTAMPTZ  DEFAULT NULL,
    deleted_by   VARCHAR(512) DEFAULT NULL
);
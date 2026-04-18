CREATE TABLE trx_outgoing_requests (
    id               BIGSERIAL PRIMARY KEY,
    reference_id     VARCHAR(256),
    service_code     VARCHAR(100)  NOT NULL,
    endpoint         VARCHAR(512)  NOT NULL,
    request_payload  TEXT,
    response_payload TEXT,
    response_code    VARCHAR(20),
    status           VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    error_message    TEXT,
    created_on       TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_on       TIMESTAMPTZ
);

CREATE INDEX idx_outgoing_req_reference ON trx_outgoing_requests (reference_id);
CREATE INDEX idx_outgoing_req_status    ON trx_outgoing_requests (status);
CREATE INDEX idx_outgoing_req_service   ON trx_outgoing_requests (service_code);

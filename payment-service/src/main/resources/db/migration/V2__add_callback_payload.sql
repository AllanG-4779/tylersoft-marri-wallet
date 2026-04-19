ALTER TABLE trx_outgoing_requests
    ADD COLUMN callback_payload     TEXT,
    ADD COLUMN callback_received_on TIMESTAMPTZ;

-- Prevent duplicate ticket type names within the same event.
ALTER TABLE ticket_types
    ADD CONSTRAINT uq_ticket_type_event_name UNIQUE (event_id, name);

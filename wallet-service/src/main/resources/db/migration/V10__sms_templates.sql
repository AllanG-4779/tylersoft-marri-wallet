-- =============================================================
-- V10: SMS message templates per transaction type, code, direction and status
--
-- direction : SENDER   — message sent to the debit account holder
--             RECEIVER — message sent to the credit account holder
-- status    : SUCCESS, FAILURE  (extend as needed, e.g. PENDING, CALLBACK_WAIT)
--
-- Placeholders: {amount}, {currency}, {balance}, {ref}, {recipient}
-- =============================================================

CREATE TABLE cfg_sms_templates
(
    id               SERIAL       PRIMARY KEY,
    transaction_type VARCHAR(80)  NOT NULL,
    transaction_code VARCHAR(80)  NOT NULL,
    direction        VARCHAR(10)  NOT NULL CHECK (direction IN ('SENDER', 'RECEIVER')),
    status           VARCHAR(30)  NOT NULL,
    template         VARCHAR(500) NOT NULL,
    created_by       VARCHAR(512) DEFAULT 'system',
    created_on       TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP,
    updated_on       TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_sms_template UNIQUE (transaction_type, transaction_code, direction, status)
);

-- ── Fund Transfer (FT) ────────────────────────────────────────────────────────
INSERT INTO cfg_sms_templates (transaction_type, transaction_code, direction, status, template) VALUES
('FT', 'FT', 'SENDER',   'SUCCESS', 'Your transfer of {currency} {amount} was successful. New balance: {currency} {balance}. Ref: {ref}.'),
('FT', 'FT', 'RECEIVER', 'SUCCESS', 'You have received {currency} {amount} in your wallet. Ref: {ref}.'),
('FT', 'FT', 'SENDER',   'FAILURE', 'Your transfer of {currency} {amount} could not be processed. Ref: {ref}.');

-- ── Airtime ───────────────────────────────────────────────────────────────────
INSERT INTO cfg_sms_templates (transaction_type, transaction_code, direction, status, template) VALUES
('AIRTIME', 'AIRTIME', 'SENDER', 'SUCCESS', 'Airtime of {currency} {amount} sent to {recipient} successfully. New balance: {currency} {balance}. Ref: {ref}.'),
('AIRTIME', 'AIRTIME', 'SENDER', 'FAILURE', 'Airtime purchase of {currency} {amount} for {recipient} failed. Ref: {ref}.');

-- Orange airtime
INSERT INTO cfg_sms_templates (transaction_type, transaction_code, direction, status, template) VALUES
('AIRTIME', 'ORANGE', 'SENDER', 'SUCCESS', 'Orange airtime of {currency} {amount} sent to {recipient} successfully. New balance: {currency} {balance}. Ref: {ref}.'),
('AIRTIME', 'ORANGE', 'SENDER', 'FAILURE', 'Orange airtime of {currency} {amount} for {recipient} could not be processed. Ref: {ref}.');

-- ── Deposit ───────────────────────────────────────────────────────────────────
INSERT INTO cfg_sms_templates (transaction_type, transaction_code, direction, status, template) VALUES
('DEPOSIT', 'DEPOSIT', 'SENDER', 'SUCCESS', 'A deposit of {currency} {amount} has been received. New balance: {currency} {balance}. Ref: {ref}.'),
('DEPOSIT', 'DEPOSIT', 'SENDER', 'FAILURE', 'Deposit of {currency} {amount} failed. Ref: {ref}.');

-- Card top-up deposit (all relevant statuses)
INSERT INTO cfg_sms_templates (transaction_type, transaction_code, direction, status, template) VALUES
('DEPOSIT', 'CARD', 'SENDER', 'SUCCESS',         'Your card top-up of {currency} {amount} was successful. New balance: {currency} {balance}. Ref: {ref}.'),
('DEPOSIT', 'CARD', 'SENDER', 'FAILURE',         'Your card top-up of {currency} {amount} could not be processed. Ref: {ref}.'),
('DEPOSIT', 'CARD', 'SENDER', 'CALLBACK_WAIT',   'Your card top-up of {currency} {amount} is being processed. Ref: {ref}.'),
('DEPOSIT', 'CARD', 'SENDER', 'FAILED_CALLBACK', 'Your card top-up of {currency} {amount} was not confirmed by the payment provider. Ref: {ref}.');

-- ── Withdrawal ────────────────────────────────────────────────────────────────
INSERT INTO cfg_sms_templates (transaction_type, transaction_code, direction, status, template) VALUES
('WITHDRAWAL', 'WITHDRAWAL', 'SENDER', 'SUCCESS', 'Withdrawal of {currency} {amount} successful. Remaining balance: {currency} {balance}. Ref: {ref}.'),
('WITHDRAWAL', 'WITHDRAWAL', 'SENDER', 'FAILURE', 'Withdrawal of {currency} {amount} could not be processed. Ref: {ref}.');

-- ── Bill Payment ──────────────────────────────────────────────────────────────
INSERT INTO cfg_sms_templates (transaction_type, transaction_code, direction, status, template) VALUES
('BILL', 'BILL', 'SENDER', 'SUCCESS', 'Bill payment of {currency} {amount} to {recipient} was successful. New balance: {currency} {balance}. Ref: {ref}.'),
('BILL', 'BILL', 'SENDER', 'FAILURE', 'Bill payment of {currency} {amount} to {recipient} failed. Ref: {ref}.');

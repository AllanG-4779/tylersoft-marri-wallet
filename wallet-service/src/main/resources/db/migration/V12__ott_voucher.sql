
-- 6. SMS templates
INSERT INTO cfg_sms_templates (transaction_type, transaction_code, direction, status, template) VALUES
('OTT_VOUCHER', 'OTT', 'SENDER', 'SUCCESS',
    'OTT voucher of {currency} {amount} purchased for {recipient}. New balance: {currency} {balance}. Ref: {ref}.'),
('OTT_VOUCHER', 'OTT', 'SENDER', 'FAILURE',
    'OTT voucher purchase of {currency} {amount} could not be processed. Ref: {ref}.')
ON CONFLICT (transaction_type, transaction_code, direction, status) DO NOTHING;

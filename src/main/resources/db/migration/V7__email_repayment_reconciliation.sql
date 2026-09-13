ALTER TABLE payment_notification
    ADD COLUMN principal_amount DECIMAL(19,2) NULL AFTER reported_amount,
    ADD COLUMN interest_amount DECIMAL(19,2) NULL AFTER principal_amount,
    ADD COLUMN lending_type VARCHAR(50) NULL AFTER interest_amount,
    ADD COLUMN account_last4 VARCHAR(4) NULL AFTER lending_type,
    ADD COLUMN transaction_reference VARCHAR(160) NULL AFTER account_last4,
    ADD COLUMN amount_validation_status VARCHAR(30) NULL AFTER transaction_reference,
    ADD INDEX ix_notification_reconciliation (user_id, notification_type, notification_date);

ALTER TABLE bank_credit
    ADD INDEX ix_bank_credit_reference (user_id, bank_transaction_reference);

ALTER TABLE reconciliation_record
    ADD COLUMN bank_payment_notification_id BIGINT NULL AFTER payment_notification_id,
    ADD CONSTRAINT uk_reconciliation_notification UNIQUE (user_id, payment_notification_id),
    ADD CONSTRAINT uk_reconciliation_bank_notification UNIQUE (user_id, bank_payment_notification_id);

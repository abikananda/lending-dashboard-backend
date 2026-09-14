ALTER TABLE payment_notification
    ADD COLUMN principal_amount DECIMAL(19,2) NULL AFTER reported_amount,
    ADD COLUMN interest_amount DECIMAL(19,2) NULL AFTER principal_amount,
    ADD COLUMN lending_type VARCHAR(50) NULL AFTER interest_amount,
    ADD COLUMN account_last4 VARCHAR(4) NULL AFTER lending_type,
    ADD COLUMN transaction_reference VARCHAR(160) NULL AFTER account_last4,
    ADD COLUMN amount_validation_status VARCHAR(30) NULL AFTER transaction_reference,
    ADD COLUMN reconciliation_account_id BIGINT NULL AFTER user_id,
    ADD INDEX ix_notification_reconciliation (user_id, notification_type, notification_date);

ALTER TABLE bank_credit
    ADD INDEX ix_bank_credit_reference (user_id, bank_transaction_reference);

ALTER TABLE reconciliation_record
    ADD COLUMN bank_payment_notification_id BIGINT NULL AFTER payment_notification_id,
    ADD CONSTRAINT uk_reconciliation_notification UNIQUE (user_id, payment_notification_id),
    ADD CONSTRAINT uk_reconciliation_bank_notification UNIQUE (user_id, bank_payment_notification_id);

CREATE TABLE email_reconciliation_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    label VARCHAR(100) NOT NULL,
    lender_id VARCHAR(64) NOT NULL,
    mailbox_email VARCHAR(255) NOT NULL,
    encrypted_app_password VARCHAR(1000) NOT NULL,
    bank_sender VARCHAR(255) NOT NULL,
    bank_account_last4 VARCHAR(4) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_email_account_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_email_account_lender UNIQUE (user_id, lender_id),
    CONSTRAINT uk_email_account_bank UNIQUE (user_id, bank_account_last4),
    INDEX ix_email_account_enabled (enabled, user_id)
);

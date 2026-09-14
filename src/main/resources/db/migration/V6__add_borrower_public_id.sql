ALTER TABLE loan_report_staging
    ADD COLUMN borrower_public_id VARCHAR(36) NULL AFTER loan_id,
    ADD INDEX ix_stage_borrower_public_id (borrower_public_id);

ALTER TABLE manual_lending
    ADD COLUMN borrower_public_id VARCHAR(36) NULL AFTER loan_id,
    ADD INDEX ix_manual_borrower_public_id (user_id, borrower_public_id);

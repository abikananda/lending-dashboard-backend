ALTER TABLE loan_report_staging
    ADD COLUMN reported_npa_amount DECIMAL(19,2) NULL AFTER reported_interest_received;

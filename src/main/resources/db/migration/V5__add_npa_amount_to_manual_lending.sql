ALTER TABLE manual_lending
    ADD COLUMN npa_amount DECIMAL(19,2) NOT NULL DEFAULT 0.00 AFTER outstanding_principal;

UPDATE manual_lending
SET npa_amount = GREATEST(invested_amount - calculated_principal_received, 0.00)
WHERE is_npa = TRUE;

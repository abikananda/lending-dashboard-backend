ALTER TABLE manual_lending
    ADD COLUMN npa_amount DECIMAL(19,2) NOT NULL DEFAULT 0.00 AFTER outstanding_principal;

UPDATE manual_lending
SET npa_amount = GREATEST(invested_amount - calculated_principal_received, 0.00)
WHERE loan_status IN ('NPA', 'CLOSED')
  AND invested_amount > calculated_principal_received;

UPDATE manual_lending
SET is_npa = npa_amount > 0,
    npa_reason = CASE
        WHEN npa_amount = 0 THEN NULL
        WHEN loan_status = 'NPA' THEN 'REPORTED_AS_NPA'
        ELSE 'CLOSED_BEFORE_PRINCIPAL_RECOVERY'
    END;

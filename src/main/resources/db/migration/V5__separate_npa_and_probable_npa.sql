ALTER TABLE manual_lending
    ADD COLUMN npa_amount DECIMAL(19,2) NOT NULL DEFAULT 0.00 AFTER outstanding_principal,
    ADD COLUMN is_probable_npa BOOLEAN NOT NULL DEFAULT FALSE AFTER npa_amount,
    ADD COLUMN probable_npa_amount DECIMAL(19,2) NOT NULL DEFAULT 0.00 AFTER is_probable_npa;

-- Preserve the previous inferred-NPA result as probable NPA before replacing is_npa
-- with the official value supplied by the LenDenClub report.
UPDATE manual_lending
SET is_probable_npa = is_npa AND loan_status = 'ACTIVE',
    probable_npa_amount = CASE
        WHEN is_npa AND loan_status = 'ACTIVE' THEN outstanding_principal
        ELSE 0.00
    END;

UPDATE manual_lending m
SET m.is_npa = CASE
        WHEN EXISTS (
            SELECT 1 FROM loan_report_staging s
            WHERE s.import_batch_id = m.source_import_batch_id
              AND LOWER(s.scheme_id) = LOWER(m.scheme_id)
              AND s.reported_npa_amount IS NOT NULL
        ) THEN EXISTS (
            SELECT 1 FROM loan_report_staging s
            WHERE s.import_batch_id = m.source_import_batch_id
              AND LOWER(s.scheme_id) = LOWER(m.scheme_id)
              AND s.reported_npa_amount > 0
        )
        ELSE m.loan_status = 'NPA' OR (m.loan_status = 'CLOSED' AND m.outstanding_principal > 0)
    END;

UPDATE manual_lending
SET npa_amount = CASE WHEN is_npa THEN outstanding_principal ELSE 0.00 END,
    npa_reason = CASE
        WHEN is_npa AND loan_status = 'NPA' THEN 'REPORTED_AS_NPA'
        WHEN is_npa THEN 'CLOSED_BEFORE_PRINCIPAL_RECOVERY'
        WHEN is_probable_npa THEN npa_reason
        ELSE NULL
    END;

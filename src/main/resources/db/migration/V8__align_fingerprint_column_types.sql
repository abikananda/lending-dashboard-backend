-- Keep the database types aligned with the JPA String mappings.
-- A SHA-256 hexadecimal fingerprint remains limited to exactly 64 characters
-- by the application while VARCHAR avoids Hibernate CHAR/VARCHAR validation errors.

ALTER TABLE bank_credit
    MODIFY COLUMN row_fingerprint VARCHAR(64) NOT NULL;

ALTER TABLE wallet_transaction
    MODIFY COLUMN row_fingerprint VARCHAR(64) NOT NULL;

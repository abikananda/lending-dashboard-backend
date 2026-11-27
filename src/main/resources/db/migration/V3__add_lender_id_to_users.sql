ALTER TABLE users ADD COLUMN lender_id VARCHAR(64) NULL;
CREATE UNIQUE INDEX uk_users_lender_id ON users(lender_id);

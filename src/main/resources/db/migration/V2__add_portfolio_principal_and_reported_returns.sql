CREATE TABLE lending_portfolio (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    investment_principal_amount DECIMAL(19,2) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_lending_portfolio_user UNIQUE (user_id),
    CONSTRAINT fk_lending_portfolio_user FOREIGN KEY (user_id) REFERENCES users(id)
);

ALTER TABLE loan_report_staging
    ADD COLUMN reported_principal_received DECIMAL(19,2) NULL AFTER amount_received,
    ADD COLUMN reported_interest_received DECIMAL(19,2) NULL AFTER reported_principal_received;

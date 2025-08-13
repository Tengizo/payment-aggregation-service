create schema if not exists payment;

CREATE TABLE IF NOT EXISTS payment.transactions (
    transaction_id UUID PRIMARY KEY,
    account_id VARCHAR(64) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    ts_utc TIMESTAMP WITH TIME ZONE NOT NULL,
    business_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS payment.daily_balance (
    account_id VARCHAR(64) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    business_date DATE NOT NULL,
    balance NUMERIC(19,4) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             PRIMARY KEY (account_id, currency, business_date)
    );


-- Index for balance queries by account and date
CREATE INDEX IF NOT EXISTS idx_bal_account_date
    ON payment.daily_balance(account_id, business_date);
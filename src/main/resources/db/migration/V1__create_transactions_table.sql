-- Create transactions table for idempotency and audit trail
CREATE TABLE IF NOT EXISTS transactions (
    transaction_id UUID PRIMARY KEY,
    account_id VARCHAR(64) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    ts_utc TIMESTAMP WITH TIME ZONE NOT NULL,
    business_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for account-based queries
CREATE INDEX IF NOT EXISTS idx_tx_account_date
    ON transactions(account_id, business_date);

-- Composite index for balance calculations
CREATE INDEX IF NOT EXISTS idx_tx_account_currency_date
    ON transactions(account_id, currency, business_date);

-- Index for timestamp-based queries (optional, for analytics)
CREATE INDEX IF NOT EXISTS idx_tx_created_at
    ON transactions(created_at);

COMMENT ON TABLE transactions IS 'Stores all transactions for idempotency and audit purposes';
COMMENT ON COLUMN transactions.transaction_id IS 'Unique identifier for idempotency';
COMMENT ON COLUMN transactions.business_date IS 'Derived from ts_utc for daily aggregation';

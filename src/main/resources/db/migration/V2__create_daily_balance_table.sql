-- Create daily balance aggregate table
CREATE TABLE IF NOT EXISTS daily_balance (
    account_id VARCHAR(64) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    business_date DATE NOT NULL,
    balance NUMERIC(19,4) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (account_id, currency, business_date)
);

-- Index for balance queries by account and date
CREATE INDEX IF NOT EXISTS idx_bal_account_date
    ON daily_balance(account_id, business_date);

-- Index for updated_at (useful for incremental exports/CDC)
CREATE INDEX IF NOT EXISTS idx_bal_updated
    ON daily_balance(updated_at);

COMMENT ON TABLE daily_balance IS 'Daily aggregated balances per account and currency';
COMMENT ON COLUMN daily_balance.balance IS 'Net balance for the specific day';

-- Transactions seed
INSERT INTO payment.transactions (transaction_id, account_id, currency, amount, ts_utc, business_date)
VALUES
  ('00000000-0000-0000-0000-0000000000a1', 'ACC-1', 'USD', 100.00, '2025-01-01T10:00:00Z', '2025-01-01'),
  ('00000000-0000-0000-0000-0000000000a2', 'ACC-1', 'USD', -25.50, '2025-01-01T12:30:00Z', '2025-01-01'),
  ('00000000-0000-0000-0000-0000000000b1', 'ACC-2', 'EUR', 300.00, '2025-01-02T09:15:00Z', '2025-01-02')
ON CONFLICT (transaction_id) DO NOTHING;

-- Daily balances matching the above transactions
INSERT INTO payment.daily_balance (account_id, currency, business_date, balance)
VALUES
  ('ACC-1', 'USD', '2025-01-01', 74.50), -- 100.00 + (-25.50)
  ('ACC-2', 'EUR', '2025-01-02', 300.00)
ON CONFLICT (account_id, currency, business_date) DO NOTHING;

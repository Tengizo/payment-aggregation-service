# Design Rationale – Payment Aggregation Service

## 1. Overview
The Payment Aggregation Service is designed to process incoming payment transactions in an **idempotent** and **concurrency-safe** manner, while maintaining **daily aggregated balances**.  
It emphasizes **data integrity**, **performance under load**, and **simplicity of design**.

---

## 2. Core Principles
1. **Idempotency**
    - Every transaction is uniquely identified by `transaction_id` (UUID from the client).
    - Duplicate transactions are ignored at the database level using `ON CONFLICT DO NOTHING`.

2. **Atomic Balance Updates**
    - Daily balances are updated in the **same SQL statement** that inserts the transaction.
    - This ensures **both operations succeed or fail together**, preventing partial updates.

3. **Concurrency Safety**
    - PostgreSQL row-level locking during `ON CONFLICT DO UPDATE` ensures increments are serialized for the same `(account_id, currency, date)`.
    - No explicit application-level locks are required.

---

## 3. Atomic Update with CTE
The design uses a **single Common Table Expression (CTE)** for transaction insertion and balance update:

```sql
WITH ins AS (
  INSERT INTO transactions (transaction_id, account_id, currency, amount, ts_utc, business_date)
  VALUES (:id, :acct, :ccy, :amt, :ts, :date)
  ON CONFLICT (transaction_id) DO NOTHING
  RETURNING account_id, currency, amount, business_date
)
INSERT INTO daily_balance (account_id, currency, business_date, balance)
SELECT account_id, currency, business_date, amount FROM ins
ON CONFLICT (account_id, currency, business_date)
DO UPDATE SET balance = daily_balance.balance + EXCLUDED.balance,
              updated_at = now();
```
### Why this works well:
- Single DB round-trip for insert + update.
- Row-level lock on conflict ensures safe increments even with concurrent requests.
- EXCLUDED.balance contains the incoming amount and is added to the existing balance atomically.
- Works safely at READ COMMITTED isolation, avoiding the need for SERIALIZABLE.
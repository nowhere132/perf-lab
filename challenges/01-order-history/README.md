# 01 · The order book screen takes seconds

**Situation.** Every trading screen has an order book tab ("Sổ lệnh"). It calls
`GET /api/accounts/{id}/orders`, which returns the account's 50 most recent
orders with a fill summary for each. There are 3 million orders in history.
Support tickets say the tab takes seconds to open, and it gets worse when many
clients open it at once.

**Goal.** `./lab test` offers 300 requests/s from the 50,000 most active accounts.

| | target |
|---|---|
| p99 latency | < 15 ms |
| errors | < 0.1 % |

**Rules.**
- The response stays the same: same fields, the 50 newest orders, newest first.
- No response caching. A client who just placed an order must see it.
- Schema changes go in `db/migrations/` and are applied with `./lab migrate`.

**You may change** anything under `app/` and `db/migrations/`.

**Instruments worth reaching for.** `./lab ash` (where database time went in the
last run), `pg_stat_statements` and `EXPLAIN (ANALYZE, BUFFERS)` through
`./lab psql`, the Postgres rows of the Grafana dashboard.

**Afterwards you should be able to say**
- how many SQL statements one request issued before and after, and how you counted them;
- which of your changes moved p50 from seconds to milliseconds, and which moved it from ~10 ms to ~2 ms.

**Stretch.** p99 < 5 ms at 1,000 requests/s (`./lab test 1 RATE=1000`).

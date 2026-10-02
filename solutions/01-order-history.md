# 01 · The order book screen takes seconds — solution

## Reading the evidence

`./lab ash` after a failing run puts ~90 % of database time on CPU in one
statement, the `select ... from orders where account_id = ? order by created_at desc`
query, with about 8 sessions busy on a 4-CPU database. The database is
saturated by a single query shape.

    explain (analyze, buffers)
    select * from orders where account_id = 123 order by created_at desc limit 50;

shows a parallel sequential scan: 3,000,000 rows read to return 50, 700 ms
even with nothing else running. There is no
index on `orders.account_id`.

After adding one the statement is fast, but a request still takes
~12 ms. `pg_stat_statements` ordered by `calls` shows the two statements on
`fills` and `order_events` each called 50 times as often as the `orders` query:
`OrderHistoryService.recent` walks the lazy `fills` and `events` collections of
every order. One request is 101 statements (the N+1 pattern, twice).

## Fix

1. `db/migrations/001-orders-by-account.sql`

       CREATE INDEX orders_account_created_idx ON orders (account_id, created_at DESC);

   Equality column first, then the sort column in the order the query wants:
   Postgres reads the first 50 index entries and stops. No sort, no scan.

2. Load the collections for all 50 orders at once. The smallest change is
   Hibernate batch fetching, in `application.yaml`:

       spring.jpa.properties.hibernate.default_batch_fetch_size: 100

   The first access to any `fills` collection then loads the collections of
   all orders in the session with one `where order_id = any(?)`. 101
   statements become 3.

## Numbers

| | p50 | p99 | note |
|---|---|---|---|
| as inherited | 4–10 s | — | cannot sustain even 30 req/s |
| index only, 300 req/s | 13 ms | 23 ms | 101 statements per request |
| index + batch fetch, 300 req/s | 2 ms | 4–8 ms | 3 statements per request |

## Take away

- Ask the database where its time went before reading application code. One
  statement at 90 % of DB time is a finished diagnosis.
- "Rows read / rows returned" is the number to look at in a plan.
- N+1 does not show up as a slow query. Every statement is fast; there are
  just a hundred of them. It shows up in call counts.

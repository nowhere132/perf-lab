## Hint 1
Do not start in the Java code. Run the test, then `./lab ash`. The second table
lists the statements the database spent its time on. One of them dominates.
Copy it into `./lab psql` with real values and put `EXPLAIN (ANALYZE, BUFFERS)`
in front.

## Hint 2
The plan shows how many rows Postgres reads to return 50. That ratio is the
first problem, and the fix is a schema change, not a code change. Think about
which columns the query filters on and which it sorts by, and in what order an
index would have to hold them so that the 50 rows can be read off the front.

## Hint 3
Once the big statement is fast, the test still misses. Look at
`pg_stat_statements` ordered by `calls`:

    select calls, round(mean_exec_time::numeric, 3) as ms, left(query, 80)
    from pg_stat_statements order by calls desc limit 5;

Compare the call counts of the top statements with the number of requests the
test sent. Each statement is fast. How many of them does one request issue?

## Hint 4
`Order.fills` and `Order.events` are lazy collections, and
`OrderHistoryService.recent` touches both for each of the 50 orders: 1 + 50 + 50
statements per request. Any way of loading the collections for all 50 orders
at once works: Hibernate batch fetching (`hibernate.default_batch_fetch_size`
or `@BatchSize`), or two explicit `where order_id in (...)` queries. A
`join fetch` of two collections at once does not: it multiplies the rows.

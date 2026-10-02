# 02 · Orders time out at the open — solution

## Reading the evidence

- 200 orders/s: p99 44 ms. 300 orders/s: seconds. A cliff, not a slope.
- Grafana, Saturation row: pool `in use` pinned at 10, `threads waiting`
  climbing to ~190. Tomcat busy threads at 200.
- `./lab threads`: `189 TIMED_WAITING | OrderService$$SpringCGLIB$$0.place`.
  The full dump shows them in `HikariPool.getConnection`, called by the
  transaction proxy before `place` itself has started.
- `./lab ash`: ~8.5 sessions in `Client / ClientRead`, state
  `idle in transaction`. The database has 10 sessions open and is waiting for
  the application to say something.

The application is holding transactions open while it does something that is
not SQL. `OrderService.place` is `@Transactional` and calls the risk service
(~30 ms) and the venue (~3 ms) in the middle.

The arithmetic, which you can do before measuring: one order holds one
connection for about 30 + 3.6 + 2 ms. Ten connections / 0.036 s = ~280
orders/s. Measured ceiling: 242–262/s.

## Fix

Hold a connection only while talking to the database.

- The risk check needs nothing from the database: do it before any transaction.
- Transaction 1: lock the account, reserve, insert the order as `NEW`. Commit.
- Venue call, no transaction open.
- Transaction 2: mark the order `ACCEPTED`, insert the event. Commit.
- If the venue call throws: a compensating transaction releases the
  reservation and marks the order `REJECTED`.

See `patches/02-order-placement.patch` (`TransactionTemplate` instead of
`@Transactional`).

Each transaction now lasts a couple of milliseconds, so ten connections carry
well over a thousand orders per second. It also stops holding the account's
row lock across a 30 ms network call.

Raising the pool size would have "worked" up to a point, which is why the rule
forbids it: it hides the design error, multiplies idle-in-transaction sessions
on the database, and keeps row locks held across remote calls.

## What the fix costs

The original was atomic: any failure rolled everything back. The new shape has
a window (after the venue accepted, before transaction 2 commits) in which a
crash leaves an order `NEW` that the exchange considers live. A real router
closes that with a recovery job that reconciles `NEW` orders against the venue
at startup. The old design had the mirror-image problem (venue accepted, local
rollback) and hid it better.

## Numbers

| | 200/s p99 | 400/s p99 | ceiling |
|---|---|---|---|
| as inherited | 44–47 ms | 3.1–3.8 s | ~250/s |
| fixed | — | 48–68 ms | ~550/s (next bottleneck, challenge 03) |

## Take away

- Throughput ceiling = resources / hold time (Little's law). If you can do
  that division, you can predict the cliff.
- Idle CPU + idle database + timeouts = something upstream is holding a scarce
  resource while waiting on something else.
- `idle in transaction` in `pg_stat_activity` is the database telling you the
  application is the slow one.

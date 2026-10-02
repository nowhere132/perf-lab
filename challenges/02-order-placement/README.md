# 02 · Orders time out at the open

**Situation.** At the opening auction order flow reaches 400 orders/s and
clients start seeing multi-second responses and timeouts on `POST /api/orders`.
The on-call engineer reports that the app's CPU is nearly idle and the database
is "doing nothing". Nobody has found anything slow.

Placing an order means: validate, reserve cash or stock, clear the firm's
pre-trade risk service (~30 ms, another team's system), send to the exchange
(~3 ms), record the outcome.

**Goal.** `./lab test` offers 400 orders/s.

| | target |
|---|---|
| p99 latency | < 100 ms |
| errors | < 0.1 % |

**Rules.**
- The database gives this service 12 connections. The pool's maximum stays
  at 10 or less (the grader checks).
- The risk service and the exchange are outside your control: their latency is
  what it is, and every order still goes through both.
- An order must not reach the exchange unless its reservation is committed,
  and a reservation must not survive an order that the exchange did not accept.

**You may change** anything under `app/`.

**Instruments worth reaching for.** The Saturation row in Grafana,
`./lab threads` while the test runs, `./lab ash` and what "idle in transaction"
means.

**Before you measure,** predict the ceiling: how many orders per second can
this design carry, given the pool size and how long one order holds a
connection? Then compare with `./lab test 2 RATE=200` and `RATE=300`.

**Afterwards you should be able to say**
- why an idle database and an idle CPU are the expected picture for this failure;
- what the ceiling is now, from the same arithmetic.

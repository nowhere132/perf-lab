# 03 · Twice the order flow

*Needs challenge 02 solved.*

**Situation.** Business is good: the firm expects 800 orders/s at the open.
After your last fix the pool is no longer the limit, yet the router tops out
well below 800. CPU: idle. Database: idle. Connection pool: mostly idle.

**Goal.** `./lab test` offers 800 orders/s.

| | target |
|---|---|
| p99 latency | < 100 ms |
| errors | < 0.1 % |

**Rules.**
- Pool maximum stays at 10 or less.
- The exchanges' connectivity rules are in the header of
  `exchange-sim/ExchangeSim.java`. Read them: they are the spec you work to.
  A message that breaks them is rejected and counts as an error.
- Do not edit `exchange-sim/`.

**You may change** anything under `app/`.

**Instruments worth reaching for.** `./lab threads`, `./lab profile lock`,
`./lab jfr` then `./lab exec jfr view contention-by-site /results/<file>`,
and `curl localhost:9000/stats`.

**Before you measure,** predict the ceiling again. Order flow splits
HOSE 50 % / HNX 30 % / UPCOM 20 %.

**Afterwards you should be able to say**
- which resource was saturated while every dashboard gauge looked idle, and what gauge would have shown it;
- why "remove the lock" is not the fix, and what is.

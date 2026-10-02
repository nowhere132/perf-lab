# 07 · Healthy dashboards, angry clients

*Needs challenges 02 and 03 solved.*

**Situation.** Operations now adjust trading limits intraday, so the router
refreshes its reference data every ten seconds instead of once at start of day
(`challenges/07-tail/compose.override.yaml`). The team's load test says
everything is fine:

    ./lab k6 orders-closed VUS=20 DURATION=60

Meanwhile brokers report that an order "hangs for half a second" several
times a minute.

**Goal.** `./lab test` offers 500 orders/s on a fixed schedule (open model).

| | target |
|---|---|
| p99 latency | < 150 ms |
| p99.9 latency | < 300 ms |
| errors | < 0.1 % |

**Rules.**
- The refresh stays on, every ten seconds, and must pick up changes to
  `symbols` and `trading_limits`.
- Pool maximum stays at 10 or less.

**You may change** anything under `app/`.

**Instruments worth reaching for.** Both load scripts, side by side. The
latency panel in Grafana at a 5 s refresh. `./lab threads` taken at the right
moment (try a loop). `./lab jfr`. Debug logging for the `lab.router` package.

**Afterwards you should be able to say**
- why the two load tests disagree about the same system, and which one a broker would agree with;
- from the stall's length and period alone, what fraction of orders it hits, and so which percentile first shows it;
- why average latency and p50 are useless for this problem.

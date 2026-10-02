# perf-lab — performance challenges on an order-routing system

Eight challenges against one small system: a Spring Boot 4 (Java 25) order
router writing to Postgres 18, with one million accounts and three million
historical orders. Each challenge hands you the system in a state that misses
a stated target, and a graded load test that says PASS or FAIL. What is wrong
is for you to find.

    ./lab start 1      # bring the stack up, print the first brief
    ./lab test         # graded run: PASS / FAIL
    ./lab help         # every command

Needs Docker, `k6` and about 6 GB of free RAM. The first start builds the
images and seeds the database (about two minutes).

## The system

    k6 ──► order-router (Spring Boot, 4 CPU / 2 GB) ──► Postgres 18 (4 CPU / 2 GB)
                 │
                 └──► exchange-sim: pre-trade risk service (~30 ms)
                                    HOSE / HNX / UPCOM gateways (~3 ms)

| Endpoint | What it does |
|---|---|
| `POST /api/orders` | validate, reserve cash or stock, risk check, send to the exchange, record |
| `GET /api/accounts/{id}/orders` | the account's 50 latest orders with fill summaries |
| `GET /api/accounts/{id}` | balances and positions |
| `GET /api/market/board/{board}` | a page of the price board, computed from recent trades |

## The challenges

| # | Symptom | Target | Area |
|---|---|---|---|
| 01 | The order book screen takes seconds | 300 req/s, p99 < 15 ms | queries |
| 02 | Orders time out at the open | 400 orders/s, p99 < 100 ms | resource pools |
| 03 | Twice the order flow | 800 orders/s, p99 < 100 ms | locks |
| 04 | The price board stutters | 300 req/s, p99 < 20 ms, p99.9 < 60 ms | garbage collection |
| 05 | Moving to the container platform | 300 req/s, p99 < 50 ms, no restarts | container limits |
| 06 | Same code, slower in the new datacentre | 450 orders/s, p99 < 200 ms on a 400-IOPS volume | WAL, fsync, buffers |
| 07 | Healthy dashboards, angry clients | 500 orders/s, p99 < 150 ms, p99.9 < 300 ms | tail latency |
| 08 | The last microsecond | 6x baseline on an in-memory benchmark | CPU caches |

01, 04, 05 and 08 stand alone. 03 builds on 02; 06 and 07 build on 02 and 03.
Your fixes accumulate: there is one codebase, and it gets better as you go.

Each challenge directory holds the brief (`README.md`), the target
(`challenge.env`), progressive hints (`hints.md`, via `./lab hint`) and, for
some, an overlay that changes how the stack is deployed
(`compose.override.yaml`). Reference solutions are in `solutions/`.

## How to work a challenge

The loop is the same every time, and practising the loop is the point:

1. **State the goal as a number.** It is in the brief. Run `./lab test` once to see how far off you are.
2. **Predict before you measure.** Most briefs ask for a back-of-envelope ceiling. Write it down.
3. **Find the saturated resource.** For every resource (CPU, memory, disk, connections, threads, locks) ask: utilisation, saturation, errors. Start outside the code: Grafana, `./lab top`, `./lab ash`, `./lab threads`.
4. **Form one hypothesis** that explains all the evidence, including what is idle.
5. **Change one thing**, `./lab deploy` (code) or `./lab restart` (config), and measure again.
6. **Explain the result.** Each brief ends with questions you should be able to answer. If the test passes and you cannot answer them, you got lucky.

A graded run is open-model: k6 sends requests on a fixed schedule whether or
not earlier ones have returned, with a 30 s warm-up that is not measured and
60 s that is. `./lab test RATE=200 DURATION=20` runs an ungraded variation.

## Instruments

| Command | Shows |
|---|---|
| Grafana `localhost:3000` | "Order router" (traffic, saturation, JVM, Postgres) and "Active Session History" |
| `./lab top` | per-container CPU, memory, IO; cgroup throttling; OOM kills |
| `./lab ash` | where database time went during the last run, by wait event and statement |
| `./lab psql` | `pg_stat_statements`, `pg_stat_io`, `EXPLAIN (ANALYZE, BUFFERS)` |
| `./lab threads` | thread dump, summarised by state and application frame |
| `./lab profile cpu\|alloc\|lock\|wall 30` | async-profiler flame graph in `results/` |
| `./lab jfr 60` | Java Flight Recorder file in `results/` |
| `./lab gc 30` | `jstat -gcutil`, a line per second |
| `./lab exec jcmd 1 help` | everything else the JVM can tell you |

## Working with an AI assistant

`CLAUDE.md` tells Claude Code to act as a coach in this directory: it will ask
for your hypothesis first, help you read evidence (paste a flame graph, a GC
log, a plan), and stay out of `solutions/` unless you say you give up. Useful
prompts:

- "Here is `./lab ash` and `./lab threads` from a failing run. My hypothesis is X. What would disprove it?"
- "Review my load test method for this claim."
- "I passed challenge 3. Quiz me on the questions at the end of the brief."

## Layout

    lab                     the CLI
    compose.yaml            the stack
    app/                    the order router (yours to change)
    exchange-sim/           risk service and exchange gateways (not yours)
    db/init/                schema and seed          db/migrations/   your schema changes
    load/                   k6 workloads
    challenges/NN-*/        brief, target, hints, deployment overlay
    solutions/              reference solutions (spoilers)
    observability/          Prometheus and Grafana provisioning
    ash/                    Active Session History sampler
    docs/pgbench-lab.md     the original pgbench scenarios (run.sh, mix.sh, scripts/)

Port 3000 is shared with the original lab's Grafana (`grafana/docker-compose.yaml`);
run one or the other.

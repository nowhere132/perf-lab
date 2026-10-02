# 08 · The last microsecond

**Situation.** With the I/O problems gone, the desk wants to know what the
in-memory part of the order path costs. `HotPathBench` strips everything else
away: one dispatcher thread per board pushes orders for random accounts (one
million of them) through the intraday exposure check (`ExposureBook`) and the
routing counters (`RoutingStats`). No HTTP, no SQL, no locks held for long,
and it still manages only a few million operations per second on three cores.

**Goal.**

    ./lab bench        # first run records your baseline

| | target |
|---|---|
| throughput | 6x your baseline |
| correctness | `CHECK ok`: every order counted exactly once |

**Rules.**
- Do not edit `lab/router/bench/`.
- `ExposureBook` and `RoutingStats` keep their public methods: `OrderService`
  calls them. What is behind the methods is yours.
- Both stay safe for concurrent use.

**You may change** `app/src/main/java/lab/router/risk/`.

**Instruments worth reaching for.** This is where `perf` earns its place. From
the host, while the bench runs:

    PID=$(pgrep -n -f '^java .*HotPathBench')
    sudo perf stat -e cycles,instructions,cache-misses,L1-dcache-load-misses -p $PID -- sleep 5
    sudo perf c2c record -p $PID -- sleep 5 && sudo perf c2c report

Look at instructions per cycle before you look at any code. Then read the two
classes and count, for one operation, the objects allocated and the pointers
followed.

**Afterwards you should be able to say**
- how many cache misses one exposure check cost before and after, and why;
- why three threads that never touch the same counter still slowed each other down;
- what each of the two fixes was worth alone, and why the product is larger than the sum.

**Stretch.** Rewrite the benchmark with JMH and `-prof perfnorm`, and compare its numbers with this harness.

# 07 · Healthy dashboards, angry clients — solution

## Reading the evidence

| same system | p50 | p99 | p99.9 |
|---|---|---|---|
| closed model, 20 VUs | 45 ms | 55 ms | 565 ms |
| open model, 500/s | 45 ms | 782 ms | 978 ms |

The closed model sends a request only when a user is free. During a 0.5 s
stall, 20 users have 20 requests outstanding and then send nothing, so the
stall contributes 20 slow samples among ~4,000 per ten seconds (0.5 %): just
beyond p99. In the open model 250 orders arrive during the stall and all of
them wait: 5 % of all requests, so p95 and everything above it is ruined.
That gap is coordinated omission. Brokers live in the open model.

The arithmetic without any tool: stall length / period = 0.5 s / 10 s = 5 % of
arrivals affected, with an average extra wait of half the stall.

Finding it: spikes are exactly 10 s apart, and one thing runs every 10 s.
A thread dump taken during a spike (one in twelve, in a loop) shows
`179 WAITING | lab.router.market.ReferenceData.find`: request threads parked
on a `ReentrantReadWriteLock`, and the scheduler thread
inside `ReferenceData.reload` doing JDBC. `reload()` takes the write lock,
clears the map and then runs one `findById` per symbol: 1,601 queries, about
half a second, with every reader locked out.

## Fix

`patches/07-tail.patch`:

- Build the new map without any lock, then publish it by assigning a
  `volatile` field (an immutable snapshot). Readers do one volatile read and
  never wait; they see the old data until the instant the new map is complete.
- Load trading limits with one `findAll()` instead of 1,600 `findById()`
  calls: the reload drops from ~500 ms to a few ms.

Either change alone passes. The first removes the stall whatever the reload
costs; the second makes the reload cheap but leaves a design in which a slow
database freezes order entry.

## Numbers (500 orders/s, open model)

| | p99 | p99.9 |
|---|---|---|
| as deployed | 500–780 ms | 630–980 ms |
| fixed | 62–89 ms | 76–230 ms |

## Take away

- A load generator that waits for responses cannot see stalls. Use arrival
  rates for latency questions; keep closed models for "how many users can I
  fit".
- Averages and medians are blind to events that hit 5 % of requests. Decide
  which percentile the business cares about and compute what fraction of
  requests a periodic event touches.
- Read-mostly data should be published, not locked: build aside, swap a
  reference.

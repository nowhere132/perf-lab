# 03 · Twice the order flow — solution

## Reading the evidence

Ceiling ~500 orders/s with CPU, database and pool all idle (one connection in
use). `./lab threads` during the run:

    180 BLOCKED | lab.router.gateway.VenueSession.send(VenueSession.java:36)
     18 WAITING | lab.router.gateway.RiskClient.approve(RiskClient.java:36)

`VenueSession.send` is `synchronized` and performs the HTTP round trip to the
exchange inside the lock. One session per venue, so one order in flight per
venue. A round trip is ~3.6 ms: HOSE can carry 1 / 0.0036 = ~275 orders/s, and
HOSE gets half the flow, so the router saturates at ~500–550/s. Java monitors have
no gauge on a standard dashboard, which is why everything looked idle; the
`blocked` series of "JVM threads by state" is the one that shows it.

## Why not just shrink the lock

The venue requires each session's messages to arrive in sequence, without gaps
(`exchange-sim/ExchangeSim.java`). With request/response over HTTP the only way
to guarantee arrival order is one message at a time. Protect only the counter
and two threads can take seq 7 and 8 and deliver 8 first: `409 sequence gap`.
Try it and watch `curl localhost:9000/stats`.

## Fix

The same spec allows 16 sessions per venue. Keep each session strictly
sequential and hold several: a `BlockingQueue<VenueSession>` per venue, borrow
one per order, return it in `finally`. See `patches/03-more-flow.patch`.

Sizing: 400 orders/s to HOSE x 3.6 ms = 1.5 sessions busy on average. Twelve
leaves room for bursts.

## Numbers

| | 800/s p99 | ceiling |
|---|---|---|
| one session per venue | 3.8 s | ~500/s |
| 12 sessions per venue | 70 ms | ~1,100–1,300/s (pool of 10 waiting on commit flushes) |

## Take away

- A lock around I/O turns a multi-threaded server into a single-threaded one
  for that path. Capacity = 1 / (time under the lock).
- When the lock protects a real invariant of a resource, do not weaken the
  lock: get more instances of the resource (striping, pooling, partitioning).
- Thread dumps are the cheapest profiler there is for "everything is idle but
  nothing moves".

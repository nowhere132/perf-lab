# 04 · The price board stutters — solution

## Reading the evidence

p50 ~5 ms, p99 ~600 ms: stop-the-world pauses. `./lab gc 30` shows `FGC`
incrementing every few seconds with `FGCT` growing ~0.5 s each time; Grafana's
"Longest GC pause" says the same, with collector names `Copy` and
`MarkSweepCompact`. Those are the serial collector's generations.

`./lab exec jcmd 1 VM.flags` confirms `-XX:+UseSerialGC`, which nobody asked
for. The JVM picks it whenever the machine is not "server class": fewer than
2 CPUs or less than 1,792 MB of memory. The container has 768 MB.

Why is the old generation filling so fast? Request garbage dies young and is
cheap (minor collections are a few ms). The problem is `TickStore`: 50,000
`Tick` objects per second (each with a `BigDecimal` and a linked-list node),
each kept for ~30 s until 1,000 newer ticks of its symbol have arrived. They
survive the young generation, get promoted, then die: ~6 MB/s of garbage
created directly in the old generation, on top of a ~250 MB live set in a
400 MB heap. The serial collector can only clean the old generation with a
full stop-the-world mark-compact of the whole heap.

## Fix (flags)

`challenges/04-price-board/compose.override.yaml`:

    mem_limit: 1g
    JDK_JAVA_OPTIONS: "-Xms640m -Xmx640m -XX:+UseG1GC"

G1 marks the old generation concurrently and reclaims it in small increments
during young pauses, so medium-lived garbage costs milliseconds, not half
seconds. 640 MB leaves headroom over the live set and ~350 MB for the JVM's
non-heap memory and the container's other needs. ZGC works too.

A bigger heap with the serial collector does not pass: full collections become
rarer, not shorter (measured: p99 353 ms at 640 MB).

## Fix (code, the stretch goal)

Stop creating the objects. Per symbol, three primitive arrays used as a ring
buffer (`long[] time`, `long[] priceInDong`, `long[] volume`) and a write
index. 1,600 symbols x 1,000 ticks x 24 bytes = 38 MB, allocated once, never
collected. The board computation becomes a loop over `long`s. With that the
service passes on a 256 MB heap with any collector.

## Numbers (300 req/s)

| | p99 | p99.9 |
|---|---|---|
| 400 MB heap, serial (as deployed) | 649 ms | 1.0 s |
| 640 MB heap, serial | 353 ms | 723 ms |
| 400 MB heap, G1 | 8 ms | 11 ms |
| 640 MB heap, G1 | 8–12 ms | 14–32 ms |

## Take away

- Know which collector you are running. In small containers it is often not
  the one you think.
- GC cost is driven by what survives, not by how much you allocate. Objects
  that live "a while" (caches, queues, windows) are the expensive ones.
- Flags buy you a better collector. Only data layout removes the work.

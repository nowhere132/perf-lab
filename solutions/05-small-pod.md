# 05 · Moving to the container platform — solution

## Reading the evidence

**Restarts.** `./lab top` reports the app container killed with exit 137
and kernel OOM events (`journalctl -k` has the "Memory cgroup out of memory:
Killed process ... (java)" line), and
`./lab logs app` shows no `OutOfMemoryError`: the process was killed from
outside. Limit 640 MiB, `-Xmx560m`. The JVM also needs metaspace, code cache,
thread stacks, GC data structures and direct buffers, ~150 MiB here
(`-XX:NativeMemoryTracking=summary`, then `jcmd 1 VM.native_memory summary`).
560 + 150 > 640. As soon as the heap is fully touched the kernel kills it.

**Slowness.** `docker stats` says 50 %, measured against one core. The limit is
0.5 CPU, so that is 100 % of the quota. `cpu.stat`: `nr_throttled` almost
equal to `nr_periods`. In nearly every 100 ms period the container burns its
50 ms of quota and is frozen for the rest.

**After fixing both**, if you leave the collector to the JVM: 768 MiB is below
1,792 MB, so it picks the serial collector and challenge 04's half-second
pauses come back.

## Fix

`challenges/05-small-pod/compose.override.yaml`:

    cpus: 2
    mem_limit: 768m
    memswap_limit: 768m
    JDK_JAVA_OPTIONS: "-Xms480m -Xmx480m -XX:+UseG1GC"

Heap = limit - non-heap - some slack: 768 - 150 - ~140. `-XX:MaxRAMPercentage=62`
expresses the same thing relative to the limit.

## Numbers (300 req/s)

| | result |
|---|---|
| 0.5 CPU, 640 MiB, `-Xmx560m` (as deployed) | restarts, 55 % errors |
| 2 CPU, 768 MiB, `-Xmx560m` | still OOM-killed |
| 0.5 CPU, 768 MiB, `-Xmx480m` G1 | no restarts, throttled: p99 10 s |
| 2 CPU, 768 MiB, `-Xmx480m` G1 | p99 12–18 ms |

## Take away

- Container memory limit = heap + everything else the process maps. Size the
  heap from the limit, never equal to it.
- An OOM kill leaves no Java stack trace. Exit code 137 and the cgroup's
  `oom_kill` counter are the evidence.
- CPU utilisation is relative to something. Under a CFS quota the honest
  saturation signal is throttled periods, not percent.
- JVM ergonomics are decided from what the container looks like. Check the
  result (`VM.flags`) instead of assuming.

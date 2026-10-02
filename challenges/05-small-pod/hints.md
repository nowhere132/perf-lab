## Hint 1
Two separate symptoms, two separate causes. Take the restarts first.
`./lab top` shows, for the app container, its limits, its memory and how it
last died. `./lab logs app` shows what the JVM said before it died, which is
interesting for what is missing from it.

## Hint 2
The JVM never logs an `OutOfMemoryError`: the heap is fine. The kernel kills
the process because the container as a whole went over its memory limit. The
limit is 640 MiB and the heap alone is allowed 560 MiB. A JVM is more than its
heap: metaspace, the JIT code cache, thread stacks (200 Tomcat threads), GC
bookkeeping, direct buffers. Here that is roughly 150 MiB. Enable
`-XX:NativeMemoryTracking=summary` and run
`./lab exec jcmd 1 VM.native_memory summary` to see it itemised.

## Hint 3
Now the slowness. "50 % CPU" is measured against one core; the container's
limit is half a core, so it is using 100 % of what it is allowed. Look at
`nr_throttled` against `nr_periods` in `./lab top`: in almost every 100 ms
scheduling period the container used up its quota and was frozen for the rest
of the period. A request that arrives during the frozen part simply waits.

## Hint 4
Spend the budget (2 CPUs, 768 MiB) deliberately. Heap = limit minus what the
JVM needs besides the heap, so about 480 MB here, not 700. Then check which
collector you ended up with (`./lab exec jcmd 1 VM.flags`): below 1,792 MB or
below 2 CPUs the JVM considers the machine "client class" and silently picks
the serial collector, which brings back the half-second pauses of challenge 04.
Ask for G1 explicitly.

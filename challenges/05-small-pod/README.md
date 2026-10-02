# 05 · Moving to the container platform

**Situation.** The platform team moved the router onto the shared container
platform with their default sizing; see
`challenges/05-small-pod/compose.override.yaml`. Since then the pod restarts
every minute or so, and between restarts it is slow. The platform dashboard
shows the pod at "50 % CPU", so they have ruled CPU out.

**Goal.** `./lab test` offers 300 requests/s of front-office reads (price
board and account summary).

| | target |
|---|---|
| p99 latency | < 50 ms |
| errors | < 0.1 % |
| restarts during the run | none |

**Rules.**
- Your budget is 2 CPUs and 768 MiB for the app container (the grader checks).
  How you use it, limits and JVM flags alike, is up to you.

**You may change** the overlay file (then `./lab restart app`). No code change
is needed.

**Instruments worth reaching for.** `./lab top` (cgroup CPU throttling, memory,
OOM kills), `docker inspect`, `./lab exec jcmd 1 VM.flags`,
`./lab exec jcmd 1 VM.native_memory summary` (needs
`-XX:NativeMemoryTracking=summary`), `./lab logs app`.

**Afterwards you should be able to say**
- why the container died although the heap never ran out;
- why "50 % CPU" was full saturation, and which counter proves it;
- how much memory a JVM needs beyond `-Xmx`, and where it goes;
- which collector the JVM picks at your final sizing if you do not tell it.

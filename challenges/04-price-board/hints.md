## Hint 1
The shape of the latency tells you the class of problem before any tool does:
p50 is a few milliseconds and p99 is hundreds. Something stops the whole
process now and then. Watch `./lab gc 30` during a run: one line per second.
See which column jumps when the board freezes, and what the last two columns
(time in GC) do.

## Hint 2
`FGC` (full collections) ticks up every few seconds and each one costs around
half a second. Ask the JVM which collector it is running:
`./lab exec jcmd 1 VM.flags`. Nobody chose that collector. Find out what makes
a JVM choose it by itself (it has to do with how big the machine looks from
inside the container).

## Hint 3
The serial collector stops the world and uses one thread for everything. Minor
collections are cheap here. The full ones are not, and they are frequent
because the old generation keeps filling up. With what? Not with request
garbage, which dies young. Look for objects that live for tens of seconds and
then die: `TickStore` creates 50,000 of them per second and keeps each for
about half a minute. Too old to die in the young generation, too short-lived
to stay: every one of them becomes old-generation garbage.

## Hint 4
Two separate ways out. Flags: a collector that cleans the old generation
concurrently and incrementally (`-XX:+UseG1GC`, or ZGC) plus some headroom
above the ~250 MB live set, within the 1 GiB budget. A bigger heap alone, with
the serial collector, only makes the freezes rarer and no shorter: try it and
look at p99.9. Code (the stretch goal): if a tick is not an object there is
nothing to collect. Three primitive ring buffers per symbol (`long[]` time,
price, volume) hold the same data in a fraction of the memory with no garbage
at all.

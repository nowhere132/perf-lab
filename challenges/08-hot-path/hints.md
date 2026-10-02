## Hint 1
`perf stat` on the running benchmark: look at instructions per cycle. A CPU
that is executing code straight through manages 2 to 4. A number well under 1
means the core is mostly waiting for memory, not computing. No profiler
showing "hot methods" will explain that: the time is in the loads, not in the
instructions.

## Hint 2
Follow one `ExposureBook.tryAdd` through memory. A `long` account id is boxed
into a `Long` (allocation). The map hashes it and reads a bucket (miss), then
a node (miss), then the key object to compare (miss), then the `BigDecimal`
value (miss). Then it allocates a new `BigDecimal` for the sum. With a million
accounts picked at random, none of those objects is in cache. Four dependent
misses at roughly 80 ns each is most of your 600 ns.

## Hint 3
Account ids are dense (1 to 1,000,000) and notionals are whole numbers of
dong. So the whole book fits in a flat array of `long`, indexed by account id:
8 MB, one miss per check, nothing allocated. `AtomicLongArray` (or a `long[]`
with a `VarHandle`) gives you the compare-and-set you need for the limit
check. Allocate it in pages if you do not want to fix the capacity up front.
That alone gets about 2.5x. Not 6x.

## Hint 4
The rest is in `RoutingStats`. Each dispatcher thread updates only its own
board's counters, so there is no contention in the code. But the six counters
are six adjacent `long`s in two tiny arrays, each array within one 64-byte cache line.
Every write by one core invalidates that line in the other two cores' caches:
false sharing. `perf c2c` exists to find exactly this. Put each board's
counters on their own cache line: pad the array so that boards are 16 slots
(128 bytes) apart, or use one `LongAdder` per counter (its cells are padded
for you), or `@Contended` with `-XX:-RestrictContended`. Alone this is worth
about 1.3x, because the map's misses hide it; together with the flat array it
is worth more than 10x.

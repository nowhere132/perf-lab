# 08 · The last microsecond — solution

## Reading the evidence

Baseline: ~5 M operations/s over three threads, ~600 ns per operation, with no
I/O and no locks. Run `perf stat` on it: an instructions-per-cycle figure well
below 1 means the cores are waiting for memory rather than computing. (The
perf steps need sudo and were not run when this lab was built; the diagnosis
below is from reading the code and from the measured effect of each fix.)

**ExposureBook.** `ConcurrentHashMap<Long, BigDecimal>` with a million random
keys. One `tryAdd`: box the key (allocate), bucket (miss), node (miss), key
object for `equals` (miss), `BigDecimal` value (miss), allocate the sum. Four
dependent cache misses and two allocations per operation.

**RoutingStats.** Two `AtomicLongArray(3)`: six `long`s in two small arrays,
the three elements of each sharing a cache line. Each dispatcher
writes only its own board's slots, yet every write invalidates the line in the
other cores' caches. `perf c2c` reports the line and the offsets hit by
different threads when this is false sharing.

## Fix

`patches/08-hot-path.patch`:

- `ExposureBook`: account ids are dense and notionals are whole dong, so
  exposure is a flat table of `long` indexed by account id (`AtomicLongArray`
  pages of 65,536, allocated on first use), updated with compare-and-set for
  the limit check. One cache miss, no allocation.
- `RoutingStats`: one `AtomicLongArray` in which each board's pair of counters
  sits 16 slots (128 bytes) from the next board's. `LongAdder` per counter, or
  `@Contended`, are equivalent.

## Numbers

| | M ops/s | vs baseline |
|---|---|---|
| as inherited | 4.95 | 1.0x |
| padded counters only | 6.5 | 1.3x |
| flat exposure table only | 12.5 | 2.5x |
| both | 55–63 | 11–13x |

The product is larger than the parts because each problem hides the other:
while every operation waits ~500 ns for the map, the cache line has time to
come back between writes; once the map is fast, the counters are all that is
left and the line bounces on every operation.

## Take away

- At this scale the cost model is cache misses and allocations, not
  instructions. IPC from `perf stat` tells you which regime you are in.
- Pointer-rich structures of boxed objects pay one miss per pointer. Dense
  primitive arrays pay one per element, or less when access is sequential.
- Threads that share no data can still share a cache line.

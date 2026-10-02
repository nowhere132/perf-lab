## Hint 1
`./lab ash` after a failing run: nearly all database time is one statement,
`COMMIT`, waiting on `WALWrite` / `WalSync`. The queries are fine. A commit
returns only after its WAL record is on the device, and the device does 400
writes a second. Count the commits per order (after challenge 02 there are
two), then compare 400 with what 450 orders/s needs. It cannot work one flush
per commit.

## Hint 2
One flush can make many commits durable at once if they are all in the WAL
buffer when the flush starts (group commit). Measure how many you get now:
commits per second from `pg_stat_database.xact_commit`, flushes per second
from the `wal` rows of `pg_stat_io`. With at most 10 connections the ratio is
poor, because each backend flushes as soon as it is ready. Postgres has a pair
of settings that make a backend wait a little for company before it flushes.

## Hint 3
`commit_delay` (microseconds) with `commit_siblings`. That raises the ceiling
but not enough. Now ask who else is writing. Compare the container's device
write count (`./lab top`, or `io.stat` in the cgroup) with the WAL flush count
over the same interval: most device writes are not WAL. `pg_stat_io` shows
which backend types write relation pages, and the `background writer` and
`client backend` rows should not be doing much of that in a healthy system.

## Hint 4
`shared_buffers` is 128 MB and the pages this workload keeps dirtying
(accounts, positions, their indexes) are several times that. Dirty pages are
evicted to make room, land in the OS page cache, and the kernel writes them
out within seconds, from the same 400 IOPS. Those pages get dirtied again a
moment later. Give Postgres enough buffers to hold the hot set (the container
has 2 GB) so a page is written once per checkpoint, and then make checkpoints
rare and spread out: `max_wal_size`, `checkpoint_timeout`. The checkpoint
lines in `./lab logs db` show how often they fired ("checkpoint starting: wal"
means the WAL filled up) and how long the sync took. `wal_compression` shrinks
the full-page images that follow each checkpoint.

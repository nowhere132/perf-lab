# 06 · Same code, slower in the new datacentre — solution

## Reading the evidence

`./lab ash`: ~85 % of database time is `COMMIT` waiting on
`LWLock / WALWrite` and `IO / WalSync`. Statements are fast; making them
durable is slow.

Budget: 400 device writes per second. `pg_test_fsync` on the volume shows
`fdatasync` at ~200 per second, so one WAL flush costs two device writes. Each
order commits twice. With one flush per commit the ceiling would be 100
orders/s; group commit (several commits made durable by one flush) is the only
reason it reaches ~200.

Two separate things are wrong.

**1. Too few commits per flush.** Over 30 s of a failing run: 12,564 commits
(`pg_stat_database.xact_commit`) and 2,535 WAL fsyncs (`pg_stat_io`,
`object = 'wal'`), so about 5 commits per flush. With only 10 connections,
each backend starts its own flush as soon as it is ready.

**2. Most of the IOPS are not going to the WAL.** Device writes from the
cgroup's `io.stat` over the same 30 s: 12,120, the full 400/s. The 2,535 WAL
flushes account for about 5,000 of them. The rest are data pages.
`pg_stat_io` shows `background writer` and `client backend` writing relation
pages. `shared_buffers` is 128 MB while the pages this workload dirties
(accounts, positions and their indexes, ~500 MB) do not fit, so dirty pages
are evicted into the OS page cache, the kernel writes them back within
seconds, and the workload dirties them again. On top of that, the WAL fills
`max_wal_size = 1GB` in about a minute, so checkpoints run back to back
(`checkpoint starting: wal` in the log), and the first change to each page
after a checkpoint logs the whole 8 kB page.

## Fix

`challenges/06-cloud-disk/postgresql.conf`:

    shared_buffers = 768MB        # hot set stays in Postgres; one write per page per checkpoint
    commit_delay = 2000           # wait up to 2 ms for company before flushing
    commit_siblings = 2
    max_wal_size = 8GB            # checkpoints by the clock, not by WAL volume
    checkpoint_timeout = 30min
    wal_compression = lz4         # smaller full-page images

## Numbers (450 orders/s offered)

| settings | achieved | p99 |
|---|---|---|
| as provisioned | 197–207/s | 5–6 s |
| + `commit_delay` | 366/s | 3.1 s |
| + `commit_delay`, checkpoints far apart (128 MB buffers) | 336/s | 4.0 s |
| + `commit_delay`, 768 MB buffers (default checkpoints) | 450/s | 210 ms |
| all of the above | 450/s | 115 ms |
| buffers + checkpoints, no `commit_delay` | 270/s | 3.8 s |
| `wal_sync_method = open_datasync` instead of the default | no measurable change here |

Neither half works alone: group commit without fixing the writeback is
starved of IOPS, and fixing the writeback without group commit still pays a
flush for every three commits.

## What each setting trades away

- `commit_delay`: every commit that finds siblings waits up to 2 ms more. On a
  fast disk that is pure loss; here a flush takes 5 ms anyway.
- `shared_buffers` at 768 MB: memory, and a larger shutdown checkpoint.
- Rare checkpoints: longer crash recovery (more WAL to replay) and more disk
  used by WAL.

An application-side alternative with the same effect as group commit is fewer
commits per order, but the two-transaction shape from challenge 02 exists for
a reason.

## Take away

- On a slow device the unit of cost is the flush, not the statement. Count
  flushes per business operation.
- The WAL and the data files share the device. Anything that makes Postgres
  write the same page repeatedly (small buffer pool, frequent checkpoints)
  takes IOPS away from commits.
- The OS page cache is not free storage for dirty pages: the kernel will write
  them, on its schedule, from your budget.

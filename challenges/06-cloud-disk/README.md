# 06 · Same code, slower in the new datacentre

*Needs challenges 02 and 03 solved.*

**Situation.** The database moved to the new datacentre, onto a network volume
provisioned at 400 write IOPS, with a `postgresql.conf` that is essentially
what `initdb` generates (`challenges/06-cloud-disk/`). The same router that
carried 800 orders/s now struggles at a quarter of that.

**Goal.** `./lab test` offers 450 orders/s.

| | target |
|---|---|
| p99 latency | < 200 ms |
| errors | < 0.1 % |

**Rules.**
- The volume stays at 400 write IOPS (the grader checks). That is what was paid for.
- Durability is not negotiable: `fsync`, `full_page_writes` and
  `synchronous_commit` stay `on` (the grader checks), and no order state is
  acknowledged to the client before it is on disk.
- Pool maximum stays at 10 or less.

**You may change** `challenges/06-cloud-disk/postgresql.conf` (then
`./lab restart db`) and anything under `app/`.

**Instruments worth reaching for.** `./lab ash`, `./lab top` (device IO
counters), `pg_stat_io`, `pg_stat_wal`, `pg_stat_checkpointer`, the checkpoint
lines in `./lab logs db`, and Postgres' own disk probe:

    docker exec -u postgres perflab-db-1 /usr/lib/postgresql/18/bin/pg_test_fsync -s 2 -f /var/lib/postgresql/fsync.test

**Before you measure,** work out the budget: how many device writes does one
order cost, and who else is spending from the same 400 per second?

**Afterwards you should be able to say**
- what a commit is waiting for, and how many commits one device write can carry;
- which writes other than the WAL were competing for the budget, and what made them happen so often;
- what each setting you changed trades away.

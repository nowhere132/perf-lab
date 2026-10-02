-- Active Session History for Postgres: sample pg_stat_activity at 10 Hz into a
-- table, so you can look at *when* things went wrong instead of staring at a
-- live counter. This is the same idea as Oracle ASH / pgsentinel, in 40 lines.
CREATE SCHEMA IF NOT EXISTS ash;

CREATE UNLOGGED TABLE IF NOT EXISTS ash.samples(
  ts               timestamptz NOT NULL,
  run_label        text,
  pid              int,
  state            text,
  wait_event_type  text,          -- NULL + state='active'  =>  on CPU
  wait_event       text,
  application_name text,
  query_id         bigint,
  xact_age_ms      numeric,
  blocking_pids    int[],
  query            text
);
CREATE INDEX IF NOT EXISTS samples_ts_idx    ON ash.samples(ts);
CREATE INDEX IF NOT EXISTS samples_label_idx ON ash.samples(run_label, ts);

-- One backend loops, snapshots, commits, sleeps. COMMIT each iteration keeps it
-- from becoming the long-running transaction that ruins everyone else's day.
CREATE OR REPLACE PROCEDURE ash.sample_loop(
  duration_s int    DEFAULT 60,
  label      text   DEFAULT 'run',
  hz         numeric DEFAULT 10
) LANGUAGE plpgsql AS $$
DECLARE
  stop timestamptz := clock_timestamp() + make_interval(secs => duration_s);
  gap  double precision := 1.0 / hz;
  at   timestamptz;
BEGIN
  WHILE clock_timestamp() < stop LOOP
    at := clock_timestamp();   -- pinned: one instant == one sample
    INSERT INTO ash.samples
    SELECT at, label, a.pid, a.state, a.wait_event_type, a.wait_event,
           a.application_name, a.query_id,
           extract(epoch FROM (at - a.xact_start)) * 1000,
           nullif(pg_blocking_pids(a.pid), '{}'),
           left(a.query, 120)
    FROM pg_stat_activity a
    WHERE a.backend_type = 'client backend'
      AND a.pid <> pg_backend_pid()
      AND coalesce(a.state, '') <> 'idle';
    COMMIT;
    PERFORM pg_sleep(gap);
  END LOOP;
END $$;

-- ── Average Active Sessions ────────────────────────────────────────────────
-- AAS = (rows in bucket) / (sample instants in bucket)
--     = how many backends were simultaneously busy, on average.
-- This is the single number that replaces "is the DB slow?" with an answer:
--   AAS_cpu  > #cores      -> genuinely CPU-saturated. Cache/shard/scale up.
--   AAS_lock large         -> contention. Fix the transaction, not the hardware.
--   AAS total ~ 1-2        -> the DB is idle and your problem is somewhere else.

CREATE OR REPLACE VIEW ash.aas_by_class AS
WITH n AS (
  SELECT run_label, date_trunc('second', ts) AS t, count(DISTINCT ts) AS samples
  FROM ash.samples GROUP BY 1, 2
)
SELECT s.run_label,
       n.t,
       CASE WHEN s.wait_event_type IS NULL THEN 'CPU' ELSE s.wait_event_type END AS wait_class,
       round(count(*)::numeric / max(n.samples), 2) AS aas
FROM ash.samples s
JOIN n ON n.run_label IS NOT DISTINCT FROM s.run_label
      AND n.t = date_trunc('second', s.ts)
GROUP BY 1, 2, 3;

CREATE OR REPLACE VIEW ash.aas_by_event AS
WITH n AS (
  SELECT run_label, date_trunc('second', ts) AS t, count(DISTINCT ts) AS samples
  FROM ash.samples GROUP BY 1, 2
)
SELECT s.run_label,
       n.t,
       coalesce(s.wait_event_type, 'CPU') || ' / ' || coalesce(s.wait_event, 'on-cpu') AS wait_event,
       round(count(*)::numeric / max(n.samples), 2) AS aas
FROM ash.samples s
JOIN n ON n.run_label IS NOT DISTINCT FROM s.run_label
      AND n.t = date_trunc('second', s.ts)
GROUP BY 1, 2, 3;

-- Where did the time go, for a whole run? The terminal answer.
CREATE OR REPLACE VIEW ash.profile AS
WITH n AS (SELECT run_label, count(DISTINCT ts) AS samples FROM ash.samples GROUP BY 1)
SELECT s.run_label,
       coalesce(s.wait_event_type, 'CPU')      AS wait_class,
       coalesce(s.wait_event, 'on-cpu')        AS wait_event,
       count(*)                                AS sample_rows,
       round(count(*)::numeric / max(n.samples), 2) AS aas,
       round(100.0 * count(*) / sum(count(*)) OVER (PARTITION BY s.run_label), 1) AS pct_of_db_time
FROM ash.samples s JOIN n ON n.run_label IS NOT DISTINCT FROM s.run_label
GROUP BY 1, 2, 3 ORDER BY 1, 4 DESC;

-- Who blocked whom, and for how long. Only meaningful when wait_class='Lock'.
CREATE OR REPLACE VIEW ash.blocking AS
SELECT b.run_label,
       b.query        AS blocked_query,
       blk.query      AS blocking_query,
       blk.wait_event_type AS blocker_was_waiting_on,
       count(*)       AS samples_blocked,
       round(count(*) / 10.0, 1) AS approx_seconds_blocked   -- at 10 Hz
FROM ash.samples b
CROSS JOIN LATERAL unnest(b.blocking_pids) AS bp(pid)
LEFT JOIN ash.samples blk ON blk.ts = b.ts AND blk.pid = bp.pid
WHERE b.blocking_pids IS NOT NULL
GROUP BY 1, 2, 3, 4 ORDER BY 5 DESC;

-- ── Throughput, so AAS has a denominator ───────────────────────────────────
-- AAS alone cannot tell you good from bad: 15 sessions on CPU is excellent at
-- 26k TPS and a disaster at 200 TPS. Sample the cumulative counters at 1 Hz and
-- take deltas.
CREATE UNLOGGED TABLE IF NOT EXISTS ash.dbstats(
  ts           timestamptz NOT NULL,
  run_label    text,
  xact_commit  bigint, xact_rollback bigint,
  blks_hit     bigint, blks_read     bigint,
  tup_updated  bigint, tup_inserted  bigint,
  wal_records  bigint, wal_bytes     numeric
);
CREATE INDEX IF NOT EXISTS dbstats_label_idx ON ash.dbstats(run_label, ts);

CREATE OR REPLACE VIEW ash.tps AS
SELECT run_label,
       ts,
       round(  (xact_commit  - lag(xact_commit)  OVER w)
             / extract(epoch FROM ts - lag(ts) OVER w), 1)               AS commits_per_s,
       round(  (xact_rollback - lag(xact_rollback) OVER w)
             / extract(epoch FROM ts - lag(ts) OVER w), 1)               AS rollbacks_per_s,
       round(  (tup_updated  - lag(tup_updated)  OVER w)
             / extract(epoch FROM ts - lag(ts) OVER w), 1)               AS rows_updated_per_s,
       round(  (wal_bytes    - lag(wal_bytes)    OVER w)
             / extract(epoch FROM ts - lag(ts) OVER w) / 1048576.0, 2)   AS wal_mb_per_s,
       round(100.0 * (blks_hit - lag(blks_hit) OVER w)
             / nullif((blks_hit - lag(blks_hit) OVER w)
                    + (blks_read - lag(blks_read) OVER w), 0), 2)        AS cache_hit_pct
FROM ash.dbstats
WINDOW w AS (PARTITION BY run_label ORDER BY ts);

-- Re-create the sampler so it records both, waits at 10 Hz and counters at 1 Hz.
CREATE OR REPLACE PROCEDURE ash.sample_loop(
  duration_s int    DEFAULT 60,
  label      text   DEFAULT 'run',
  hz         numeric DEFAULT 10
) LANGUAGE plpgsql AS $$
DECLARE
  stop timestamptz := clock_timestamp() + make_interval(secs => duration_s);
  gap  double precision := 1.0 / hz;
  at   timestamptz;
  tick int := 0;
BEGIN
  WHILE clock_timestamp() < stop LOOP
    at := clock_timestamp();   -- pinned: one instant == one sample
    INSERT INTO ash.samples
    SELECT at, label, a.pid, a.state, a.wait_event_type, a.wait_event,
           a.application_name, a.query_id,
           extract(epoch FROM (at - a.xact_start)) * 1000,
           nullif(pg_blocking_pids(a.pid), '{}'),
           left(a.query, 120)
    FROM pg_stat_activity a
    WHERE a.backend_type = 'client backend'
      AND a.pid <> pg_backend_pid()
      AND coalesce(a.state, '') <> 'idle';

    IF tick % greatest((hz)::int, 1) = 0 THEN
      INSERT INTO ash.dbstats
      SELECT at, label, d.xact_commit, d.xact_rollback, d.blks_hit, d.blks_read,
             d.tup_updated, d.tup_inserted, w.wal_records, w.wal_bytes
      FROM pg_stat_database d CROSS JOIN pg_stat_wal w
      WHERE d.datname = current_database();
    END IF;

    tick := tick + 1;
    COMMIT;
    PERFORM pg_sleep(gap);
  END LOOP;
END $$;

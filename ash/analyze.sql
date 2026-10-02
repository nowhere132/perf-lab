\echo '── Where database time went, per run ──'
SELECT run_label, wait_class, wait_event, aas, pct_of_db_time
FROM ash.profile WHERE pct_of_db_time >= 1 ORDER BY run_label, pct_of_db_time DESC;

\echo ''
\echo '── Peak concurrency per run (total AAS, and how much of it was waiting) ──'
SELECT run_label,
       round(sum(aas) FILTER (WHERE wait_class = 'CPU'), 2)  AS aas_on_cpu,
       round(sum(aas) FILTER (WHERE wait_class = 'Lock'), 2) AS aas_on_locks,
       round(sum(aas), 2)                                    AS aas_total
FROM ash.profile GROUP BY 1 ORDER BY 1;

\echo ''
\echo '── Longest blocking chains ──'
SELECT run_label, approx_seconds_blocked,
       left(blocked_query, 45) AS blocked, left(blocking_query, 45) AS blocker
FROM ash.blocking ORDER BY approx_seconds_blocked DESC LIMIT 15;

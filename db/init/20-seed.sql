-- 1,600 symbols, 1M accounts, 2M positions, 3M historical orders with their
-- fills and events. Deterministic where the load generator needs to know the
-- data (symbol codes, prices, who holds what); random where it does not.
\timing on
SET synchronous_commit = off;
SET maintenance_work_mem = '512MB';

CREATE FUNCTION sym(i int) RETURNS text IMMUTABLE PARALLEL SAFE LANGUAGE sql
RETURN chr(65 + i / 676) || chr(65 + (i / 26) % 26) || chr(65 + i % 26);

INSERT INTO symbols
SELECT sym(i),
       CASE WHEN i < 400 THEN 'HOSE' WHEN i < 700 THEN 'HNX' ELSE 'UPCOM' END,
       10000 + ((i * 37) % 900) * 100,
       CASE WHEN i < 400 THEN 7 WHEN i < 700 THEN 10 ELSE 15 END,
       100, 100
FROM generate_series(0, 1599) i;

INSERT INTO trading_limits
SELECT symbol, 500000, 1000000 + (hashtext(symbol) & 1048575) FROM symbols;

INSERT INTO accounts (id, account_no, status, tier, cash)
SELECT g, '0' || lpad(g::text, 9, '0'), 'ACTIVE', (g % 4)::smallint, 100000000000
FROM generate_series(1, 1000000) g;

-- account g holds symbol #(g mod 1600) and symbol #((7g+3) mod 1600)
INSERT INTO positions (account_id, symbol, qty)
SELECT g, sym(i), 100000000
FROM generate_series(1, 1000000) g
CROSS JOIN LATERAL (VALUES (g % 1600), ((g * 7 + 3) % 1600)) v(i);

-- History. Accounts 1..50,000 are the active traders: 50 orders each.
-- The other 500k orders are spread thinly over everyone else.
INSERT INTO orders (id, account_id, symbol, side, qty, price, status, venue, venue_order_id, created_at, updated_at)
SELECT g,
       CASE WHEN g <= 2500000 THEN 1 + g % 50000
            ELSE 50001 + (random() * 949999)::int END,
       sym(i),
       CASE WHEN random() < 0.5 THEN 'BUY' ELSE 'SELL' END,
       (1 + (random() * 49)::int) * 100,
       10000 + ((i * 37) % 900) * 100,
       CASE WHEN g % 5 = 0 THEN 'ACCEPTED' ELSE 'FILLED' END,
       CASE WHEN i < 400 THEN 'HOSE' WHEN i < 700 THEN 'HNX' ELSE 'UPCOM' END,
       'H' || g,
       ts, ts
FROM (SELECT g, ((g::bigint * 7919) % 1600)::int AS i,
             now() - random() * interval '90 days' AS ts
      FROM generate_series(1, 3000000) g) x;

-- 80% of orders are filled; 30% of those in two clips.
INSERT INTO fills (id, order_id, qty, price, executed_at)
SELECT row_number() OVER (), o.id, o.qty / n.clips, o.price, o.created_at + interval '1 second' * c
FROM orders o
CROSS JOIN LATERAL (SELECT CASE WHEN o.id % 10 < 3 THEN 2 ELSE 1 END AS clips) n
CROSS JOIN LATERAL generate_series(1, n.clips) c
WHERE o.status = 'FILLED';

INSERT INTO order_events (id, order_id, type, detail, created_at)
SELECT o.id, o.id, 'ACCEPTED', 'venue=' || o.venue, o.created_at
FROM orders o;

CREATE INDEX fills_order_id_idx        ON fills (order_id);
CREATE INDEX order_events_order_id_idx ON order_events (order_id);

-- The application allocates ids in blocks of 50 (Hibernate pooled optimizer).
CREATE SEQUENCE orders_seq       INCREMENT BY 50 START WITH 10000000;
CREATE SEQUENCE fills_seq        INCREMENT BY 50 START WITH 10000000;
CREATE SEQUENCE order_events_seq INCREMENT BY 50 START WITH 10000000;

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO router;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO router;
DROP FUNCTION sym(int);

VACUUM (ANALYZE);

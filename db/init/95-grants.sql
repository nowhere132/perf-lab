-- ash schema comes from ../ash/install.sql, mounted as 90-ash.sql
GRANT pg_monitor TO grafana;
GRANT USAGE ON SCHEMA ash TO grafana;
GRANT SELECT ON ALL TABLES IN SCHEMA ash TO grafana;
ALTER DEFAULT PRIVILEGES IN SCHEMA ash GRANT SELECT ON TABLES TO grafana;

CREATE TABLE lab_migrations (file text PRIMARY KEY, applied_at timestamptz NOT NULL DEFAULT now());

-- Order-routing schema. This file is the schema "as inherited": it runs once,
-- when the database volume is created. Your own changes go in db/migrations/
-- (see ./lab migrate) so they survive ./lab reseed.

CREATE ROLE router  LOGIN PASSWORD 'router'  CONNECTION LIMIT 12;
CREATE ROLE grafana LOGIN PASSWORD 'grafana';

CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

CREATE TABLE symbols (
    symbol     text PRIMARY KEY,
    board      text          NOT NULL,          -- HOSE | HNX | UPCOM
    ref_price  numeric(12,2) NOT NULL,
    band_pct   numeric(5,2)  NOT NULL,          -- daily price limit, +/- %
    tick_size  numeric(12,2) NOT NULL,
    lot_size   int           NOT NULL
);

CREATE TABLE trading_limits (
    symbol        text PRIMARY KEY REFERENCES symbols,
    max_order_qty int    NOT NULL,
    foreign_room  bigint NOT NULL
);

CREATE TABLE accounts (
    id          bigint PRIMARY KEY,
    account_no  text          NOT NULL UNIQUE,
    status      text          NOT NULL,         -- ACTIVE | SUSPENDED
    tier        smallint      NOT NULL,
    cash        numeric(20,2) NOT NULL,
    held        numeric(20,2) NOT NULL DEFAULT 0,
    updated_at  timestamptz   NOT NULL DEFAULT now()
);

CREATE TABLE positions (
    account_id bigint NOT NULL,
    symbol     text   NOT NULL,
    qty        bigint NOT NULL,
    held_qty   bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (account_id, symbol)
);

CREATE TABLE orders (
    id              bigint PRIMARY KEY,
    account_id      bigint        NOT NULL,
    symbol          text          NOT NULL,
    side            text          NOT NULL,     -- BUY | SELL
    qty             int           NOT NULL,
    price           numeric(12,2) NOT NULL,
    status          text          NOT NULL,     -- NEW | ACCEPTED | REJECTED | FILLED
    venue           text          NOT NULL,
    venue_order_id  text,
    created_at      timestamptz   NOT NULL DEFAULT now(),
    updated_at      timestamptz   NOT NULL DEFAULT now()
);

CREATE TABLE fills (
    id          bigint PRIMARY KEY,
    order_id    bigint        NOT NULL,
    qty         int           NOT NULL,
    price       numeric(12,2) NOT NULL,
    executed_at timestamptz   NOT NULL
);

CREATE TABLE order_events (
    id         bigint PRIMARY KEY,
    order_id   bigint      NOT NULL,
    type       text        NOT NULL,
    detail     text,
    created_at timestamptz NOT NULL DEFAULT now()
);

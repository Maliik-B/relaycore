-- Transactional outbox: domain events captured in the same transaction as the state change that
-- produced them, so a DB write and a Kafka publish never have to be a (impossible) shared transaction.
-- A relay polls PENDING rows, publishes them, and flips them to SENT. payload is a JSON string
-- (varchar, not jsonb) — safest under ddl-auto: validate and consistent with idempotency_keys.
create table outbox_events (
    id             uuid          primary key,
    aggregate_type varchar(64)   not null,
    aggregate_id   varchar(64)   not null,
    type           varchar(64)   not null,
    topic          varchar(128)  not null,
    payload        varchar(8000) not null,
    status         varchar(16)   not null,
    created_at     timestamptz   not null,
    sent_at        timestamptz
);

-- Partial index over just the unpublished backlog: keeps the relay's oldest-first poll cheap as the
-- table fills with SENT history.
create index ix_outbox_pending on outbox_events (created_at) where status = 'PENDING';

-- M4: matchmaking queue. A player enqueues a ticket (region + skill bucket); a background worker groups
-- QUEUED tickets by (region, skill_bucket) into matches and flips them to MATCHED with the formed match
-- id. version gives optimistic locking; player_id is an opaque reference into the players module (no FK).
create table matchmaking_tickets (
    id           uuid        primary key,
    player_id    uuid        not null,
    region       varchar(32) not null,
    skill_bucket integer     not null,
    status       varchar(16) not null,
    match_id     uuid,
    version      bigint      not null,
    created_at   timestamptz not null,
    updated_at   timestamptz not null
);

-- Supports the worker's oldest-first scan of the open queue, grouped by bucket.
create index ix_ticket_queued on matchmaking_tickets (region, skill_bucket, created_at) where status = 'QUEUED';

-- At most one open ticket per player: a second concurrent enqueue loses this unique race (-> 409) rather
-- than letting a player sit in the queue twice (which could otherwise self-match).
create unique index uq_ticket_player_queued on matchmaking_tickets (player_id) where status = 'QUEUED';

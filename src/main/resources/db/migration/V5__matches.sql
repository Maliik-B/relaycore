-- Matches: the event source for the reward loop. M3 records completed results only; reporting a result
-- writes this row AND a match-completed outbox event in one transaction. The full FORMING -> ACTIVE ->
-- COMPLETED lifecycle (queue + worker) lands in M4. winner_id is an opaque reference into the players
-- module — no cross-module FK.
create table matches (
    id           uuid        primary key,
    status       varchar(16) not null,
    winner_id    uuid        not null,
    created_at   timestamptz not null,
    completed_at timestamptz
);

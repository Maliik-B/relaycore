-- M4: matches gain a real lifecycle. The worker forms an ACTIVE match from queued tickets; reporting a
-- result transitions it to COMPLETED. winner_id is therefore unknown until completion (nullable), and a
-- version column guards the ACTIVE -> COMPLETED transition against a concurrent double-report.
-- region/skill_bucket record the bucket the match was formed from. (matches is empty pre-M4, so the new
-- NOT NULL columns need no backfill.)
alter table matches
    alter column winner_id    drop not null,
    add  column region        varchar(32) not null,
    add  column skill_bucket  integer     not null,
    add  column version       bigint      not null default 0;

-- Participants the worker grouped into a match. player_id is an opaque reference into the players module
-- (no cross-module FK); match_id is FK'd within this module. The unique (match_id, player_id) keeps a
-- player from appearing twice in one match.
create table match_participants (
    id         uuid        primary key,
    match_id   uuid        not null,
    player_id  uuid        not null,
    created_at timestamptz not null,
    constraint uq_match_player unique (match_id, player_id),
    constraint fk_participant_match foreign key (match_id) references matches (id)
);

create index ix_participant_match on match_participants (match_id);

-- Players / auth: the identity every other module hangs off.
-- Flyway owns the schema; the Player entity is mapped against this table with ddl-auto=validate.
create table players (
    id             uuid          primary key,
    username       varchar(32)   not null,
    email          varchar(255)  not null,
    password_hash  varchar(100)  not null,
    display_name   varchar(64)   not null,
    matches_played integer       not null default 0,
    wins           integer       not null default 0,
    losses         integer       not null default 0,
    created_at     timestamptz   not null,
    updated_at     timestamptz   not null,
    constraint uq_players_username unique (username),
    constraint uq_players_email    unique (email)
);

-- Idempotency keys: replay-safe record of a processed mutating request. The unique (scope, key)
-- constraint is the serialization point that makes concurrent same-key requests grant exactly once.
create table idempotency_keys (
    id              uuid          primary key,
    scope           varchar(64)   not null,
    idempotency_key varchar(200)  not null,
    request_hash    varchar(64),
    response_body   varchar(8000) not null,
    created_at      timestamptz   not null,
    constraint uq_idempotency_scope_key unique (scope, idempotency_key)
);

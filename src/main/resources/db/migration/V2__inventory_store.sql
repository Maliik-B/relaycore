-- Inventory / store: the depth domain. Currency wallet + item inventory (both optimistically locked)
-- and a store catalog. player_id is an opaque reference into the players module — no cross-module FK
-- (modules stay decoupled); item_id is FK'd to the catalog within this module.

create table catalog_items (
    item_id varchar(64)  primary key,
    name    varchar(128) not null,
    price   bigint       not null check (price >= 0),
    enabled boolean      not null default true
);

insert into catalog_items (item_id, name, price, enabled) values
    ('potion_health', 'Health Potion',  25, true),
    ('emote_wave',    'Wave Emote',      50, true),
    ('shield_wood',   'Wooden Shield',   75, true),
    ('sword_iron',    'Iron Sword',     100, true),
    ('skin_gold',     'Gold Skin',      500, true),
    ('legacy_banner', 'Legacy Banner',  999, false);

create table wallets (
    player_id  uuid        primary key,
    balance    bigint      not null default 0 check (balance >= 0),
    version    bigint      not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table inventory_items (
    id         uuid        primary key,
    player_id  uuid        not null,
    item_id    varchar(64) not null,
    quantity   integer     not null check (quantity >= 0),
    version    bigint      not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uq_inventory_player_item unique (player_id, item_id),
    constraint fk_inventory_item foreign key (item_id) references catalog_items (item_id)
);

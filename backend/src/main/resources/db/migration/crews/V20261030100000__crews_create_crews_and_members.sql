-- Crews and their members (M1, plan §3). Text limits are checked by Bean Validation, not here, so a limit change needs
-- no migration. created_by/updated_by hold User ids without a foreign key: they must survive account deletion (ACC-3).

CREATE TABLE crews (
    id                uuid        PRIMARY KEY,
    name              text        NOT NULL,
    established_month date        NOT NULL CHECK (extract(day FROM established_month) = 1),
    motto             text,
    description       text,
    icon_key          text        NOT NULL CHECK (icon_key IN ('USERS', 'DICE', 'DINING', 'FILM', 'BEER', 'GAMEPAD',
                                                               'TROPHY', 'MUSIC', 'MOUNTAIN', 'TENT', 'PIZZA', 'COFFEE')),
    time_zone         text        NOT NULL,
    version           bigint      NOT NULL,
    created_at        timestamptz NOT NULL,
    created_by        uuid        NOT NULL,
    updated_at        timestamptz NOT NULL,
    updated_by        uuid        NOT NULL,
    deleted_at        timestamptz
);

-- The purge job (M1-7) looks only at deleted crews.
CREATE INDEX crews_deleted_at_idx ON crews (deleted_at) WHERE deleted_at IS NOT NULL;

CREATE TABLE members (
    id               uuid        PRIMARY KEY,
    -- The purge deletes a crew's members with it.
    crew_id          uuid        NOT NULL REFERENCES crews (id) ON DELETE CASCADE,
    -- Null for historical members, who have no account.
    user_id          uuid        REFERENCES users (id),
    role             text        NOT NULL CHECK (role IN ('ADMIN', 'FRIEND', 'NEW_GUY')),
    first_name       text        NOT NULL,
    last_name        text,
    nickname         text,
    pronouns         text,
    joined_month     date        CHECK (extract(day FROM joined_month) = 1),
    birth_month      smallint    CHECK (birth_month BETWEEN 1 AND 12),
    birth_day        smallint    CHECK (birth_day BETWEEN 1 AND 31),
    birth_year       smallint,
    phone            text,
    email            text,
    food_preferences text,
    status           text        NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    -- The members list's sort order (M1-11): nickname if present, otherwise first name, then last name.
    sort_name        text        NOT NULL GENERATED ALWAYS AS (lower(coalesce(nullif(nickname, ''), first_name))) STORED,
    sort_last_name   text        NOT NULL GENERATED ALWAYS AS (lower(coalesce(last_name, ''))) STORED,
    version          bigint      NOT NULL,
    created_at       timestamptz NOT NULL,
    created_by       uuid        NOT NULL,
    updated_at       timestamptz NOT NULL,
    updated_by       uuid        NOT NULL,
    CONSTRAINT members_birthday_month_and_day CHECK ((birth_month IS NULL) = (birth_day IS NULL)),
    CONSTRAINT members_birth_year_needs_date CHECK (birth_year IS NULL OR birth_month IS NOT NULL)
);

-- One Member per User per crew.
CREATE UNIQUE INDEX members_crew_user_uq ON members (crew_id, user_id) WHERE user_id IS NOT NULL;
CREATE INDEX members_user_id_idx ON members (user_id);
-- Keyset paging of the members list in its sort order (ACTIVE sorts before INACTIVE); also serves crew_id lookups.
CREATE INDEX members_list_order_idx ON members (crew_id, status, sort_name, sort_last_name, id);

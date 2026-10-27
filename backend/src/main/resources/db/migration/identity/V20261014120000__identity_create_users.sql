-- CrewUp users, provisioned on their first authenticated call (M0-6). One row per OIDC subject (the token's sub).
-- The table is "users" because "user" is reserved in PostgreSQL.

CREATE TABLE users (
    id           uuid        PRIMARY KEY,
    subject      text        NOT NULL UNIQUE,
    email        text,
    display_name text        NOT NULL,
    created_at   timestamptz NOT NULL,
    updated_at   timestamptz NOT NULL
);

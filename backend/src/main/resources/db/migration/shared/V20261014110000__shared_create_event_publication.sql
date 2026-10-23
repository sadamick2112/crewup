-- Spring Modulith's event publication registry (spring-modulith-starter-jpa, plan §3). Modules publish
-- application events (MOD-3), and the registry stores each publication until its listener completes, so
-- events survive a restart. Columns match Modulith 2.1's JPA mapping (Hibernate validates them at startup).
-- The archive table backs the optional ARCHIVE completion mode; Hibernate validates it too.

CREATE TABLE event_publication (
    id                     uuid        NOT NULL PRIMARY KEY,
    listener_id            text        NOT NULL,
    event_type             text        NOT NULL,
    serialized_event       text        NOT NULL,
    publication_date       timestamptz NOT NULL,
    completion_date        timestamptz,
    status                 text,
    completion_attempts    integer,
    last_resubmission_date timestamptz
);

CREATE INDEX event_publication_serialized_event_hash_idx ON event_publication USING hash (serialized_event);
CREATE INDEX event_publication_by_completion_date_idx ON event_publication (completion_date);

CREATE TABLE event_publication_archive (
    id                     uuid        NOT NULL PRIMARY KEY,
    listener_id            text        NOT NULL,
    event_type             text        NOT NULL,
    serialized_event       text        NOT NULL,
    publication_date       timestamptz NOT NULL,
    completion_date        timestamptz,
    status                 text,
    completion_attempts    integer,
    last_resubmission_date timestamptz
);

CREATE INDEX event_publication_archive_serialized_event_hash_idx ON event_publication_archive USING hash (serialized_event);
CREATE INDEX event_publication_archive_by_completion_date_idx ON event_publication_archive (completion_date);

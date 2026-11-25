CREATE TABLE IF NOT EXISTS script_store
(
    name         TEXT    NOT NULL,
    owner        BLOB    NOT NULL,
    script       TEXT    NOT NULL,
    published_at INTEGER NOT NULL,
    PRIMARY KEY (name, owner)
);

CREATE INDEX IF NOT EXISTS script_store_published_at ON script_store (published_at DESC, owner, name);

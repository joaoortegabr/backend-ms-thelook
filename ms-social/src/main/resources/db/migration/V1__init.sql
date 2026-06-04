CREATE TABLE IF NOT EXISTS tb_creator (
    id              UUID        PRIMARY KEY,
    user_id         UUID        NOT NULL UNIQUE,
    name            VARCHAR(64),
    avatar_url      VARCHAR(255),
    bio             TEXT,
    instagram       VARCHAR(64),
    birth_date      DATE,
    city            VARCHAR(255),
    uf              VARCHAR(255),
    created_at      DATE,
    is_active       BOOLEAN     NOT NULL DEFAULT false,
    followers_count BIGINT      NOT NULL DEFAULT 0,
    deleted_at      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_creator_user_id ON tb_creator(user_id);

CREATE TABLE IF NOT EXISTS tb_outbox (
    id              UUID    PRIMARY KEY,
    aggregate_id    VARCHAR(255),
    type            VARCHAR(255),
    payload         TEXT,
    created_at      TIMESTAMP,
    processed       BOOLEAN NOT NULL DEFAULT false,
    retry_count     INT     NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_outbox_unprocessed ON tb_outbox(processed, created_at)
    WHERE processed = false;
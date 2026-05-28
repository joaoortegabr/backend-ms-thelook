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
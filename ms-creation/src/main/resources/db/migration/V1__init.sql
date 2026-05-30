CREATE TABLE IF NOT EXISTS tb_outfit (
    id           UUID         PRIMARY KEY,
    creator_id   UUID         NOT NULL,
    title        VARCHAR(64)  NOT NULL,
    image1_url   VARCHAR(255) NOT NULL,
    image2_url   VARCHAR(255),
    style        VARCHAR(50)  NOT NULL,
    image_status VARCHAR(50),
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_outfit_creator_id   ON tb_outfit(creator_id);
CREATE INDEX IF NOT EXISTS idx_outfit_image_status ON tb_outfit(image_status);

CREATE TABLE IF NOT EXISTS tb_outfit_colors (
    outfit_id UUID        NOT NULL REFERENCES tb_outfit(id),
    colors    VARCHAR(50) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_outfit_colors_outfit_id ON tb_outfit_colors(outfit_id);

CREATE TABLE IF NOT EXISTS tb_item (
    id           UUID         PRIMARY KEY,
    outfit_id    UUID         NOT NULL REFERENCES tb_outfit(id),
    item_type    VARCHAR(50)  NOT NULL,
    item_name    VARCHAR(32)  NOT NULL,
    item_img     VARCHAR(255) NOT NULL,
    item_url     VARCHAR(255) NOT NULL,
    image_status VARCHAR(50),
    created_at   TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_item_outfit_id ON tb_item(outfit_id);

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
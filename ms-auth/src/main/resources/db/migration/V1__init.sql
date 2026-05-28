CREATE TABLE IF NOT EXISTS tb_user (
    id        UUID         PRIMARY KEY,
    username  VARCHAR(255) NOT NULL UNIQUE,
    password  VARCHAR(255),
    role      VARCHAR(50)  NOT NULL,
    google_id VARCHAR(255) UNIQUE
);
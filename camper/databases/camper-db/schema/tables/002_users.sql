CREATE TABLE IF NOT EXISTS users (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email      VARCHAR(255) NOT NULL,
    username   VARCHAR(100),
    created_at TIMESTAMPTZ  NOT NULL    DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL    DEFAULT now(),

    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);

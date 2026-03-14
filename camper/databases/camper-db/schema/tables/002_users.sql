CREATE TABLE IF NOT EXISTS users (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email             VARCHAR(255) NOT NULL,
    username          VARCHAR(100),
    experience_level  VARCHAR(20),
    avatar_seed       VARCHAR(64),
    profile_completed BOOLEAN      NOT NULL    DEFAULT false,
    created_at        TIMESTAMPTZ  NOT NULL    DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL    DEFAULT now(),

    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_experience_level CHECK (experience_level IS NULL OR experience_level IN ('beginner', 'intermediate', 'advanced', 'expert'))
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);

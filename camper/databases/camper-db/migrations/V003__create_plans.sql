CREATE TABLE IF NOT EXISTS plans (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(255) NOT NULL,
    visibility VARCHAR(10)  NOT NULL    DEFAULT 'private',
    owner_id   UUID         NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL    DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL    DEFAULT now(),

    CONSTRAINT ck_plans_visibility CHECK (visibility IN ('public', 'private')),
    CONSTRAINT fk_plans_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);

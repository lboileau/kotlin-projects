CREATE TABLE IF NOT EXISTS worlds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    greeting VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_worlds_name UNIQUE (name)
);

CREATE INDEX IF NOT EXISTS idx_worlds_name ON worlds (name);

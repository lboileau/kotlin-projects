CREATE TABLE IF NOT EXISTS ladder_participants (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    ladder_id  UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_ladder_participants_ladder_user UNIQUE (ladder_id, user_id),
    CONSTRAINT fk_ladder_participants_ladder FOREIGN KEY (ladder_id) REFERENCES activity_ladders (id) ON DELETE CASCADE,
    CONSTRAINT fk_ladder_participants_user   FOREIGN KEY (user_id)   REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_ladder_participants_ladder_id ON ladder_participants (ladder_id);
CREATE INDEX IF NOT EXISTS idx_ladder_participants_user_id   ON ladder_participants (user_id);

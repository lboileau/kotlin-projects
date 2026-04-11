CREATE TABLE IF NOT EXISTS ladder_votes (
    id                    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    ladder_id             UUID        NOT NULL,
    round_number          INT         NOT NULL,
    user_id               UUID        NOT NULL,
    voted_for_activity_id UUID        NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_ladder_votes_round_user UNIQUE (ladder_id, round_number, user_id),
    CONSTRAINT fk_ladder_votes_ladder   FOREIGN KEY (ladder_id)             REFERENCES activity_ladders (id)  ON DELETE CASCADE,
    CONSTRAINT fk_ladder_votes_user     FOREIGN KEY (user_id)               REFERENCES users (id)             ON DELETE CASCADE,
    CONSTRAINT fk_ladder_votes_activity FOREIGN KEY (voted_for_activity_id) REFERENCES ladder_activities (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_ladder_votes_ladder_round ON ladder_votes (ladder_id, round_number);

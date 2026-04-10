CREATE TABLE IF NOT EXISTS activity_ladders (
    id                            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_id                    UUID         NOT NULL,
    title                         VARCHAR(200) NOT NULL,
    status                        VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',
    current_round_number          INT,
    current_match_activity_a_id   UUID,
    current_match_activity_b_id   UUID,
    is_final_round                BOOLEAN      NOT NULL DEFAULT false,
    is_grand_final_reset          BOOLEAN      NOT NULL DEFAULT false,
    winner_activity_id            UUID,
    created_at                    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_activity_ladders_status CHECK (status IN ('DRAFT', 'ACTIVE', 'COMPLETED')),
    CONSTRAINT fk_activity_ladders_creator FOREIGN KEY (creator_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_activity_ladders_match_a FOREIGN KEY (current_match_activity_a_id) REFERENCES ladder_activities (id) ON DELETE SET NULL,
    CONSTRAINT fk_activity_ladders_match_b FOREIGN KEY (current_match_activity_b_id) REFERENCES ladder_activities (id) ON DELETE SET NULL,
    CONSTRAINT fk_activity_ladders_winner  FOREIGN KEY (winner_activity_id) REFERENCES ladder_activities (id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS idx_activity_ladders_creator_id ON activity_ladders (creator_id);
CREATE INDEX IF NOT EXISTS idx_activity_ladders_status ON activity_ladders (status);

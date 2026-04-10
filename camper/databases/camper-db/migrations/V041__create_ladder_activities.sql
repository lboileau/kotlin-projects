CREATE TABLE IF NOT EXISTS ladder_activities (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    ladder_id        UUID          NOT NULL,
    name             VARCHAR(200)  NOT NULL,
    image_url        VARCHAR(2000) NOT NULL,
    distance_minutes INT           NOT NULL,
    cost_per_person  DECIMAL(10,2) NOT NULL,
    losses           INT           NOT NULL DEFAULT 0,
    bracket          VARCHAR(16)   NOT NULL DEFAULT 'WINNERS',
    display_order    INT           NOT NULL,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT ck_ladder_activities_distance CHECK (distance_minutes >= 0),
    CONSTRAINT ck_ladder_activities_cost     CHECK (cost_per_person >= 0),
    CONSTRAINT ck_ladder_activities_losses   CHECK (losses >= 0 AND losses <= 2),
    CONSTRAINT ck_ladder_activities_bracket  CHECK (bracket IN ('WINNERS', 'LOSERS', 'ELIMINATED')),
    CONSTRAINT fk_ladder_activities_ladder   FOREIGN KEY (ladder_id) REFERENCES activity_ladders (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_ladder_activities_ladder_id      ON ladder_activities (ladder_id);
CREATE INDEX IF NOT EXISTS idx_ladder_activities_ladder_bracket ON ladder_activities (ladder_id, bracket);

-- Back-fill self-referential FKs on activity_ladders that point at ladder_activities.
-- These could not be declared in V040 because ladder_activities did not exist yet.
-- Each block is idempotent: duplicate_object (SQLSTATE 42710) is silently ignored.

DO $$
BEGIN
    ALTER TABLE activity_ladders
        ADD CONSTRAINT fk_activity_ladders_match_a
            FOREIGN KEY (current_match_activity_a_id) REFERENCES ladder_activities (id) ON DELETE SET NULL;
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$
BEGIN
    ALTER TABLE activity_ladders
        ADD CONSTRAINT fk_activity_ladders_match_b
            FOREIGN KEY (current_match_activity_b_id) REFERENCES ladder_activities (id) ON DELETE SET NULL;
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$
BEGIN
    ALTER TABLE activity_ladders
        ADD CONSTRAINT fk_activity_ladders_winner
            FOREIGN KEY (winner_activity_id) REFERENCES ladder_activities (id) ON DELETE SET NULL;
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

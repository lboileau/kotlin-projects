CREATE TABLE IF NOT EXISTS plan_members (
    plan_id    UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    role       VARCHAR(20) NOT NULL DEFAULT 'member',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    PRIMARY KEY (plan_id, user_id),
    CONSTRAINT fk_plan_members_plan FOREIGN KEY (plan_id) REFERENCES plans (id) ON DELETE CASCADE,
    CONSTRAINT fk_plan_members_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_plan_members_role CHECK (role IN ('member', 'manager'))
);

CREATE INDEX IF NOT EXISTS idx_plan_members_user_id ON plan_members (user_id);

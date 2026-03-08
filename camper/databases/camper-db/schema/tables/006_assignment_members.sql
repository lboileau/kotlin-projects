CREATE TABLE IF NOT EXISTS assignment_members (
    assignment_id   UUID        NOT NULL,
    user_id         UUID        NOT NULL,
    plan_id         UUID        NOT NULL,
    assignment_type VARCHAR(10) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    PRIMARY KEY (assignment_id, user_id),
    CONSTRAINT ck_assignment_members_type CHECK (assignment_type IN ('tent', 'canoe')),
    CONSTRAINT uq_assignment_members_plan_id_user_id_type UNIQUE (plan_id, user_id, assignment_type),
    CONSTRAINT fk_assignment_members_assignment FOREIGN KEY (assignment_id) REFERENCES assignments (id) ON DELETE CASCADE,
    CONSTRAINT fk_assignment_members_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_assignment_members_plan FOREIGN KEY (plan_id) REFERENCES plans (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_assignment_members_user_id ON assignment_members (user_id);
CREATE INDEX IF NOT EXISTS idx_assignment_members_plan_id ON assignment_members (plan_id);
CREATE INDEX IF NOT EXISTS idx_assignment_members_assignment_id ON assignment_members (assignment_id);

CREATE TABLE IF NOT EXISTS assignments (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id       UUID         NOT NULL,
    name          VARCHAR(255) NOT NULL,
    type          VARCHAR(10)  NOT NULL,
    max_occupancy INT          NOT NULL,
    owner_id      UUID         NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL    DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL    DEFAULT now(),

    CONSTRAINT ck_assignments_type CHECK (type IN ('tent', 'canoe')),
    CONSTRAINT ck_assignments_max_occupancy CHECK (max_occupancy > 0),
    CONSTRAINT uq_assignments_plan_id_name_type UNIQUE (plan_id, name, type),
    CONSTRAINT fk_assignments_plan FOREIGN KEY (plan_id) REFERENCES plans (id) ON DELETE CASCADE,
    CONSTRAINT fk_assignments_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_assignments_plan_id ON assignments (plan_id);
CREATE INDEX IF NOT EXISTS idx_assignments_owner_id ON assignments (owner_id);

-- Transfer assignment ownership to plan owner when a user is deleted
CREATE OR REPLACE FUNCTION transfer_assignment_ownership_on_user_delete()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE assignments
    SET owner_id = (SELECT owner_id FROM plans WHERE plans.id = assignments.plan_id)
    WHERE assignments.owner_id = OLD.id;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_transfer_assignment_ownership
    BEFORE DELETE ON users
    FOR EACH ROW
    EXECUTE FUNCTION transfer_assignment_ownership_on_user_delete();

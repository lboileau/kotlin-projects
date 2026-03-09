-- Allow items to belong to both a plan AND a user (personal gear per plan).
-- Previously: exactly one of plan_id or user_id must be set.
-- Now: at least one must be set. Both can be set for personal gear scoped to a plan.

ALTER TABLE items DROP CONSTRAINT chk_items_single_owner;

ALTER TABLE items ADD CONSTRAINT chk_items_owner CHECK (
    plan_id IS NOT NULL OR user_id IS NOT NULL
);

-- Add composite index for the new query pattern (personal gear per plan)
CREATE INDEX idx_items_plan_id_user_id ON items (plan_id, user_id) WHERE user_id IS NOT NULL;

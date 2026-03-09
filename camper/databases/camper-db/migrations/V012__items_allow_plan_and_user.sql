-- Allow items to belong to both a plan AND a user (personal gear per plan).
-- Previously: exactly one of plan_id or user_id must be set.
-- Now: plan_id is always required. user_id is optional (set for personal gear).
-- Shared gear: plan_id only. Personal gear: plan_id + user_id.

ALTER TABLE items DROP CONSTRAINT chk_items_single_owner;

ALTER TABLE items ADD CONSTRAINT chk_items_owner CHECK (
    plan_id IS NOT NULL
);

-- Migrate existing user-only items (plan_id IS NULL) by deleting them.
-- These are orphaned personal items from before plan-scoping was enforced.
DELETE FROM items WHERE plan_id IS NULL;

-- Add composite index for the new query pattern (personal gear per plan)
CREATE INDEX idx_items_plan_id_user_id ON items (plan_id, user_id) WHERE user_id IS NOT NULL;

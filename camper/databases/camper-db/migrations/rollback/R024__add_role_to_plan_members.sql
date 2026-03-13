ALTER TABLE plan_members DROP CONSTRAINT IF EXISTS ck_plan_members_role;
ALTER TABLE plan_members DROP COLUMN IF EXISTS role;

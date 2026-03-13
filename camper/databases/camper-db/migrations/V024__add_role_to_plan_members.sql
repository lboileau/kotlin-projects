ALTER TABLE plan_members ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'member';
ALTER TABLE plan_members ADD CONSTRAINT ck_plan_members_role CHECK (role IN ('member', 'manager'));

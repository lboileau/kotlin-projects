ALTER TABLE users ADD COLUMN IF NOT EXISTS external_id UUID NOT NULL DEFAULT gen_random_uuid();

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_external_id ON users (external_id);

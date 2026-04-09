DROP INDEX IF EXISTS idx_users_external_id;
ALTER TABLE users DROP COLUMN IF EXISTS external_id;

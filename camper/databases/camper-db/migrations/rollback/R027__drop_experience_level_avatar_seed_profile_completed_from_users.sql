ALTER TABLE users DROP CONSTRAINT IF EXISTS ck_users_experience_level;
ALTER TABLE users DROP COLUMN IF EXISTS profile_completed;
ALTER TABLE users DROP COLUMN IF EXISTS avatar_seed;
ALTER TABLE users DROP COLUMN IF EXISTS experience_level;

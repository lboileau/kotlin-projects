ALTER TABLE users ADD COLUMN IF NOT EXISTS experience_level VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_seed VARCHAR(64);
ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_completed BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE users ADD CONSTRAINT ck_users_experience_level
    CHECK (experience_level IS NULL OR experience_level IN ('beginner', 'intermediate', 'advanced', 'expert'));

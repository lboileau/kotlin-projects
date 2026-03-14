-- Rollback: clear backfilled avatar seeds (original state was NULL)
UPDATE users
SET avatar_seed = NULL,
    updated_at  = now()
WHERE avatar_seed IS NOT NULL;

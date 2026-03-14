-- Enable pgcrypto for SHA-256 digest
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Backfill avatar_seed for users who don't have one yet.
-- Computes SHA-256 hex of the lowercase trimmed username (or email if no username).
UPDATE users
SET avatar_seed = encode(digest(lower(trim(coalesce(username, email))), 'sha256'), 'hex'),
    updated_at  = now()
WHERE avatar_seed IS NULL;

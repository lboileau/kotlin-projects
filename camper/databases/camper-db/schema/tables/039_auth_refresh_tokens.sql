CREATE TABLE IF NOT EXISTS auth_refresh_tokens (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    family_id  UUID         NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_auth_refresh_tokens_user_id
    ON auth_refresh_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_auth_refresh_tokens_token_hash
    ON auth_refresh_tokens (token_hash);
CREATE INDEX IF NOT EXISTS idx_auth_refresh_tokens_family_id
    ON auth_refresh_tokens (family_id);

CREATE TABLE IF NOT EXISTS auth_otp_codes (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL,
    code_hash     VARCHAR(64)  NOT NULL,
    expires_at    TIMESTAMPTZ  NOT NULL,
    used_at       TIMESTAMPTZ,
    attempt_count INTEGER      NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_auth_otp_codes_email_expires
    ON auth_otp_codes (email, expires_at);

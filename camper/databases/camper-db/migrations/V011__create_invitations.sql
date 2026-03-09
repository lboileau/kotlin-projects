CREATE TABLE IF NOT EXISTS invitations (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id         UUID        NOT NULL,
    user_id         UUID        NOT NULL,
    email           VARCHAR(255) NOT NULL,
    inviter_id      UUID        NOT NULL,
    resend_email_id VARCHAR(255),
    status          VARCHAR(20) NOT NULL DEFAULT 'pending',
    sent_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_invitations_plan_id_user_id UNIQUE (plan_id, user_id),
    CONSTRAINT ck_invitations_status CHECK (status IN ('pending', 'sent', 'delivered', 'bounced', 'failed', 'complained', 'delayed')),
    CONSTRAINT fk_invitations_plan FOREIGN KEY (plan_id) REFERENCES plans (id) ON DELETE CASCADE,
    CONSTRAINT fk_invitations_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_invitations_inviter FOREIGN KEY (inviter_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_invitations_plan_id ON invitations (plan_id);
CREATE INDEX IF NOT EXISTS idx_invitations_user_id ON invitations (user_id);
CREATE INDEX IF NOT EXISTS idx_invitations_resend_email_id ON invitations (resend_email_id);

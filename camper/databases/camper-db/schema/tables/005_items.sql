CREATE TABLE IF NOT EXISTS items (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id    UUID,
    user_id    UUID,
    name       VARCHAR(255) NOT NULL,
    category   VARCHAR(50)  NOT NULL,
    quantity   INTEGER      NOT NULL DEFAULT 1,
    packed     BOOLEAN      NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_items_owner CHECK (plan_id IS NOT NULL),
    CONSTRAINT fk_items_plan FOREIGN KEY (plan_id) REFERENCES plans (id) ON DELETE CASCADE,
    CONSTRAINT fk_items_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_items_plan_id ON items (plan_id);
CREATE INDEX IF NOT EXISTS idx_items_user_id ON items (user_id);
CREATE INDEX IF NOT EXISTS idx_items_plan_id_user_id ON items (plan_id, user_id) WHERE user_id IS NOT NULL;

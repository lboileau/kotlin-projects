CREATE TABLE IF NOT EXISTS recipe_favorites (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id  UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_recipe_favorites_recipe_user UNIQUE (recipe_id, user_id),
    CONSTRAINT fk_recipe_favorites_recipe FOREIGN KEY (recipe_id) REFERENCES recipes (id) ON DELETE CASCADE,
    CONSTRAINT fk_recipe_favorites_user   FOREIGN KEY (user_id)   REFERENCES users (id)   ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_recipe_favorites_user_id ON recipe_favorites (user_id);

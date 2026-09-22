-- A recipe's instructions as an ordered list of steps. Steps are plain text, never reviewed,
-- and always written as a whole list (replace-all), so the row id exists only as a key.
CREATE TABLE IF NOT EXISTS recipe_steps (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id  UUID        NOT NULL,
    position   INT         NOT NULL,
    text       TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_recipe_steps_position CHECK (position >= 0),
    CONSTRAINT ck_recipe_steps_text CHECK (length(btrim(text)) > 0),
    CONSTRAINT uq_recipe_steps_recipe_position UNIQUE (recipe_id, position),
    CONSTRAINT fk_recipe_steps_recipe FOREIGN KEY (recipe_id) REFERENCES recipes (id) ON DELETE CASCADE
);

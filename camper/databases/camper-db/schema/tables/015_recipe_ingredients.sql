CREATE TABLE IF NOT EXISTS recipe_ingredients (
    id                       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id                UUID         NOT NULL,
    ingredient_id            UUID,
    original_text            TEXT,
    quantity                 NUMERIC      NOT NULL,
    unit                     VARCHAR(20)  NOT NULL,
    status                   VARCHAR(20)  NOT NULL DEFAULT 'approved',
    matched_ingredient_id    UUID,
    suggested_ingredient_name TEXT,
    suggested_category       VARCHAR(50),
    suggested_unit           VARCHAR(20),
    review_flags             JSONB        NOT NULL DEFAULT '[]',
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_recipe_ingredients_quantity CHECK (quantity > 0),
    CONSTRAINT ck_recipe_ingredients_status CHECK (status IN ('pending_review', 'approved')),
    CONSTRAINT fk_recipe_ingredients_recipe FOREIGN KEY (recipe_id) REFERENCES recipes (id) ON DELETE CASCADE,
    CONSTRAINT fk_recipe_ingredients_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredients (id) ON DELETE RESTRICT,
    CONSTRAINT fk_recipe_ingredients_matched_ingredient FOREIGN KEY (matched_ingredient_id) REFERENCES ingredients (id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_recipe_ingredients_recipe_id_ingredient_id ON recipe_ingredients (recipe_id, ingredient_id) WHERE ingredient_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_recipe_ingredients_recipe_id ON recipe_ingredients (recipe_id);
CREATE INDEX IF NOT EXISTS idx_recipe_ingredients_ingredient_id ON recipe_ingredients (ingredient_id);
CREATE INDEX IF NOT EXISTS idx_recipe_ingredients_matched_ingredient_id ON recipe_ingredients (matched_ingredient_id);

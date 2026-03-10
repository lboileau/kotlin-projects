CREATE TABLE IF NOT EXISTS ingredients (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(255) NOT NULL,
    category     VARCHAR(50)  NOT NULL,
    default_unit VARCHAR(20)  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_ingredients_name UNIQUE (name),
    CONSTRAINT ck_ingredients_category CHECK (category IN ('produce', 'dairy', 'meat', 'seafood', 'pantry', 'spice', 'condiment', 'frozen', 'bakery', 'other')),
    CONSTRAINT ck_ingredients_default_unit CHECK (default_unit IN ('g', 'kg', 'ml', 'l', 'tsp', 'tbsp', 'cup', 'oz', 'lb', 'pieces', 'whole', 'bunch', 'can', 'clove', 'pinch', 'slice', 'sprig'))
);

CREATE INDEX IF NOT EXISTS idx_ingredients_name ON ingredients (name);

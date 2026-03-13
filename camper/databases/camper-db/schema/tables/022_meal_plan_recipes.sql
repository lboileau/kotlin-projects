CREATE TABLE IF NOT EXISTS meal_plan_recipes (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    meal_plan_day_id UUID        NOT NULL,
    meal_type        VARCHAR(20) NOT NULL,
    recipe_id        UUID        NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_meal_plan_recipes_meal_type CHECK (meal_type IN ('breakfast', 'lunch', 'dinner', 'snack')),
    CONSTRAINT fk_meal_plan_recipes_meal_plan_day FOREIGN KEY (meal_plan_day_id) REFERENCES meal_plan_days (id) ON DELETE CASCADE,
    CONSTRAINT fk_meal_plan_recipes_recipe FOREIGN KEY (recipe_id) REFERENCES recipes (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_meal_plan_recipes_meal_plan_day_id ON meal_plan_recipes (meal_plan_day_id);
CREATE INDEX IF NOT EXISTS idx_meal_plan_recipes_recipe_id ON meal_plan_recipes (recipe_id);

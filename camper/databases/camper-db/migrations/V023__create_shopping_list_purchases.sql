CREATE TABLE IF NOT EXISTS shopping_list_purchases (
    id                 UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    meal_plan_id       UUID        NOT NULL,
    ingredient_id      UUID        NOT NULL,
    unit               VARCHAR(20) NOT NULL,
    quantity_purchased NUMERIC     NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_shopping_list_purchases_quantity CHECK (quantity_purchased >= 0),
    CONSTRAINT uq_shopping_list_purchases_meal_plan_ingredient_unit UNIQUE (meal_plan_id, ingredient_id, unit),
    CONSTRAINT fk_shopping_list_purchases_meal_plan FOREIGN KEY (meal_plan_id) REFERENCES meal_plans (id) ON DELETE CASCADE,
    CONSTRAINT fk_shopping_list_purchases_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredients (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_shopping_list_purchases_meal_plan_id ON shopping_list_purchases (meal_plan_id);
CREATE INDEX IF NOT EXISTS idx_shopping_list_purchases_ingredient_id ON shopping_list_purchases (ingredient_id);

CREATE TABLE IF NOT EXISTS shopping_list_manual_items (
    id                 UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    meal_plan_id       UUID        NOT NULL,
    ingredient_id      UUID,
    description        TEXT,
    quantity           NUMERIC     NOT NULL DEFAULT 1,
    unit               VARCHAR(20),
    quantity_purchased NUMERIC     NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_manual_items_has_ingredient_or_description
        CHECK (ingredient_id IS NOT NULL OR description IS NOT NULL),
    CONSTRAINT ck_manual_items_quantity_positive
        CHECK (quantity > 0),
    CONSTRAINT ck_manual_items_purchased_non_negative
        CHECK (quantity_purchased >= 0),
    CONSTRAINT fk_manual_items_meal_plan
        FOREIGN KEY (meal_plan_id) REFERENCES meal_plans (id) ON DELETE CASCADE,
    CONSTRAINT fk_manual_items_ingredient
        FOREIGN KEY (ingredient_id) REFERENCES ingredients (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_manual_items_meal_plan_ingredient_unit
    ON shopping_list_manual_items (meal_plan_id, ingredient_id, unit)
    WHERE ingredient_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_manual_items_meal_plan_id
    ON shopping_list_manual_items (meal_plan_id);

-- Change ingredient_id FK from RESTRICT to SET NULL so deleting an ingredient
-- nulls out the reference on recipe_ingredients instead of blocking.
ALTER TABLE recipe_ingredients
    DROP CONSTRAINT fk_recipe_ingredients_ingredient,
    ADD CONSTRAINT fk_recipe_ingredients_ingredient
        FOREIGN KEY (ingredient_id) REFERENCES ingredients (id) ON DELETE SET NULL;

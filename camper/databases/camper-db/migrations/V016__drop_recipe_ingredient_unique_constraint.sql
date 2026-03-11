-- Drop the unique constraint on (recipe_id, ingredient_id) to allow
-- multiple recipe_ingredient rows mapping to the same ingredient
-- (e.g., "1 tsp salt" and "salt to taste" as separate lines).
-- Users consolidate during the review step.
DROP INDEX IF EXISTS uq_recipe_ingredients_recipe_id_ingredient_id;

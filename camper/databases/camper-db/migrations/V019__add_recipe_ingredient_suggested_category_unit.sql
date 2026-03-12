ALTER TABLE recipe_ingredients ADD COLUMN IF NOT EXISTS suggested_category VARCHAR(50);
ALTER TABLE recipe_ingredients ADD COLUMN IF NOT EXISTS suggested_unit VARCHAR(20);

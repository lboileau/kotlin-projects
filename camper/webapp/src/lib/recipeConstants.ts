/** Recipe-related constants shared between RecipesPage and RecipeForm. */

export const CATEGORIES = [
  'produce', 'dairy', 'meat', 'seafood', 'pantry',
  'spice', 'condiment', 'frozen', 'bakery', 'other',
] as const;

export const MEALS = [
  'breakfast', 'lunch', 'dinner', 'snack',
  'dessert', 'appetizer', 'side', 'drink',
] as const;

export const THEMES = [
  'chicken', 'beef', 'pork', 'fish', 'seafood',
  'vegetarian', 'vegan', 'pasta', 'soup', 'salad', 'other',
] as const;

export interface DraftIngredient {
  ingredientId: string;
  ingredientName: string;
  quantity: number;
  unit: string;
}

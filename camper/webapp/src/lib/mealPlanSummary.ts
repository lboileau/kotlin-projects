import type { MealPlanDetailResponse } from '../api/client';

export function buildMealPlanSummary(mealPlan: MealPlanDetailResponse): string {
  const seen = new Set<string>();
  const entries: string[] = [];

  for (const day of mealPlan.days) {
    for (const mealType of ['breakfast', 'lunch', 'dinner', 'snack'] as const) {
      for (const recipe of day.meals[mealType]) {
        if (seen.has(recipe.recipeId)) continue;
        seen.add(recipe.recipeId);
        const entry = recipe.recipeWebLink
          ? `${recipe.recipeName}\n${recipe.recipeWebLink}`
          : recipe.recipeName;
        entries.push(entry);
      }
    }
  }

  return entries.join('\n\n');
}

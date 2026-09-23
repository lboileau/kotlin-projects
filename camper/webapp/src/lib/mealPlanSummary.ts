import type { MealPlanDetailResponse } from '../api/types';

/**
 * The plan as text for the clipboard: each recipe once, its name and a link
 * under it — the recipe's source page when it has one, otherwise the
 * recipe's page in this app (`appOrigin` + `/recipes/{id}`), so whoever
 * receives the summary can always open every recipe.
 */
export function buildMealPlanSummary(mealPlan: MealPlanDetailResponse, appOrigin: string): string {
  const seen = new Set<string>();
  const entries: string[] = [];

  for (const day of mealPlan.days) {
    for (const mealType of ['breakfast', 'lunch', 'dinner', 'snack'] as const) {
      for (const recipe of day.meals[mealType]) {
        if (seen.has(recipe.recipeId)) continue;
        seen.add(recipe.recipeId);
        const link = recipe.recipeWebLink ?? `${appOrigin}/recipes/${recipe.recipeId}`;
        entries.push(`${recipe.recipeName}\n${link}`);
      }
    }
  }

  return entries.join('\n\n');
}

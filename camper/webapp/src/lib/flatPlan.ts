import type { MealPlanDetailResponse, MealPlanRecipeDetailResponse } from '../api/mealPlans';

export const MEAL_TYPES = ['breakfast', 'lunch', 'dinner', 'snack'] as const;

export interface FlatPlanRecipe {
  recipeId: string;
  recipeName: string;
  recipeWebLink: string | null;
  baseServings: number;
  /**
   * Every mealPlanRecipeId this recipe occupies across all days/meal
   * slots. Older multi-day plans can have more than one — removing the
   * recipe means deleting every id here.
   */
  mealPlanRecipeIds: string[];
}

/**
 * Flattens the backend's day x meal-slot structure into the one flat
 * recipe list the UI shows, de-duplicated by recipeId. New plans only
 * ever get additions under day 1 / dinner, but existing multi-day plans
 * (and duplicates within one day) are handled the same way.
 */
export function flattenMealPlan(detail: MealPlanDetailResponse): FlatPlanRecipe[] {
  const byRecipeId = new Map<string, FlatPlanRecipe>();

  for (const day of detail.days) {
    for (const mealType of MEAL_TYPES) {
      for (const entry of day.meals[mealType]) {
        const existing = byRecipeId.get(entry.recipeId);
        if (existing) {
          existing.mealPlanRecipeIds.push(entry.id);
        } else {
          byRecipeId.set(entry.recipeId, {
            recipeId: entry.recipeId,
            recipeName: entry.recipeName,
            recipeWebLink: entry.recipeWebLink,
            baseServings: entry.baseServings,
            mealPlanRecipeIds: [entry.id],
          });
        }
      }
    }
  }

  return [...byRecipeId.values()];
}

/** The set of recipe ids already in the plan, for "already added" checks. */
export function recipeIdsInPlan(detail: MealPlanDetailResponse): Set<string> {
  return new Set(flattenMealPlan(detail).map((recipe) => recipe.recipeId));
}

/** The first occurrence of a recipe already in the plan, or null. Used to no-op a duplicate add. */
export function findRecipeInPlan(
  detail: MealPlanDetailResponse,
  recipeId: string,
): MealPlanRecipeDetailResponse | null {
  for (const day of detail.days) {
    for (const mealType of MEAL_TYPES) {
      const found = day.meals[mealType].find((entry) => entry.recipeId === recipeId);
      if (found) return found;
    }
  }
  return null;
}

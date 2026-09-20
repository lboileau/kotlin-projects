import { request, requestWithStatus, type ResponseWithStatus } from './http';

export interface MealPlanResponse {
  id: string;
  planId: string | null;
  name: string;
  servings: number;
  scalingMode: string;
  isTemplate: boolean;
  sourceTemplateId: string | null;
  createdBy: string;
  /** Distinct recipes in the plan, across every day/meal slot. */
  recipeCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface MealPlanDetailResponse {
  id: string;
  planId: string | null;
  name: string;
  servings: number;
  scalingMode: string;
  isTemplate: boolean;
  sourceTemplateId: string | null;
  createdBy: string;
  days: MealPlanDayResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface MealPlanDayResponse {
  id: string;
  dayNumber: number;
  meals: MealsByTypeResponse;
}

export interface MealsByTypeResponse {
  breakfast: MealPlanRecipeDetailResponse[];
  lunch: MealPlanRecipeDetailResponse[];
  dinner: MealPlanRecipeDetailResponse[];
  snack: MealPlanRecipeDetailResponse[];
}

export type MealType = keyof MealsByTypeResponse;

export interface MealPlanRecipeDetailResponse {
  id: string;
  recipeId: string;
  recipeName: string;
  recipeWebLink: string | null;
  baseServings: number;
  scaleFactor: number;
  isFullyPurchased: boolean;
  ingredients: MealPlanIngredientResponse[];
}

export interface MealPlanIngredientResponse {
  recipeIngredientId: string;
  ingredientId: string;
  ingredientName: string;
  category: string;
  quantity: number;
  scaledQuantity: number;
  unit: string;
}

/** GET /api/meal-plans?createdBy={userId} — this user's plans (templates included, filter client-side). */
export function listMyMealPlans(userId: string): Promise<MealPlanResponse[]> {
  return request(`/api/meal-plans?createdBy=${encodeURIComponent(userId)}`);
}

/** GET /api/meal-plans/{id} */
export function getMealPlanDetail(mealPlanId: string): Promise<MealPlanDetailResponse> {
  return request(`/api/meal-plans/${mealPlanId}`);
}

/** POST /api/meal-plans — always a standalone, non-template plan (planId null, isTemplate false). */
export function createMealPlan(input: { name: string; servings: number }): Promise<MealPlanResponse> {
  return request('/api/meal-plans', {
    method: 'POST',
    body: { name: input.name, servings: input.servings, isTemplate: false, planId: null },
  });
}

/** PUT /api/meal-plans/{id} */
export function updateMealPlan(
  mealPlanId: string,
  input: { name?: string; servings?: number },
): Promise<MealPlanResponse> {
  return request(`/api/meal-plans/${mealPlanId}`, { method: 'PUT', body: input });
}

/** DELETE /api/meal-plans/{id} */
export function deleteMealPlan(mealPlanId: string): Promise<void> {
  return request(`/api/meal-plans/${mealPlanId}`, { method: 'DELETE' });
}

/**
 * POST /api/meal-plans/{id}/recipes — server picks the plan's
 * lowest-numbered day (creating day 1 first if the plan has none), meal
 * type `dinner`, concurrency-safe. Status carries meaning here: `201`
 * with a new entry, or `200` with the EXISTING entry when the recipe is
 * already anywhere in the plan — callers use the status, not the body
 * shape, to tell those apart (`requestWithStatus`, not `request`).
 */
export function addRecipeToPlan(
  mealPlanId: string,
  recipeId: string,
): Promise<ResponseWithStatus<MealPlanRecipeDetailResponse>> {
  return requestWithStatus(`/api/meal-plans/${mealPlanId}/recipes`, { method: 'POST', body: { recipeId } });
}

/** DELETE /api/meal-plans/{id}/recipes/{recipeId} — removes every occurrence of the recipe; idempotent. */
export function removeRecipeFromPlan(mealPlanId: string, recipeId: string): Promise<void> {
  return request(`/api/meal-plans/${mealPlanId}/recipes/${recipeId}`, { method: 'DELETE' });
}

/** POST /api/meal-plans/{id}/duplicate — atomic server-side copy (days + recipes; not purchases/manual items). Omit `name` for the server's default ("<source> copy"). */
export function duplicateMealPlan(mealPlanId: string, name?: string): Promise<MealPlanResponse> {
  return request(`/api/meal-plans/${mealPlanId}/duplicate`, {
    method: 'POST',
    body: name !== undefined ? { name } : {},
  });
}

import { request } from './http';

export interface MealPlanResponse {
  id: string;
  planId: string | null;
  name: string;
  servings: number;
  scalingMode: string;
  isTemplate: boolean;
  sourceTemplateId: string | null;
  createdBy: string;
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

/** POST /api/meal-plans/{id}/days */
export function addMealPlanDay(mealPlanId: string, dayNumber: number): Promise<MealPlanDayResponse> {
  return request(`/api/meal-plans/${mealPlanId}/days`, { method: 'POST', body: { dayNumber } });
}

/** POST /api/meal-plans/{id}/days/{dayId}/recipes */
export function addRecipeToMeal(
  mealPlanId: string,
  dayId: string,
  input: { mealType: MealType; recipeId: string },
): Promise<MealPlanRecipeDetailResponse> {
  return request(`/api/meal-plans/${mealPlanId}/days/${dayId}/recipes`, { method: 'POST', body: input });
}

/** DELETE /api/meal-plan-recipes/{mealPlanRecipeId} — note: not nested under /meal-plans. */
export function removeRecipeFromMeal(mealPlanRecipeId: string): Promise<void> {
  return request(`/api/meal-plan-recipes/${mealPlanRecipeId}`, { method: 'DELETE' });
}

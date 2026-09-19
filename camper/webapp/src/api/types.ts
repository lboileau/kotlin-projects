// Shared cross-domain response types. Meal-plan shapes live here for now
// because `lib/mealPlanSummary.ts` needs them; they should move into
// `api/mealPlans.ts` once that domain is built out (plan build order,
// step 3/4) alongside the functions that fetch them.

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

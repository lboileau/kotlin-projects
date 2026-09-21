// Meal-plan response types now live in `api/mealPlans.ts`, alongside the
// fetch functions that produce them. Re-exported here so
// `lib/mealPlanSummary.ts` (and its test) don't need to change their
// import path.
export type {
  MealPlanResponse,
  MealPlanDetailResponse,
  MealPlanDayResponse,
  MealsByTypeResponse,
  MealPlanRecipeDetailResponse,
  MealPlanIngredientResponse,
} from './mealPlans';

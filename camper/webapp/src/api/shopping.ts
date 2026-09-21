import { request } from './http';

export type ShoppingItemStatus = 'done' | 'more_needed' | 'not_purchased' | 'no_longer_needed';
export type ShoppingItemSource = 'recipe' | 'manual';

export interface ShoppingRecipeRef {
  id: string;
  name: string;
}

export interface ShoppingListItemResponse {
  ingredientId: string | null;
  ingredientName: string | null;
  description: string | null;
  quantityRequired: number;
  quantityPurchased: number;
  unit: string | null;
  status: ShoppingItemStatus;
  usedInRecipes: string[];
  usedInRecipeRefs: ShoppingRecipeRef[];
  source: ShoppingItemSource;
  manualItemId: string | null;
}

export interface ShoppingListCategoryResponse {
  category: string;
  items: ShoppingListItemResponse[];
}

export interface ShoppingListResponse {
  mealPlanId: string;
  mealPlanName: string;
  servings: number;
  scalingMode: string;
  totalItems: number;
  fullyPurchasedCount: number;
  categories: ShoppingListCategoryResponse[];
}

/** The shape POST …/shopping-list/items returns — distinct from ShoppingListItemResponse (quantity, not quantityRequired; a top-level category). */
export interface ManualShoppingItemResponse {
  id: string;
  ingredientId: null;
  ingredientName: null;
  description: string;
  quantity: number;
  unit: null;
  quantityPurchased: number;
  status: ShoppingItemStatus;
  category: string;
}

/** GET /api/meal-plans/{id}/shopping-list */
export function getShoppingList(mealPlanId: string): Promise<ShoppingListResponse> {
  return request(`/api/meal-plans/${mealPlanId}/shopping-list`);
}

export interface UpdateRecipePurchaseInput {
  ingredientId: string;
  unit: string;
  quantityPurchased: number;
}

export interface UpdateManualPurchaseInput {
  manualItemId: string;
  quantityPurchased: number;
}

/** PATCH /api/meal-plans/{id}/shopping-list — one call per (ingredientId,unit) or manualItemId entry. */
export function updatePurchase(
  mealPlanId: string,
  input: UpdateRecipePurchaseInput | UpdateManualPurchaseInput,
): Promise<void> {
  return request(`/api/meal-plans/${mealPlanId}/shopping-list`, { method: 'PATCH', body: input });
}

/** POST /api/meal-plans/{id}/shopping-list/items — description only; quantity/unit would 400. */
export function addManualItem(mealPlanId: string, description: string): Promise<ManualShoppingItemResponse> {
  return request(`/api/meal-plans/${mealPlanId}/shopping-list/items`, { method: 'POST', body: { description } });
}

/** DELETE /api/meal-plans/{id}/shopping-list/items/{itemId} */
export function removeManualItem(mealPlanId: string, itemId: string): Promise<void> {
  return request(`/api/meal-plans/${mealPlanId}/shopping-list/items/${itemId}`, { method: 'DELETE' });
}

/** DELETE /api/meal-plans/{id}/shopping-list — resets every purchase. */
export function resetPurchases(mealPlanId: string): Promise<void> {
  return request(`/api/meal-plans/${mealPlanId}/shopping-list`, { method: 'DELETE' });
}

import { request, requestWithStatus, type ResponseWithStatus } from './http';

export type PlanRole = 'owner' | 'member';

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
  /** The caller's role on this plan — never derived client-side from id comparisons. */
  role: PlanRole;
  /** People with access, not counting the owner. */
  memberCount: number;
  /** The owner's username, or email when they have none. */
  ownerName: string;
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
  role: PlanRole;
  memberCount: number;
  ownerName: string;
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

/** GET /api/meal-plans/mine — plans the caller owns plus plans shared with them, newest updated first (templates included, filter client-side). Identity comes from the X-User-Id header, so no id is passed here. */
export function listMyMealPlans(): Promise<MealPlanResponse[]> {
  return request('/api/meal-plans/mine');
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

/** POST /api/meal-plans/{id}/duplicate — atomic server-side copy (days + recipes; not purchases/manual items). Omit `name` for the server's default ("<source> copy"). The copy is the caller's alone: no members, no share token. */
export function duplicateMealPlan(mealPlanId: string, name?: string): Promise<MealPlanResponse> {
  return request(`/api/meal-plans/${mealPlanId}/duplicate`, {
    method: 'POST',
    body: name !== undefined ? { name } : {},
  });
}

export interface ShareTokenResponse {
  token: string;
}

/**
 * GET /api/meal-plans/{id}/share — owner or member. Creates the share
 * link's backing token on the first call for an unshared plan; the
 * server is race-safe, so concurrent first calls return the same token.
 * There is no reset — a share link never expires or rotates.
 */
export function getShareToken(mealPlanId: string): Promise<ShareTokenResponse> {
  return request(`/api/meal-plans/${mealPlanId}/share`);
}

export interface AcceptInviteResponse {
  mealPlanId: string;
  name: string;
  role: PlanRole;
  /** True when the caller already had access (including the owner opening their own link) — nothing changed. */
  alreadyMember: boolean;
}

/** POST /api/meal-plan-invites/{token}/accept — idempotent. Unknown token: 404 NOT_FOUND. */
export function acceptInvite(token: string): Promise<AcceptInviteResponse> {
  return request(`/api/meal-plan-invites/${encodeURIComponent(token)}/accept`, { method: 'POST' });
}

export interface PlanMemberResponse {
  userId: string;
  /** Falls back to email when the member has no username. */
  username: string;
  role: PlanRole;
  joinedAt: string;
}

/** GET /api/meal-plans/{id}/members — owner or member. Owner first (joinedAt = the plan's createdAt), then members by join time. */
export function getMembers(mealPlanId: string): Promise<PlanMemberResponse[]> {
  return request(`/api/meal-plans/${mealPlanId}/members`);
}

/**
 * DELETE /api/meal-plans/{id}/members/{userId} — the owner removing
 * anyone, or a member removing themselves (leave). 204, idempotent (a
 * non-member is still 204). Removing the plan's owner (or a legacy
 * trip's owner) is 400 BAD_REQUEST; a member removing someone else is 403.
 */
export function removeMember(mealPlanId: string, userId: string): Promise<void> {
  return request(`/api/meal-plans/${mealPlanId}/members/${userId}`, { method: 'DELETE' });
}

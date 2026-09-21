import { request } from './http';
import type { IngredientResponse } from './ingredients';

export type RecipeStatus = 'draft' | 'published';

export interface RecipeResponse {
  id: string;
  name: string;
  description: string | null;
  webLink: string | null;
  baseServings: number;
  status: RecipeStatus;
  createdBy: string;
  duplicateOfId: string | null;
  meal: string | null;
  theme: string | null;
  /** How many people have favourited this recipe. Computed server-side, never stored on the recipe. */
  favoriteCount: number;
  /** Whether the X-User-Id caller has favourited it. */
  favoritedByMe: boolean;
  createdAt: string;
  updatedAt: string;
}

export type RecipeIngredientStatus = 'pending_review' | 'approved';

export interface RecipeIngredientResponse {
  id: string;
  recipeId: string;
  ingredient: IngredientResponse | null;
  originalText: string | null;
  quantity: number;
  unit: string;
  status: RecipeIngredientStatus;
  matchedIngredient: IngredientResponse | null;
  suggestedIngredientName: string | null;
  suggestedCategory: string | null;
  suggestedUnit: string | null;
  reviewFlags: string[];
  createdAt: string;
  updatedAt: string;
}

export interface RecipeDetailResponse extends RecipeResponse {
  duplicateOf: RecipeResponse | null;
  ingredients: RecipeIngredientResponse[];
}

export interface CreateRecipeIngredientRequest {
  ingredientId: string;
  quantity: number;
  unit: string;
}

export interface CreateRecipeRequest {
  name: string;
  description?: string;
  webLink?: string;
  baseServings: number;
  meal?: string;
  theme?: string;
  ingredients: CreateRecipeIngredientRequest[];
}

export interface UpdateRecipeRequest {
  name?: string;
  description?: string;
  baseServings?: number;
  meal?: string;
  theme?: string;
}

export type ResolveRecipeIngredientAction = 'CONFIRM_MATCH' | 'SELECT_EXISTING' | 'CREATE_NEW';

export interface ResolveRecipeIngredientRequest {
  action: ResolveRecipeIngredientAction;
  ingredientId?: string;
  newIngredient?: { name: string; category: string; defaultUnit: string };
  quantity?: number;
  unit?: string;
}

/** GET /api/recipes — published recipes plus the caller's own drafts (server-side filtered). */
export function getRecipes(): Promise<RecipeResponse[]> {
  return request('/api/recipes');
}

/** GET /api/recipes/{id} */
export function getRecipe(recipeId: string): Promise<RecipeDetailResponse> {
  return request(`/api/recipes/${recipeId}`);
}

/** POST /api/recipes — not optimistic; waits for the server. */
export function createRecipe(payload: CreateRecipeRequest): Promise<RecipeResponse> {
  return request('/api/recipes', { method: 'POST', body: payload });
}

/** PUT /api/recipes/{id} */
export function updateRecipe(recipeId: string, payload: UpdateRecipeRequest): Promise<RecipeResponse> {
  return request(`/api/recipes/${recipeId}`, { method: 'PUT', body: payload });
}

/** DELETE /api/recipes/{id} */
export function deleteRecipe(recipeId: string): Promise<void> {
  return request(`/api/recipes/${recipeId}`, { method: 'DELETE' });
}

/** POST /api/recipes/{id}/ingredients — add a new line to an existing recipe. */
export function addRecipeIngredient(
  recipeId: string,
  payload: CreateRecipeIngredientRequest,
): Promise<RecipeIngredientResponse> {
  return request(`/api/recipes/${recipeId}/ingredients`, { method: 'POST', body: payload });
}

/** DELETE /api/recipes/{id}/ingredients/{lineId} — lineId is the recipe-ingredient LINE id. */
export function removeRecipeIngredient(recipeId: string, lineId: string): Promise<void> {
  return request(`/api/recipes/${recipeId}/ingredients/${lineId}`, { method: 'DELETE' });
}

/** PUT /api/recipes/{id}/ingredients/{lineId} — resolves a pending line, or edits an approved one. */
export function resolveRecipeIngredient(
  recipeId: string,
  lineId: string,
  payload: ResolveRecipeIngredientRequest,
): Promise<RecipeIngredientResponse> {
  return request(`/api/recipes/${recipeId}/ingredients/${lineId}`, { method: 'PUT', body: payload });
}

const DEV_IMPORT_MIN_MS = 5000;

/**
 * POST /api/recipes/import — server-side scrape + LLM extraction, expect
 * 10-60s. 400 on a blank/invalid url, 409 (code CONFLICT) when a recipe
 * with that webLink already exists, 422 (code IMPORT_FAILED or
 * SCRAPE_FAILED) when the page couldn't be fetched or read.
 */
export function importRecipe(url: string): Promise<RecipeDetailResponse> {
  const sent = request<RecipeDetailResponse>('/api/recipes/import', { method: 'POST', body: { url } });
  if (!import.meta.env.DEV) return sent;

  // Dev server only: a real import takes up to a minute, but locally (no
  // ANTHROPIC_API_KEY, so the backend's stub scraper) it answers at once and
  // the waiting state — the dog — is gone before it can be seen. Hold the
  // answer, success or failure, until it has been up for five seconds.
  const shown = new Promise<void>((resolve) => window.setTimeout(resolve, DEV_IMPORT_MIN_MS));
  return Promise.allSettled([sent, shown]).then(() => sent);
}

export type ResolveDuplicateAction = 'NOT_DUPLICATE' | 'USE_EXISTING';

/**
 * PUT /api/recipes/{id}/resolve-duplicate — NOT_DUPLICATE returns the
 * (now unblocked) recipe; USE_EXISTING returns 204 and deletes THIS
 * recipe server-side.
 */
export function resolveDuplicate(recipeId: string, action: ResolveDuplicateAction): Promise<RecipeResponse | undefined> {
  return request(`/api/recipes/${recipeId}/resolve-duplicate`, { method: 'PUT', body: { action } });
}

/** POST /api/recipes/{id}/publish — 409 if already published, 422 while blocked (duplicate flag or pending lines). */
export function publishRecipe(recipeId: string): Promise<RecipeResponse> {
  return request(`/api/recipes/${recipeId}/publish`, { method: 'POST' });
}

/** The totals a favourite/un-favourite answers with, for the calling user. */
export interface RecipeFavoriteStatusResponse {
  recipeId: string;
  favoriteCount: number;
  favoritedByMe: boolean;
}

export interface RecipeFavoriteUserResponse {
  userId: string;
  /** Their username, falling back to their email when they have none. */
  username: string;
  favoritedAt: string;
}

/** PUT /api/recipes/{id}/favorite — idempotent; 404 if the recipe isn't visible to you. */
export function favoriteRecipe(recipeId: string): Promise<RecipeFavoriteStatusResponse> {
  return request(`/api/recipes/${recipeId}/favorite`, { method: 'PUT' });
}

/** DELETE /api/recipes/{id}/favorite — idempotent; answers 200 with the new totals, not 204. */
export function unfavoriteRecipe(recipeId: string): Promise<RecipeFavoriteStatusResponse> {
  return request(`/api/recipes/${recipeId}/favorite`, { method: 'DELETE' });
}

/** GET /api/recipes/{id}/favorites — who favourited it, oldest first. */
export function getRecipeFavorites(recipeId: string): Promise<RecipeFavoriteUserResponse[]> {
  return request(`/api/recipes/${recipeId}/favorites`);
}

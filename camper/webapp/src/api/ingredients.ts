import { request } from './http';

export interface IngredientResponse {
  id: string;
  name: string;
  category: string;
  defaultUnit: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateIngredientRequest {
  name: string;
  category: string;
  defaultUnit: string;
}

export interface UpdateIngredientRequest {
  name?: string;
  category?: string;
  defaultUnit?: string;
}

/** GET /api/ingredients — the full, unpaginated list. Search and filtering are client-side. */
export function getIngredients(): Promise<IngredientResponse[]> {
  return request('/api/ingredients');
}

/** POST /api/ingredients — 409 (code CONFLICT) when the name is already taken. */
export function createIngredient(payload: CreateIngredientRequest): Promise<IngredientResponse> {
  return request('/api/ingredients', { method: 'POST', body: payload });
}

/** PUT /api/ingredients/{id} — 409 (code CONFLICT) when the new name is already taken. */
export function updateIngredient(ingredientId: string, payload: UpdateIngredientRequest): Promise<IngredientResponse> {
  return request(`/api/ingredients/${ingredientId}`, { method: 'PUT', body: payload });
}

/**
 * DELETE /api/ingredients/{id} — destructive and global: every recipe
 * line using this ingredient goes back to `pending_review`, and every
 * recipe affected (any user's) flips back to draft.
 */
export function deleteIngredient(ingredientId: string): Promise<void> {
  return request(`/api/ingredients/${ingredientId}`, { method: 'DELETE' });
}

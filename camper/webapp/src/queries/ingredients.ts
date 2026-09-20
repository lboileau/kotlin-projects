import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query';
import { ApiError } from '../api/http';
import {
  createIngredient,
  deleteIngredient,
  getIngredients,
  updateIngredient,
  type CreateIngredientRequest,
  type IngredientResponse,
  type UpdateIngredientRequest,
} from '../api/ingredients';
import { recipesKey } from './recipes';

export const ingredientsKey = ['ingredients'] as const;

/** The full, cached ingredient list — `IngredientPicker` and the ingredients page both read from this one query. */
export function useIngredients() {
  return useQuery({ queryKey: ingredientsKey, queryFn: getIngredients });
}

function upsertIngredient(queryClient: QueryClient, ingredient: IngredientResponse) {
  queryClient.setQueryData<IngredientResponse[]>(ingredientsKey, (old) => {
    const list = old ?? [];
    const next = list.some((existing) => existing.id === ingredient.id)
      ? list.map((existing) => (existing.id === ingredient.id ? ingredient : existing))
      : [...list, ingredient];
    // Re-sort on every upsert, not just insert — a rename needs to land in the right spot too.
    return next.sort((a, b) => a.name.localeCompare(b.name, undefined, { sensitivity: 'base' }));
  });
}

/** Case-insensitive, trimmed name match against an already-fetched list. */
export function findIngredientByName(list: IngredientResponse[], name: string): IngredientResponse | undefined {
  const needle = name.trim().toLowerCase();
  return list.find((i) => i.name.trim().toLowerCase() === needle);
}

/**
 * Plain create — a 409 (name taken) throws like any other error. Used by
 * ingredient management (`NewIngredientSheet`), where a duplicate name is
 * a real mistake the user should see, not something to paper over.
 */
export function useCreateIngredient() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateIngredientRequest) => createIngredient(payload),
    meta: { suppressErrorToast: true },
    onSuccess: (created) => upsertIngredient(queryClient, created),
  });
}

/**
 * Shared "make sure this ingredient exists" body for both helpers below:
 * use `existing` if one was already found, otherwise create — falling
 * back to a fresh lookup on a genuine 409 (the DB's unique constraint on
 * name is case-SENSITIVE, so an exact-case collision can still race past
 * an already-fetched list).
 */
async function createOrUseExisting(
  queryClient: QueryClient,
  existing: IngredientResponse | undefined,
  payload: CreateIngredientRequest,
): Promise<IngredientResponse> {
  if (existing) return existing;
  try {
    const created = await createIngredient(payload);
    upsertIngredient(queryClient, created);
    return created;
  } catch (err) {
    if (err instanceof ApiError && err.code === 'CONFLICT') {
      const list = await queryClient.fetchQuery({ queryKey: ingredientsKey, queryFn: getIngredients, staleTime: 0 });
      const found = findIngredientByName(list, payload.name);
      if (found) return found;
    }
    throw err;
  }
}

/**
 * "Make sure this ingredient exists" — creates it, or returns the
 * existing one if the name is already taken (case-insensitively — a
 * stale cache could otherwise let `createOrFindIngredient("butter")`
 * through as a would-be sibling of an existing "Butter", since the DB's
 * own uniqueness check is case-sensitive and won't catch it). Always
 * forces a fresh fetch of the ingredient list first, so used where a
 * name collision isn't an error but the desired outcome (the ingredient
 * picker's create-in-place, and a single accept in the recipe review) —
 * unlike `useCreateIngredient` above. For several creates at once, see
 * `createOrFindIngredientIn`, which shares one fetch across all of them.
 */
export async function createOrFindIngredient(
  queryClient: QueryClient,
  payload: CreateIngredientRequest,
): Promise<IngredientResponse> {
  const freshList = await queryClient.fetchQuery({ queryKey: ingredientsKey, queryFn: getIngredients, staleTime: 0 });
  return createOrUseExisting(queryClient, findIngredientByName(freshList, payload.name), payload);
}

/**
 * Same contract as `createOrFindIngredient`, but checks a list the caller
 * already fetched instead of forcing its own fetch — used by "Accept
 * all" so N concurrent creates share ONE fresh fetch instead of one each.
 */
export async function createOrFindIngredientIn(
  queryClient: QueryClient,
  freshList: IngredientResponse[],
  payload: CreateIngredientRequest,
): Promise<IngredientResponse> {
  return createOrUseExisting(queryClient, findIngredientByName(freshList, payload.name), payload);
}

export function useUpdateIngredient(ingredientId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: UpdateIngredientRequest) => updateIngredient(ingredientId, payload),
    meta: { suppressErrorToast: true },
    onSuccess: (updated) => {
      upsertIngredient(queryClient, updated);
      // A rename or recategorise changes what a cached recipe detail's lines
      // and shopping rows (both keyed/labeled by ingredient) show.
      queryClient.invalidateQueries({ queryKey: ['recipe'] });
      queryClient.invalidateQueries({ queryKey: ['shopping'] });
    },
  });
}

/**
 * Deleting an ingredient is global and destructive: every recipe line
 * using it goes back to `pending_review`, and every affected recipe
 * (any user's) flips back to draft — so recipe lists/details and any
 * shopping list or plan referencing it are all invalidated too.
 */
export function useDeleteIngredient() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (ingredientId: string) => deleteIngredient(ingredientId),
    onSuccess: (_data, ingredientId) => {
      queryClient.setQueryData<IngredientResponse[]>(ingredientsKey, (old) =>
        (old ?? []).filter((ingredient) => ingredient.id !== ingredientId),
      );
      queryClient.invalidateQueries({ queryKey: recipesKey });
      // Prefix matches: invalidate every cached ['recipe', id], ['plan', id], etc. — not just one.
      queryClient.invalidateQueries({ queryKey: ['recipe'] });
      queryClient.invalidateQueries({ queryKey: ['shopping'] });
      queryClient.invalidateQueries({ queryKey: ['plan'] });
    },
  });
}

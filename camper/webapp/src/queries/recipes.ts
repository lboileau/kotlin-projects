import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  addRecipeIngredient,
  createRecipe,
  deleteRecipe,
  getRecipe,
  getRecipes,
  importRecipe,
  publishRecipe,
  removeRecipeIngredient,
  resolveDuplicate,
  resolveRecipeIngredient,
  updateRecipe,
  type CreateRecipeIngredientRequest,
  type CreateRecipeRequest,
  type RecipeDetailResponse,
  type ResolveDuplicateAction,
  type ResolveRecipeIngredientRequest,
  type UpdateRecipeRequest,
} from '../api/recipes';

export const recipesKey = ['recipes'] as const;
export const recipeKey = (recipeId: string) => ['recipe', recipeId] as const;

export function useRecipes() {
  return useQuery({ queryKey: recipesKey, queryFn: getRecipes });
}

export function useRecipe(recipeId: string | undefined) {
  return useQuery({
    queryKey: recipeKey(recipeId ?? ''),
    queryFn: () => getRecipe(recipeId as string),
    enabled: Boolean(recipeId),
  });
}

/** Creating (or importing) a recipe is not optimistic — see requirements. */
export function useCreateRecipe() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateRecipeRequest) => createRecipe(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: recipesKey });
    },
  });
}

export function useUpdateRecipe(recipeId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: UpdateRecipeRequest) => updateRecipe(recipeId, payload),
    onSuccess: (updated) => {
      queryClient.setQueryData<RecipeDetailResponse>(recipeKey(recipeId), (old) =>
        old ? { ...old, ...updated } : old,
      );
      queryClient.invalidateQueries({ queryKey: recipesKey });
    },
  });
}

export function useDeleteRecipe() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (recipeId: string) => deleteRecipe(recipeId),
    onSuccess: (_data, recipeId) => {
      queryClient.removeQueries({ queryKey: recipeKey(recipeId) });
      queryClient.invalidateQueries({ queryKey: recipesKey });
      // Deleting a recipe removes it from every meal plan that uses it
      // server-side, so a warm plan/shopping cache would otherwise still
      // list it and link to a 404. Prefix matches: ['plans'] catches
      // ['plans','mine'], ['plan'] catches every ['plan', id], etc.
      queryClient.invalidateQueries({ queryKey: ['plans'] });
      queryClient.invalidateQueries({ queryKey: ['plan'] });
      queryClient.invalidateQueries({ queryKey: ['shopping'] });
    },
  });
}

/**
 * Ingredient line changes on an existing recipe apply immediately, one
 * endpoint call per change. Each also invalidates `['recipes']` — adding,
 * removing or resolving a line can change the recipe's draft status,
 * which the recipe list (draft badge, "mine" filter) shows.
 */
export function useAddRecipeIngredient(recipeId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateRecipeIngredientRequest) => addRecipeIngredient(recipeId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: recipeKey(recipeId) });
      queryClient.invalidateQueries({ queryKey: recipesKey });
    },
  });
}

export function useRemoveRecipeIngredient(recipeId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (lineId: string) => removeRecipeIngredient(recipeId, lineId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: recipeKey(recipeId) });
      queryClient.invalidateQueries({ queryKey: recipesKey });
    },
  });
}

export function useResolveRecipeIngredient(recipeId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ lineId, payload }: { lineId: string; payload: ResolveRecipeIngredientRequest }) =>
      resolveRecipeIngredient(recipeId, lineId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: recipeKey(recipeId) });
      queryClient.invalidateQueries({ queryKey: recipesKey });
    },
  });
}

/**
 * Importing waits for the server (10-60s scrape + LLM call). The caller
 * shows its own inline error per code, so the global toast is suppressed.
 */
export function useImportRecipe() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (url: string) => importRecipe(url),
    meta: { suppressErrorToast: true },
    onSuccess: (created) => {
      queryClient.setQueryData(recipeKey(created.id), created);
      queryClient.invalidateQueries({ queryKey: recipesKey });
    },
  });
}

export function useResolveDuplicate(recipeId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (action: ResolveDuplicateAction) => resolveDuplicate(recipeId, action),
    onSuccess: (_data, action) => {
      if (action === 'USE_EXISTING') {
        // The recipe itself was deleted server-side.
        queryClient.removeQueries({ queryKey: recipeKey(recipeId) });
        queryClient.invalidateQueries({ queryKey: recipesKey });
      } else {
        queryClient.invalidateQueries({ queryKey: recipeKey(recipeId) });
      }
    },
  });
}

export function usePublishRecipe(recipeId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => publishRecipe(recipeId),
    onSuccess: (updated) => {
      queryClient.setQueryData<RecipeDetailResponse>(recipeKey(recipeId), (old) =>
        old ? { ...old, ...updated } : old,
      );
      queryClient.invalidateQueries({ queryKey: recipesKey });
    },
  });
}

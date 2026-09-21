import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  addRecipeIngredient,
  createRecipe,
  deleteRecipe,
  favoriteRecipe,
  getRecipe,
  getRecipeFavorites,
  getRecipes,
  importRecipe,
  publishRecipe,
  removeRecipeIngredient,
  resolveDuplicate,
  resolveRecipeIngredient,
  unfavoriteRecipe,
  updateRecipe,
  type CreateRecipeIngredientRequest,
  type CreateRecipeRequest,
  type RecipeDetailResponse,
  type RecipeFavoriteStatusResponse,
  type RecipeFavoriteUserResponse,
  type RecipeResponse,
  type ResolveDuplicateAction,
  type ResolveRecipeIngredientRequest,
  type UpdateRecipeRequest,
} from '../api/recipes';
import { applyFavoriteToDetail, applyFavoriteToList, applyFavoriteToPeople } from '../lib/recipeFavorites';
import { useAuth } from '../auth/useAuth';

export const recipesKey = ['recipes'] as const;
export const recipeKey = (recipeId: string) => ['recipe', recipeId] as const;
export const recipeFavoritesKey = (recipeId: string) => ['recipe-favorites', recipeId] as const;

/** One key for every recipe's toggle, so `isMutating` can count them all at settle time. */
const favoriteMutationKey = ['recipe-favorite'] as const;

export function useRecipes() {
  return useQuery({ queryKey: recipesKey, queryFn: getRecipes });
}

/**
 * Who favourited a recipe, oldest first. One query shared by two callers:
 * the recipe page's who-line ("You, Alice and 1 other"), which passes
 * `enabled: false` while the count is 0 so nothing is fetched for a recipe
 * nobody has favourited, and the `FavouritedBySheet` it links to, which
 * always wants it. With the sheet open over the page they are the same key,
 * so that is one request, not two.
 */
export function useRecipeFavorites(recipeId: string | undefined, options?: { enabled?: boolean }) {
  return useQuery({
    queryKey: recipeFavoritesKey(recipeId ?? ''),
    queryFn: () => getRecipeFavorites(recipeId as string),
    enabled: Boolean(recipeId) && (options?.enabled ?? true),
  });
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

export interface RecipeEdits {
  /** Changed fields only; omitted when none changed. */
  fields?: UpdateRecipeRequest;
  removedLineIds: string[];
  changedLines: { lineId: string; ingredientId: string; quantity: number; unit: string }[];
  addedLines: CreateRecipeIngredientRequest[];
}

/**
 * Saves everything the edit form changed, in one go: the recipe's fields,
 * then its lines (removals, changes, additions) through the per-line
 * endpoints, since there is no endpoint that replaces a recipe's lines. One
 * call after another so a failure stops at a known point; whatever happened,
 * the recipe is refetched afterwards so the form's next open shows the
 * server's truth rather than a guess.
 */
export function useSaveRecipeEdits(recipeId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ fields, removedLineIds, changedLines, addedLines }: RecipeEdits) => {
      if (fields) await updateRecipe(recipeId, fields);
      for (const lineId of removedLineIds) await removeRecipeIngredient(recipeId, lineId);
      for (const { lineId, ...line } of changedLines) {
        await resolveRecipeIngredient(recipeId, lineId, { action: 'SELECT_EXISTING', ...line });
      }
      for (const line of addedLines) await addRecipeIngredient(recipeId, line);
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: recipeKey(recipeId) });
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

// Per-recipe network serialization + coalescing, the same idiom as
// `rowChains` / `rowLatestPurchases` in queries/shopping.ts. A PUT and a
// DELETE for the same heart can otherwise reach the server out of order
// (tap on then off quickly) and whichever lands last wins, which may not be
// the one the user meant. `favoriteChains` serializes: a recipe's next send
// waits for its previous one to settle. `favoriteLatest` coalesces: a send
// superseded before its turn comes is skipped entirely, so only the latest
// desired state is ever sent. The optimistic cache write in `onMutate` is
// untouched by any of this — it is what keeps every tap instant.
//
// Keyed by recipe id alone: unlike a shopping row (whose key is per
// ingredient, and the same ingredient appears in several plans' lists),
// recipes are globally unique, so no composite key is needed.
const favoriteChains = new Map<string, Promise<void>>();
const favoriteLatest = new Map<string, boolean>();

function sendFavoriteInOrder(
  recipeId: string,
  favorited: boolean,
): Promise<RecipeFavoriteStatusResponse | undefined> {
  favoriteLatest.set(recipeId, favorited);

  const previousLink = favoriteChains.get(recipeId) ?? Promise.resolve();
  const thisLink = previousLink
    .catch(() => {
      // A previous link's own failure is reported through THAT mutate()
      // call, not this one — it must not block this recipe's future taps
      // from ever running.
    })
    .then(() => {
      const desired = favoriteLatest.get(recipeId);
      if (desired === undefined) return undefined; // a later link already sent it
      favoriteLatest.delete(recipeId);
      return desired ? favoriteRecipe(recipeId) : unfavoriteRecipe(recipeId);
    });

  favoriteChains.set(
    recipeId,
    thisLink.then(
      () => undefined,
      () => undefined,
    ),
  );
  return thisLink;
}

/**
 * Optimistic favourite toggle. Writes to the list cache, the detail cache
 * AND the cached list of people behind the who-line at once — so the count,
 * the heart and "You" all change on the tap rather than after a refetch. It
 * rolls back with the inverse edit applied to whatever is in the cache at
 * that moment (never a whole-list snapshot, which would wipe a different
 * concurrent mutation that already succeeded — see webapp/CLAUDE.md). Sends
 * are serialized and coalesced per recipe (`sendFavoriteInOrder`), so rapid
 * double-taps on one heart cannot land out of order. Failures surface
 * through the global mutation error toast.
 */
export function useToggleFavorite() {
  const queryClient = useQueryClient();
  // The viewer's own row in the people list, so the who-line can show "You"
  // immediately. Signed out there is nobody to add, and the rest of the
  // toggle still works on its own.
  const { user } = useAuth();
  const me: RecipeFavoriteUserResponse | null = user
    ? { userId: user.id, username: user.username ?? user.email, favoritedAt: new Date().toISOString() }
    : null;

  return useMutation({
    mutationKey: favoriteMutationKey,
    mutationFn: ({ recipeId, favorited }: { recipeId: string; favorited: boolean }) =>
      sendFavoriteInOrder(recipeId, favorited),
    onMutate: async ({ recipeId, favorited }) => {
      await queryClient.cancelQueries({ queryKey: recipesKey });
      await queryClient.cancelQueries({ queryKey: recipeKey(recipeId) });
      await queryClient.cancelQueries({ queryKey: recipeFavoritesKey(recipeId) });
      queryClient.setQueryData<RecipeResponse[]>(recipesKey, (current) =>
        applyFavoriteToList(current, recipeId, favorited),
      );
      queryClient.setQueryData<RecipeDetailResponse>(recipeKey(recipeId), (current) =>
        applyFavoriteToDetail(current, recipeId, favorited),
      );
      if (me) {
        queryClient.setQueryData<RecipeFavoriteUserResponse[]>(recipeFavoritesKey(recipeId), (current) =>
          applyFavoriteToPeople(current, me, favorited),
        );
      }
    },
    onError: (_error, { recipeId, favorited }) => {
      // Targeted inverse edit on the CURRENT cache. `setFavorite` and
      // `applyFavoriteToPeople` are both guarded, so if something already
      // brought the row back this is a no-op rather than a second decrement
      // (or a second removal).
      queryClient.setQueryData<RecipeResponse[]>(recipesKey, (current) =>
        applyFavoriteToList(current, recipeId, !favorited),
      );
      queryClient.setQueryData<RecipeDetailResponse>(recipeKey(recipeId), (current) =>
        applyFavoriteToDetail(current, recipeId, !favorited),
      );
      if (me) {
        queryClient.setQueryData<RecipeFavoriteUserResponse[]>(recipeFavoritesKey(recipeId), (current) =>
          applyFavoriteToPeople(current, me, !favorited),
        );
      }
    },
    onSuccess: (status, { recipeId }) => {
      // A coalesced link sends nothing and resolves undefined — the
      // optimistic write already says the right thing, so leave the cache
      // alone rather than overwriting it with a guess.
      if (!status) return;
      queryClient.setQueryData<RecipeResponse[]>(recipesKey, (current) =>
        current?.map((recipe) =>
          recipe.id === recipeId
            ? { ...recipe, favoriteCount: status.favoriteCount, favoritedByMe: status.favoritedByMe }
            : recipe,
        ),
      );
      queryClient.setQueryData<RecipeDetailResponse>(recipeKey(recipeId), (current) =>
        current && current.id === recipeId
          ? { ...current, favoriteCount: status.favoriteCount, favoritedByMe: status.favoritedByMe }
          : current,
      );
      // Re-apply the viewer's own row against the server's answer. On the
      // 0 → 1 tap the people query was disabled (count 0) and the cache
      // empty; `onMutate`'s seed flips `enabled` to true, so a GET
      // /favorites mounts and races this PUT — if it lands first it returns
      // `[]` and wipes the seeded "You", dropping the who-line back to
      // "1 person" until the settle-time invalidation refetches. The edit is
      // guarded (`applyFavoriteToPeople` returns the identical array when
      // the viewer is already in/out), so it is a no-op whenever the cache
      // is already right, and it can never double-add or over-remove.
      if (me) {
        queryClient.setQueryData<RecipeFavoriteUserResponse[]>(recipeFavoritesKey(recipeId), (current) =>
          applyFavoriteToPeople(current, me, status.favoritedByMe),
        );
      }
    },
    onSettled: (_data, _error, { recipeId }) => {
      // `=== 1`, not `=== 0`: TanStack Query still counts THIS mutation as
      // in flight while its own onSettled runs (see webapp/CLAUDE.md).
      if (queryClient.isMutating({ mutationKey: favoriteMutationKey }) === 1) {
        void queryClient.invalidateQueries({ queryKey: recipesKey });
        void queryClient.invalidateQueries({ queryKey: recipeKey(recipeId) });
      }
      // The who-line and its sheet both read the names, and the caller's own
      // row has just changed; always refresh it. Invalidating a query that
      // isn't mounted (the count was 0, so it is disabled) only marks it
      // stale — it does not fetch.
      void queryClient.invalidateQueries({ queryKey: recipeFavoritesKey(recipeId) });
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

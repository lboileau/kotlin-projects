import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  addMealPlanDay,
  addRecipeToMeal,
  createMealPlan,
  deleteMealPlan,
  getMealPlanDetail,
  listMyMealPlans,
  removeRecipeFromMeal,
  updateMealPlan,
  type MealPlanDayResponse,
  type MealPlanDetailResponse,
  type MealPlanRecipeDetailResponse,
  type MealPlanResponse,
  type MealType,
} from '../api/mealPlans';
import { ApiError } from '../api/http';
import { useAuth } from '../auth/useAuth';
import { findRecipeInPlan, getLowestDay, MEAL_TYPES, type FlatPlanRecipe } from '../lib/flatPlan';

export const plansKey = ['plans', 'mine'] as const;
export const planKey = (planId: string) => ['plan', planId] as const;
export const shoppingKey = (planId: string) => ['shopping', planId] as const;

/** My plans (home list), templates filtered out, newest-updated first. */
export function usePlans() {
  const { user } = useAuth();
  return useQuery({
    queryKey: plansKey,
    queryFn: () => listMyMealPlans(user!.id),
    enabled: !!user,
    select: (plans) =>
      plans
        .filter((plan) => !plan.isTemplate)
        .slice()
        .sort((a, b) => (a.updatedAt < b.updatedAt ? 1 : -1)),
  });
}

/** A single plan's full detail (days/meals/recipes). */
export function usePlan(planId: string | undefined) {
  return useQuery({
    queryKey: planKey(planId ?? ''),
    queryFn: () => getMealPlanDetail(planId!),
    enabled: !!planId,
    retry: (failureCount, error) => {
      // A 404 means "not found" — show that immediately, don't retry it.
      if (error instanceof ApiError && error.status === 404) return false;
      return failureCount < 1;
    },
  });
}

/** Create a plan, then its day 1 — not optimistic, the caller shows a loading button. */
export function useCreatePlan() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: { name: string; servings: number }) => {
      const plan = await createMealPlan(input);
      await addMealPlanDay(plan.id, 1);
      return plan;
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: plansKey });
    },
  });
}

interface UpdatePlanInput {
  name?: string;
  servings?: number;
}

/** Optimistic rename / servings update, shared by the edit sheet and the servings stepper. */
export function useUpdatePlan(planId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    // Lets live sync (see src/sync/) tell "a mutation for this plan is
    // still in flight" apart from "nothing's happening", so it can defer
    // invalidating and never flickers the user's own optimistic edit.
    mutationKey: planKey(planId),
    mutationFn: (input: UpdatePlanInput) => updateMealPlan(planId, input),
    onMutate: async (input) => {
      await queryClient.cancelQueries({ queryKey: planKey(planId) });

      // Only what THIS mutation is about to change, so a rollback can
      // later restore exactly those fields rather than the whole
      // snapshot (which would also undo a different in-flight edit that
      // already succeeded — e.g. two servings taps racing).
      const previousDetail = queryClient.getQueryData<MealPlanDetailResponse>(planKey(planId));
      const previousFields: UpdatePlanInput = {};
      if (previousDetail) {
        if (input.name !== undefined) previousFields.name = previousDetail.name;
        if (input.servings !== undefined) previousFields.servings = previousDetail.servings;
      }

      queryClient.setQueryData<MealPlanDetailResponse>(planKey(planId), (current) =>
        current ? { ...current, ...input } : current,
      );
      queryClient.setQueryData<MealPlanResponse[]>(plansKey, (current) =>
        current ? current.map((plan) => (plan.id === planId ? { ...plan, ...input } : plan)) : current,
      );

      return { input, previousFields };
    },
    onError: (_error, input, context) => {
      if (!context) return;

      // Restore a field only if it's still showing exactly what THIS
      // mutation wrote — if something newer already changed it again
      // (successfully), leave that alone.
      queryClient.setQueryData<MealPlanDetailResponse>(planKey(planId), (current) => {
        if (!current) return current;
        const patch: UpdatePlanInput = {};
        if (input.name !== undefined && current.name === input.name) patch.name = context.previousFields.name;
        if (input.servings !== undefined && current.servings === input.servings) {
          patch.servings = context.previousFields.servings;
        }
        return { ...current, ...patch };
      });
      queryClient.setQueryData<MealPlanResponse[]>(plansKey, (current) => {
        if (!current) return current;
        return current.map((plan) => {
          if (plan.id !== planId) return plan;
          const patch: UpdatePlanInput = {};
          if (input.name !== undefined && plan.name === input.name) patch.name = context.previousFields.name;
          if (input.servings !== undefined && plan.servings === input.servings) {
            patch.servings = context.previousFields.servings;
          }
          return { ...plan, ...patch };
        });
      });
    },
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: planKey(planId) });
      void queryClient.invalidateQueries({ queryKey: plansKey });
      void queryClient.invalidateQueries({ queryKey: shoppingKey(planId) });
    },
  });
}

/** Delete waits for the server — the caller navigates away and clears caches on success. */
export function useDeletePlan() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (planId: string) => deleteMealPlan(planId),
    onSuccess: (_data, planId) => {
      queryClient.removeQueries({ queryKey: planKey(planId) });
      queryClient.removeQueries({ queryKey: shoppingKey(planId) });
      queryClient.setQueryData<MealPlanResponse[]>(plansKey, (plans) =>
        plans ? plans.filter((plan) => plan.id !== planId) : plans,
      );
      void queryClient.invalidateQueries({ queryKey: plansKey });
    },
  });
}

function emptyDay(dayNumber: number): MealPlanDayResponse {
  return { id: `temp-day-${dayNumber}`, dayNumber, meals: { breakfast: [], lunch: [], dinner: [], snack: [] } };
}

// Module-level (not per-hook-instance) so every useAddRecipeToPlan caller
// for the same plan — the picker sheet, PlanDetailPage's Undo, a recipe's
// "Add to plan" sheet — shares one in-flight "create day 1" request
// instead of racing to create it themselves. Cleared once it settles.
const dayResolutionInFlight = new Map<string, Promise<MealPlanDayResponse>>();

async function createDayOne(planId: string): Promise<MealPlanDayResponse> {
  try {
    return await addMealPlanDay(planId, 1);
  } catch (error) {
    // Lost the race — someone else's day 1 already exists. Use it.
    if (error instanceof ApiError && error.status === 409) {
      const fresh = await getMealPlanDetail(planId);
      const freshDay = getLowestDay(fresh);
      if (freshDay) return freshDay;
    }
    throw error;
  }
}

/** Resolves the plan's lowest day, serializing concurrent "no days yet" callers onto one request. */
async function resolveLowestDay(planId: string, detail: MealPlanDetailResponse): Promise<MealPlanDayResponse> {
  const existingDay = getLowestDay(detail);
  if (existingDay) return existingDay;

  let promise = dayResolutionInFlight.get(planId);
  if (!promise) {
    promise = createDayOne(planId).finally(() => {
      dayResolutionInFlight.delete(planId);
    });
    dayResolutionInFlight.set(planId, promise);
  }
  return promise;
}

export interface AddRecipeToPlanInput {
  planId: string;
  recipeId: string;
  /** Optional — when known, enables an optimistic insert into the cached plan detail. */
  recipeName?: string;
  recipeWebLink?: string | null;
  baseServings?: number;
}

export interface AddRecipeToPlanResult {
  entry: MealPlanRecipeDetailResponse;
  /** True when the recipe was already in the plan and nothing was added — the caller can toast accordingly. */
  alreadyInPlan: boolean;
}

/**
 * Adds a recipe to the plan's lowest-numbered day (creating day 1 first
 * if the plan has none), meal type `dinner`. Used both by the plan's own
 * recipe picker and by a recipe's "Add to plan" sheet — the latter often
 * doesn't have the recipe's name/servings on hand, so those are optional
 * and the optimistic insert is skipped when they're missing (the mutation
 * still runs and settles correctly either way).
 *
 * `boundPlanId` is optional and only affects `mutationKey` (so live sync
 * can see this mutation in flight and defer invalidating) — pass it when
 * the caller always targets one known plan (the plan detail page, its
 * own recipe picker); omit it where the target plan varies per call (a
 * recipe's "Add to plan" sheet, which lets the user pick any plan).
 */
export function useAddRecipeToPlan(boundPlanId?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationKey: boundPlanId ? planKey(boundPlanId) : undefined,
    mutationFn: async ({ planId, recipeId }: AddRecipeToPlanInput): Promise<AddRecipeToPlanResult> => {
      let detail = queryClient.getQueryData<MealPlanDetailResponse>(planKey(planId));
      if (!detail) {
        detail = await getMealPlanDetail(planId);
      }

      // A recipe already in the plan (e.g. added from another sheet
      // whose "Added" badge is cache-only and can be stale) is a no-op,
      // not a second server row.
      const existing = findRecipeInPlan(detail, recipeId);
      if (existing) {
        return { entry: existing, alreadyInPlan: true };
      }

      const day = await resolveLowestDay(planId, detail);
      const entry = await addRecipeToMeal(planId, day.id, { mealType: 'dinner', recipeId });
      return { entry, alreadyInPlan: false };
    },
    onMutate: async ({ planId, recipeId, recipeName, recipeWebLink, baseServings }) => {
      await queryClient.cancelQueries({ queryKey: planKey(planId) });
      const previous = queryClient.getQueryData<MealPlanDetailResponse>(planKey(planId));
      const alreadyInPlan = previous ? findRecipeInPlan(previous, recipeId) !== null : false;

      let tempId: string | null = null;

      if (previous && !alreadyInPlan && recipeName !== undefined && baseServings !== undefined) {
        tempId = `temp-${crypto.randomUUID()}`;
        const tempEntry: MealPlanRecipeDetailResponse = {
          id: tempId,
          recipeId,
          recipeName,
          recipeWebLink: recipeWebLink ?? null,
          baseServings,
          scaleFactor: 1,
          isFullyPurchased: false,
          ingredients: [],
        };
        queryClient.setQueryData<MealPlanDetailResponse>(planKey(planId), (current) => {
          if (!current) return current;
          const [firstDay, ...restDays] = current.days.length > 0 ? current.days : [emptyDay(1)];
          const updatedFirstDay: MealPlanDayResponse = {
            ...firstDay,
            meals: { ...firstDay.meals, dinner: [...firstDay.meals.dinner, tempEntry] },
          };
          return { ...current, days: [updatedFirstDay, ...restDays] };
        });
      }

      return { tempId };
    },
    onError: (_error, { planId }, context) => {
      // Targeted: remove only this call's own temp entry (by its
      // generated id), on top of the CURRENT cache — a whole-snapshot
      // rollback here would also wipe out any other add that succeeded
      // in the meantime (e.g. rapid taps in the recipe picker).
      const tempId = context?.tempId;
      if (!tempId) return;
      queryClient.setQueryData<MealPlanDetailResponse>(planKey(planId), (current) => {
        if (!current) return current;
        return {
          ...current,
          days: current.days.map((day) => ({
            ...day,
            meals: {
              breakfast: day.meals.breakfast.filter((entry) => entry.id !== tempId),
              lunch: day.meals.lunch.filter((entry) => entry.id !== tempId),
              dinner: day.meals.dinner.filter((entry) => entry.id !== tempId),
              snack: day.meals.snack.filter((entry) => entry.id !== tempId),
            },
          })),
        };
      });
    },
    onSettled: (_data, _error, { planId }) => {
      void queryClient.invalidateQueries({ queryKey: planKey(planId) });
      void queryClient.invalidateQueries({ queryKey: shoppingKey(planId) });
    },
  });
}

export interface RemoveRecipeFromPlanInput {
  planId: string;
  recipeId: string;
  mealPlanRecipeIds: string[];
}

interface RemovedPlanEntry {
  dayId: string;
  mealType: MealType;
  entry: MealPlanRecipeDetailResponse;
}

/**
 * Removes every occurrence of the recipe from the plan. Optimistic — the
 * caller shows an Undo toast. `boundPlanId` is optional, same rationale
 * as `useAddRecipeToPlan` — only ever called with a known target plan in
 * this app so far, but keeping it optional matches that hook's shape.
 */
export function useRemoveRecipeFromPlan(boundPlanId?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationKey: boundPlanId ? planKey(boundPlanId) : undefined,
    mutationFn: async ({ mealPlanRecipeIds }: RemoveRecipeFromPlanInput) => {
      // allSettled, not all: for a legacy recipe with several occurrences,
      // one failing shouldn't stop the others from actually being
      // deleted server-side. Throwing (once) after still triggers the
      // rollback + toast; onSettled's invalidation then refetches the
      // real state, which reconciles any rows that did succeed.
      const results = await Promise.allSettled(mealPlanRecipeIds.map((id) => removeRecipeFromMeal(id)));
      const failure = results.find((result): result is PromiseRejectedResult => result.status === 'rejected');
      if (failure) throw failure.reason;
    },
    onMutate: async ({ planId, recipeId }) => {
      await queryClient.cancelQueries({ queryKey: planKey(planId) });
      const previous = queryClient.getQueryData<MealPlanDetailResponse>(planKey(planId));

      // Capture exactly which entries this call is about to remove (and
      // where), so a failed removal can splice back only those — not a
      // whole-list snapshot, which would also undo an unrelated add or
      // remove that succeeded in the meantime.
      const removedEntries: RemovedPlanEntry[] = [];
      if (previous) {
        for (const day of previous.days) {
          for (const mealType of MEAL_TYPES) {
            for (const entry of day.meals[mealType]) {
              if (entry.recipeId === recipeId) removedEntries.push({ dayId: day.id, mealType, entry });
            }
          }
        }

        queryClient.setQueryData<MealPlanDetailResponse>(planKey(planId), (current) => {
          if (!current) return current;
          return {
            ...current,
            days: current.days.map((day) => ({
              ...day,
              meals: {
                breakfast: day.meals.breakfast.filter((entry) => entry.recipeId !== recipeId),
                lunch: day.meals.lunch.filter((entry) => entry.recipeId !== recipeId),
                dinner: day.meals.dinner.filter((entry) => entry.recipeId !== recipeId),
                snack: day.meals.snack.filter((entry) => entry.recipeId !== recipeId),
              },
            })),
          };
        });
      }

      return { removedEntries };
    },
    onError: (_error, { planId }, context) => {
      const removedEntries = context?.removedEntries;
      if (!removedEntries?.length) return;
      queryClient.setQueryData<MealPlanDetailResponse>(planKey(planId), (current) => {
        if (!current) return current;
        let days = current.days;
        for (const { dayId, mealType, entry } of removedEntries) {
          days = days.map((day) => {
            if (day.id !== dayId) return day;
            if (day.meals[mealType].some((existing) => existing.id === entry.id)) return day; // already back
            return { ...day, meals: { ...day.meals, [mealType]: [...day.meals[mealType], entry] } };
          });
        }
        return { ...current, days };
      });
    },
    onSettled: (_data, _error, { planId }) => {
      void queryClient.invalidateQueries({ queryKey: planKey(planId) });
      void queryClient.invalidateQueries({ queryKey: shoppingKey(planId) });
    },
  });
}

export interface DuplicatePlanResult {
  newPlanId: string;
  failedRecipeNames: string[];
}

/**
 * Plain async helper rather than a mutation hook: duplicating needs
 * per-recipe progress text while it runs, which doesn't fit useMutation's
 * single pending/success/error shape. The caller (the edit sheet) drives
 * its own loading/progress state and invalidates `plansKey` itself.
 */
export async function duplicatePlan(
  plan: MealPlanResponse,
  recipes: FlatPlanRecipe[],
  onProgress?: (done: number, total: number) => void,
): Promise<DuplicatePlanResult> {
  const newPlan = await createMealPlan({ name: `${plan.name} copy`, servings: plan.servings });
  const day = await addMealPlanDay(newPlan.id, 1);

  const failedRecipeNames: string[] = [];
  for (let i = 0; i < recipes.length; i++) {
    try {
      await addRecipeToMeal(newPlan.id, day.id, { mealType: 'dinner', recipeId: recipes[i].recipeId });
    } catch {
      failedRecipeNames.push(recipes[i].recipeName);
    }
    onProgress?.(i + 1, recipes.length);
  }

  return { newPlanId: newPlan.id, failedRecipeNames };
}

// Re-exported so consumers of the plans domain don't also need to reach
// into `lib/flatPlan` directly for this one type.
export type { FlatPlanRecipe };

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  acceptInvite,
  addRecipeToPlan,
  createMealPlan,
  deleteMealPlan,
  duplicateMealPlan,
  getMealPlanDetail,
  getMembers,
  getShareToken,
  listMyMealPlans,
  removeMember,
  removeRecipeFromPlan,
  updateMealPlan,
  type MealPlanDayResponse,
  type MealPlanDetailResponse,
  type MealPlanRecipeDetailResponse,
  type MealPlanResponse,
  type MealType,
  type PlanMemberResponse,
} from '../api/mealPlans';
import { ApiError } from '../api/http';
import type { ShoppingListResponse } from '../api/shopping';
import { useAuth } from '../auth/useAuth';
import { findRecipeInPlan, MEAL_TYPES } from '../lib/flatPlan';
import { toast } from '../lib/toastStore';

export const plansKey = ['plans', 'mine'] as const;
export const planKey = (planId: string) => ['plan', planId] as const;
export const shoppingKey = (planId: string) => ['shopping', planId] as const;
export const shareKey = (planId: string) => ['plan', planId, 'share'] as const;
export const membersKey = (planId: string) => ['plan', planId, 'members'] as const;

/** My plans (home list): owned plus shared, templates filtered out, newest-updated first. */
export function usePlans() {
  const { user } = useAuth();
  return useQuery({
    queryKey: plansKey,
    queryFn: () => listMyMealPlans(),
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
      // A 404 means "not found", a 403 means "no access" — both show
      // immediately, neither is worth retrying.
      if (error instanceof ApiError && (error.status === 404 || error.status === 403)) return false;
      return failureCount < 1;
    },
  });
}

/** Create a plan — not optimistic, the caller shows a loading button. Day 1 is created lazily, server-side, on the first recipe add. */
export function useCreatePlan() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { name: string; servings: number }) => createMealPlan(input),
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

      // The shopping list's header shows the plan name from its own
      // response (`mealPlanName`), not from the plan detail — captured
      // separately (from the shopping cache itself, not `previousDetail`)
      // so its rollback is targeted to exactly what this mutation wrote.
      const previousMealPlanName = queryClient.getQueryData<ShoppingListResponse>(shoppingKey(planId))?.mealPlanName;

      queryClient.setQueryData<MealPlanDetailResponse>(planKey(planId), (current) =>
        current ? { ...current, ...input } : current,
      );
      queryClient.setQueryData<MealPlanResponse[]>(plansKey, (current) =>
        current ? current.map((plan) => (plan.id === planId ? { ...plan, ...input } : plan)) : current,
      );
      if (input.name !== undefined) {
        queryClient.setQueryData<ShoppingListResponse>(shoppingKey(planId), (current) =>
          current ? { ...current, mealPlanName: input.name! } : current,
        );
      }

      return { input, previousFields, previousMealPlanName };
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
      if (input.name !== undefined && context.previousMealPlanName !== undefined) {
        queryClient.setQueryData<ShoppingListResponse>(shoppingKey(planId), (current) => {
          if (!current || current.mealPlanName !== input.name) return current;
          return { ...current, mealPlanName: context.previousMealPlanName! };
        });
      }
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
 * Adds a recipe to the plan — a single plan-level POST. The server picks
 * the lowest-numbered day (creating day 1 first if the plan has none),
 * meal type `dinner`, and is concurrency-safe; its status carries whether
 * anything was actually added (`201`) or the recipe was already anywhere
 * in the plan (`200`, the existing entry). Used both by the plan's own
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
      const { data: entry, status } = await addRecipeToPlan(planId, recipeId);
      return { entry, alreadyInPlan: status === 200 };
    },
    onMutate: async ({ planId, recipeId, recipeName, recipeWebLink, baseServings }) => {
      await queryClient.cancelQueries({ queryKey: planKey(planId) });
      const previous = queryClient.getQueryData<MealPlanDetailResponse>(planKey(planId));
      // Best-effort, cache-only guess (can be stale) — used only to skip
      // showing a duplicate optimistic row/count while the server's own
      // 200-vs-201 (the actual source of truth) is still in flight.
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

      // recipeCount is "distinct recipes", so it only needs bumping when
      // we don't already believe the recipe is in the plan — independent
      // of whether recipeName/baseServings were given (AddToPlanSheet
      // doesn't have them, but still affects this count).
      let recipeCountBumped = false;
      if (!alreadyInPlan) {
        recipeCountBumped = true;
        queryClient.setQueryData<MealPlanResponse[]>(plansKey, (current) =>
          current
            ? current.map((plan) => (plan.id === planId ? { ...plan, recipeCount: plan.recipeCount + 1 } : plan))
            : current,
        );
      }

      return { tempId, recipeCountBumped };
    },
    onError: (_error, { planId }, context) => {
      // Targeted: remove only this call's own temp entry (by its
      // generated id), on top of the CURRENT cache — a whole-snapshot
      // rollback here would also wipe out any other add that succeeded
      // in the meantime (e.g. rapid taps in the recipe picker).
      const tempId = context?.tempId;
      if (tempId) {
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
      }
      if (context?.recipeCountBumped) {
        queryClient.setQueryData<MealPlanResponse[]>(plansKey, (current) =>
          current
            ? current.map((plan) =>
                plan.id === planId ? { ...plan, recipeCount: Math.max(0, plan.recipeCount - 1) } : plan,
              )
            : current,
        );
      }
    },
    onSettled: (_data, _error, { planId }) => {
      void queryClient.invalidateQueries({ queryKey: planKey(planId) });
      void queryClient.invalidateQueries({ queryKey: shoppingKey(planId) });
      // Reconciles recipeCount for real in case the onMutate guess above
      // (from possibly-stale cached detail) didn't match the server's.
      void queryClient.invalidateQueries({ queryKey: plansKey });
    },
  });
}

export interface RemoveRecipeFromPlanInput {
  planId: string;
  recipeId: string;
}

interface RemovedPlanEntry {
  dayId: string;
  mealType: MealType;
  entry: MealPlanRecipeDetailResponse;
}

/**
 * Removes every occurrence of the recipe from the plan — a single
 * plan-level DELETE by recipeId, idempotent server-side. Optimistic — the
 * caller shows an Undo toast. `boundPlanId` is optional, same rationale
 * as `useAddRecipeToPlan` — only ever called with a known target plan in
 * this app so far, but keeping it optional matches that hook's shape.
 *
 * The remove button stays disabled while a row is optimistic-only
 * (`PlanDetailPage.tsx` `isPendingOnly`, unchanged by this) — not because
 * the DELETE needs a real id anymore (it targets `recipeId`, which is
 * known immediately), but because a DELETE racing ahead of the still-in-
 * flight POST could land first and be a no-op (recipe not in the plan
 * yet), only for the POST to then add it back — leaving the recipe
 * stuck in the plan despite the user's remove tap.
 */
export function useRemoveRecipeFromPlan(boundPlanId?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationKey: boundPlanId ? planKey(boundPlanId) : undefined,
    mutationFn: ({ planId, recipeId }: RemoveRecipeFromPlanInput) => removeRecipeFromPlan(planId, recipeId),
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

      // recipeCount is "distinct recipes" — removing every occurrence of
      // one recipeId is always exactly one fewer, regardless of how many
      // occurrences existed. This is only reachable from a row the user
      // can already see rendered (PlanDetailPage's flattened list, which
      // requires `plan` to be loaded), so the plan is always in cache.
      queryClient.setQueryData<MealPlanResponse[]>(plansKey, (current) =>
        current
          ? current.map((plan) => (plan.id === planId ? { ...plan, recipeCount: Math.max(0, plan.recipeCount - 1) } : plan))
          : current,
      );

      return { removedEntries };
    },
    onError: (_error, { planId }, context) => {
      const removedEntries = context?.removedEntries;
      if (removedEntries?.length) {
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
      }
      queryClient.setQueryData<MealPlanResponse[]>(plansKey, (current) =>
        current ? current.map((plan) => (plan.id === planId ? { ...plan, recipeCount: plan.recipeCount + 1 } : plan)) : current,
      );
    },
    onSettled: (_data, _error, { planId }) => {
      void queryClient.invalidateQueries({ queryKey: planKey(planId) });
      void queryClient.invalidateQueries({ queryKey: shoppingKey(planId) });
      // Reconciles recipeCount for real in case the optimistic write above
      // raced with something else.
      void queryClient.invalidateQueries({ queryKey: plansKey });
    },
  });
}

/** Server-confirmed: atomic server-side copy (days + recipes; not purchases/manual items). Omit `name` for the server's default ("<source> copy"). */
export function useDuplicatePlan() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ planId, name }: { planId: string; name?: string }) => duplicateMealPlan(planId, name),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: plansKey });
    },
  });
}

/**
 * The plan's share link token. Deliberately NOT fetched just by mounting
 * — `enabled: false`, with the caller triggering the actual fetch via
 * `refetch()` when the Share section's button is tapped. This keeps the
 * (first-call-creates-it) request from firing on every plan view.
 */
export function useShareLink(planId: string | undefined) {
  return useQuery({
    queryKey: shareKey(planId ?? ''),
    queryFn: () => getShareToken(planId!),
    enabled: false,
  });
}

/** Accepting a share link — idempotent server-side. Invalidates the plans list so the newly joined (or already-owned) plan shows up. */
export function useAcceptInvite() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (token: string) => acceptInvite(token),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: plansKey });
    },
  });
}

/** A plan's members (owner first, then members by join time). Shown as soon as the edit sheet opens — unlike the share link, not deferred behind a tap. */
export function usePlanMembers(planId: string | undefined) {
  return useQuery({
    queryKey: membersKey(planId ?? ''),
    queryFn: () => getMembers(planId!),
    enabled: !!planId,
  });
}

export interface RemoveMemberInput {
  userId: string;
  /** True for a self-removal ("leave") — only changes the generic-failure toast's wording. */
  isSelf?: boolean;
}

/**
 * Removes a member — used both for the owner removing someone else and
 * for a member removing themselves ("leave"); callers tell those apart
 * via `isSelf` (for this hook's own error copy) and their own onSuccess
 * (e.g. leave navigates away and clears the selected plan id,
 * remove-by-owner just closes its inline confirm). Optimistic only for
 * the members list itself, per the sharing contract — memberCount on the
 * plan/plans caches is left to settle-time invalidation rather than
 * guessed at here. Rollback is targeted: only this call's own removed
 * member is spliced back into the CURRENT cache, never a whole-list
 * snapshot (which would also undo a different member's removal that
 * already succeeded).
 */
export function useRemoveMember(planId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ userId }: RemoveMemberInput) => removeMember(planId, userId),
    onMutate: async ({ userId }) => {
      await queryClient.cancelQueries({ queryKey: membersKey(planId) });
      const previous = queryClient.getQueryData<PlanMemberResponse[]>(membersKey(planId));
      const removedMember = previous?.find((member) => member.userId === userId) ?? null;
      queryClient.setQueryData<PlanMemberResponse[]>(membersKey(planId), (current) =>
        current ? current.filter((member) => member.userId !== userId) : current,
      );
      return { removedMember };
    },
    onError: (error, { isSelf }, context) => {
      const removedMember = context?.removedMember;
      if (removedMember) {
        queryClient.setQueryData<PlanMemberResponse[]>(membersKey(planId), (current) => {
          if (!current) return current;
          if (current.some((member) => member.userId === removedMember.userId)) return current; // already back
          return [...current, removedMember].sort((a, b) => (a.joinedAt < b.joinedAt ? -1 : 1));
        });
      }
      // A 400 means the target can't be removed (the owner, or a legacy
      // trip's owner) — worth its own message rather than the server's.
      // Otherwise leave and remove get their own wording.
      const message =
        error instanceof ApiError && error.status === 400
          ? "This person can't be removed"
          : isSelf
            ? "Couldn't leave this plan."
            : "Couldn't remove that person.";
      toast.error(message);
    },
    meta: { suppressErrorToast: true },
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: membersKey(planId) });
      void queryClient.invalidateQueries({ queryKey: planKey(planId) });
      void queryClient.invalidateQueries({ queryKey: plansKey });
    },
  });
}

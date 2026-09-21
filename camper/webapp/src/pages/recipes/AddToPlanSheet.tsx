import { useMemo, useRef, useState } from 'react';
import { useLocation, useParams } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { Badge, Button, Skeleton, Text } from '@radix-ui/themes';
import { CheckIcon, PlusIcon } from '@radix-ui/react-icons';
import { Sheet } from '../../components/Sheet';
import { useSheet } from '../../components/useSheet';
import { QueryErrorState } from '../../components/QueryErrorState';
import { planKey, useAddRecipeToPlan, useCreatePlan, usePlans } from '../../queries/plans';
import type { MealPlanDetailResponse, MealPlanResponse } from '../../api/mealPlans';
import { recipeIdsInPlan } from '../../lib/flatPlan';
import { DEFAULT_PLAN_SERVINGS, todaysDateLabel } from '../../lib/planDefaults';
import { getSelectedPlanId, setSelectedPlanId } from '../../lib/selectedPlan';
import { router } from '../../router';
import { planShareMeta } from '../../lib/planShareMeta';
import { toast } from '../../lib/toastStore';
import './AddToPlanSheet.css';

export function AddToPlanSheet() {
  const { recipeId } = useParams<{ recipeId: string }>();
  // Opened either from the recipe's own page or directly from the library
  // (`/recipes/add-to-plan/:recipeId`), so the parent to fall back to differs.
  const location = useLocation();
  const fromLibrary = location.pathname.startsWith('/recipes/add-to-plan/');
  const sheet = useSheet(fromLibrary ? `/recipes${location.search}` : `/recipes/${recipeId}`);
  const queryClient = useQueryClient();

  const { data: plans, isLoading, isError, refetch } = usePlans();
  // Whether there's already data to show — used below so a background
  // refetch error (window focus) falls through to the normal render
  // instead of blanking an already-loaded list.
  const hasData = !!plans;
  const addRecipe = useAddRecipeToPlan();
  const createPlan = useCreatePlan();

  // One shared mutation instance is used for every row (the target plan
  // varies per tap), so its own isPending/variables only ever reflect the
  // single most recent call — not enough to disable just the tapped row
  // while others stay tappable. Tracked here instead, scoped by plan id.
  const [pendingPlanIds, setPendingPlanIds] = useState<Set<string>>(new Set());

  // Currently selected plan first, so the common case (add to the plan
  // I'm already shopping/cooking for) is a single tap.
  const selectedPlanId = getSelectedPlanId();
  const orderedPlans = useMemo(() => {
    if (!plans) return [];
    if (!selectedPlanId) return plans;
    const selected = plans.filter((plan) => plan.id === selectedPlanId);
    const rest = plans.filter((plan) => plan.id !== selectedPlanId);
    return [...selected, ...rest];
  }, [plans, selectedPlanId]);

  // Best-effort: only reflects plans whose detail happens to already be
  // cached (e.g. previously viewed) — not worth a fetch per row just to
  // show a checkmark.
  function isAlreadyAdded(planId: string): boolean {
    if (!recipeId) return false;
    const detail = queryClient.getQueryData<MealPlanDetailResponse>(planKey(planId));
    if (!detail) return false;
    return recipeIdsInPlan(detail).has(recipeId);
  }

  function handleAdd(plan: MealPlanResponse) {
    if (!recipeId) return;
    setPendingPlanIds((current) => new Set(current).add(plan.id));
    addRecipe.mutate(
      { planId: plan.id, recipeId },
      {
        onSuccess: (result) => {
          toast.info(result.alreadyInPlan ? `Already in ${plan.name}` : `Added to ${plan.name}`);
          sheet.close();
        },
        onSettled: () => {
          setPendingPlanIds((current) => {
            const next = new Set(current);
            next.delete(plan.id);
            return next;
          });
        },
      },
    );
  }

  // No plans yet: make the first one here, with the same defaults as the New
  // plan sheet, rather than sending a newcomer off to another tab and back.
  const creatingRef = useRef(false);
  async function handleCreateAndAdd() {
    // A ref, not the button's loading state: a second tap can land before that re-render.
    if (!recipeId || creatingRef.current) return;
    creatingRef.current = true;
    try {
      const plan = await createPlan.mutateAsync({ name: todaysDateLabel(), servings: DEFAULT_PLAN_SERVINGS });
      setSelectedPlanId(plan.id);
      await addRecipe.mutateAsync({ planId: plan.id, recipeId });
      toast.info(`Added to your new plan, ${plan.name}`, {
        label: 'Open',
        onClick: () => void router.navigate(`/plans/${plan.id}`),
      });
      sheet.close();
    } catch {
      // The global mutation error toast already said what went wrong; stay on the sheet to retry.
    } finally {
      creatingRef.current = false;
    }
  }

  return (
    <Sheet {...sheet.sheetProps} title="Add to plan">
      {isLoading && (
        <div aria-busy="true" aria-label="Loading plans">
          <Skeleton height="48px" aria-hidden="true" />
        </div>
      )}

      {!isLoading && isError && !hasData && (
        <QueryErrorState message="Couldn't load your plans." onRetry={() => void refetch()} />
      )}

      {!isLoading && (hasData || !isError) && orderedPlans.length === 0 && (
        <div className="add-to-plan-sheet__empty">
          <Text as="p" color="gray" size="2">
            You don&apos;t have any plans yet.
          </Text>
          <Button
            size="3"
            loading={createPlan.isPending || addRecipe.isPending}
            onClick={() => void handleCreateAndAdd()}
          >
            <PlusIcon /> Create a plan and add this recipe
          </Button>
        </div>
      )}

      {!isLoading && (hasData || !isError) && orderedPlans.length > 0 && (
        <div className="add-to-plan-sheet__list">
          {orderedPlans.map((plan) => {
            const added = isAlreadyAdded(plan.id);
            const { shared } = planShareMeta(plan);
            return (
              <button
                key={plan.id}
                type="button"
                className="add-to-plan-sheet__row"
                disabled={added || pendingPlanIds.has(plan.id)}
                onClick={() => handleAdd(plan)}
              >
                <span className="add-to-plan-sheet__row-name">{plan.name}</span>
                <span className="add-to-plan-sheet__row-badges">
                  {shared && (
                    <Badge variant="soft" color="gray" size="1">
                      Shared
                    </Badge>
                  )}
                  {added && (
                    <Badge color="green" variant="soft">
                      <CheckIcon /> Added
                    </Badge>
                  )}
                </span>
              </button>
            );
          })}
        </div>
      )}
    </Sheet>
  );
}

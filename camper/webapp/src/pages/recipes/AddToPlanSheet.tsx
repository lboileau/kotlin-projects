import { useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { Badge, Skeleton, Text } from '@radix-ui/themes';
import { CheckIcon } from '@radix-ui/react-icons';
import { Sheet } from '../../components/Sheet';
import { useCloseSheet } from '../../components/useCloseSheet';
import { planKey, useAddRecipeToPlan, usePlans } from '../../queries/plans';
import type { MealPlanDetailResponse, MealPlanResponse } from '../../api/mealPlans';
import { recipeIdsInPlan } from '../../lib/flatPlan';
import { getSelectedPlanId } from '../../lib/selectedPlan';
import { toast } from '../../lib/toastStore';
import './AddToPlanSheet.css';

export function AddToPlanSheet() {
  const { recipeId } = useParams<{ recipeId: string }>();
  const closeSheet = useCloseSheet(`/recipes/${recipeId}`);
  const queryClient = useQueryClient();

  const { data: plans, isLoading } = usePlans();
  const addRecipe = useAddRecipeToPlan();

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
          closeSheet();
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

  return (
    <Sheet title="Add to plan" onClose={closeSheet}>
      {isLoading && <Skeleton height="48px" />}

      {!isLoading && orderedPlans.length === 0 && (
        <Text color="gray" size="2" className="add-to-plan-sheet__empty">
          You don&apos;t have any plans yet — create one from the Plans tab first.
        </Text>
      )}

      {!isLoading && orderedPlans.length > 0 && (
        <div className="add-to-plan-sheet__list">
          {orderedPlans.map((plan) => {
            const added = isAlreadyAdded(plan.id);
            return (
              <button
                key={plan.id}
                type="button"
                className="add-to-plan-sheet__row"
                disabled={added || pendingPlanIds.has(plan.id)}
                onClick={() => handleAdd(plan)}
              >
                <span>{plan.name}</span>
                {added && (
                  <Badge color="green" variant="soft">
                    <CheckIcon /> Added
                  </Badge>
                )}
              </button>
            );
          })}
        </div>
      )}
    </Sheet>
  );
}

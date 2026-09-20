import { useEffect, useRef, useState } from 'react';
import { Link, Outlet, useParams } from 'react-router-dom';
import { Badge, Button, IconButton, Separator, Skeleton, Text } from '@radix-ui/themes';
import { Pencil2Icon, PlusIcon, TrashIcon } from '@radix-ui/react-icons';
import { PageHeader } from '../../components/PageHeader';
import { SheetLink } from '../../components/SheetLink';
import { Stepper } from '../../components/Stepper';
import { QueryErrorState } from '../../components/QueryErrorState';
import { ApiError } from '../../api/http';
import { usePlan, useAddRecipeToPlan, useRemoveRecipeFromPlan, useUpdatePlan } from '../../queries/plans';
import { flattenMealPlan, type FlatPlanRecipe } from '../../lib/flatPlan';
import { clearSelectedPlanId, getSelectedPlanId, setSelectedPlanId } from '../../lib/selectedPlan';
import { toast } from '../../lib/toastStore';
import { useMealPlanSync } from '../../sync/useMealPlanSync';
import './PlanDetailPage.css';

export function PlanDetailPage() {
  const { planId } = useParams<{ planId: string }>();
  useMealPlanSync(planId);
  const { data: plan, isLoading, isError, error, refetch } = usePlan(planId);
  const updatePlan = useUpdatePlan(planId ?? '');
  const addRecipe = useAddRecipeToPlan(planId);
  const removeRecipe = useRemoveRecipeFromPlan(planId);

  const notFound = isError && error instanceof ApiError && error.status === 404;

  // No effect needed to seed this from `plan`: until the stepper is
  // touched this session, the displayed value just falls through to the
  // server's — once touched, the override takes over.
  const [servingsOverride, setServingsOverride] = useState<number | null>(null);
  const servings = servingsOverride ?? plan?.servings ?? 1;
  const debounceRef = useRef<number | undefined>(undefined);
  // The value queued behind the debounce timer, or null once it's been
  // sent — lets both the unmount flush and the settle handler below know
  // whether a newer change is still waiting.
  const pendingServingsRef = useRef<number | null>(null);

  useEffect(() => {
    if (planId) setSelectedPlanId(planId);
  }, [planId]);

  useEffect(() => {
    if (notFound && planId && getSelectedPlanId() === planId) {
      clearSelectedPlanId();
    }
  }, [notFound, planId]);

  useEffect(() => {
    return () => {
      // A change made <400ms before navigating away would otherwise be
      // silently dropped — flush it instead of just clearing the timer.
      if (debounceRef.current !== undefined) {
        window.clearTimeout(debounceRef.current);
        const pending = pendingServingsRef.current;
        if (pending !== null) updatePlan.mutate({ servings: pending });
      }
    };
    // Runs once, mount/unmount only: flushing must not fire early just
    // because `updatePlan` gets a new object identity on some unrelated
    // re-render while a debounce is still pending.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function handleServingsChange(next: number) {
    setServingsOverride(next);
    pendingServingsRef.current = next;
    if (debounceRef.current) window.clearTimeout(debounceRef.current);
    debounceRef.current = window.setTimeout(() => {
      debounceRef.current = undefined;
      pendingServingsRef.current = null;
      updatePlan.mutate(
        { servings: next },
        {
          onSettled: () => {
            // Only clear once nothing newer is queued — otherwise the
            // display would flash back to this just-settled value right
            // before the next debounced change lands.
            if (pendingServingsRef.current === null) setServingsOverride(null);
          },
        },
      );
    }, 400);
  }

  function isPendingOnly(recipe: FlatPlanRecipe): boolean {
    return recipe.mealPlanRecipeIds.every((id) => id.startsWith('temp-'));
  }

  function handleRemove(recipe: FlatPlanRecipe) {
    if (!planId) return;
    removeRecipe.mutate(
      { planId, recipeId: recipe.recipeId },
      {
        onSuccess: () => {
          toast.info(`Removed ${recipe.recipeName}`, {
            label: 'Undo',
            onClick: () => {
              addRecipe.mutate({
                planId,
                recipeId: recipe.recipeId,
                recipeName: recipe.recipeName,
                recipeWebLink: recipe.recipeWebLink,
                baseServings: recipe.baseServings,
              });
            },
          });
        },
      },
    );
  }

  if (notFound) {
    return (
      <div className="plan-detail-page">
        <PageHeader title="Plan" backTo="/plans" />
        <div className="plan-detail-page__not-found">
          <Text size="4" weight="medium">
            Plan not found
          </Text>
          <Text color="gray" size="2">
            It may have been deleted, or the link is wrong.
          </Text>
          <Button asChild size="3" variant="solid">
            <Link to="/plans">Back to Plans</Link>
          </Button>
        </div>
        <Outlet />
      </div>
    );
  }

  // Gated on the absence of data: this page is refetched by live sync, so
  // a background refetch error (data already loaded) must not blank an
  // already-rendered plan — only a failure with nothing to show yet does.
  if (isError && !plan) {
    return (
      <div className="plan-detail-page">
        <PageHeader title="Plan" backTo="/plans" />
        <QueryErrorState message="Couldn't load this plan." onRetry={() => void refetch()} />
        <Outlet />
      </div>
    );
  }

  if (isLoading || !plan) {
    return (
      <div className="plan-detail-page">
        <PageHeader title="Plan" backTo="/plans" />
        <div className="plan-detail-page__skeleton" aria-busy="true" aria-label="Loading plan">
          <Skeleton height="40px" aria-hidden="true" />
          <Skeleton height="60px" aria-hidden="true" />
          <Skeleton height="60px" aria-hidden="true" />
        </div>
        <Outlet />
      </div>
    );
  }

  const recipes = flattenMealPlan(plan);

  return (
    <div className="plan-detail-page">
      <PageHeader
        title={plan.name}
        backTo="/plans"
        actions={
          <SheetLink to="edit" className="plan-detail-page__edit" aria-label="Edit plan">
            <Pencil2Icon />
          </SheetLink>
        }
      />

      <div className="plan-detail-page__body">
        <div className="plan-detail-page__servings-row">
          <Text as="span" size="2" weight="medium">
            Servings
          </Text>
          <Stepper value={servings} onChange={handleServingsChange} min={1} ariaLabel="Servings" />
        </div>

        <Separator size="4" />

        <div>
          <div className="plan-detail-page__section-header">
            <Text as="p" size="3" weight="medium">
              Recipes
            </Text>
            <Button asChild size="2" variant="soft">
              <SheetLink to="add">
                <PlusIcon /> Add recipes
              </SheetLink>
            </Button>
          </div>

          {recipes.length === 0 ? (
            <div className="plan-detail-page__empty-recipes">
              <Text color="gray" size="2">
                No recipes yet — add some to start building the shopping list.
              </Text>
            </div>
          ) : (
            <div className="plan-detail-page__recipes">
              {recipes.map((recipe) => (
                <div key={recipe.recipeId} className="plan-detail-page__recipe-row">
                  <Link to={`/recipes/${recipe.recipeId}`} className="plan-detail-page__recipe-link">
                    <span className="plan-detail-page__recipe-name">{recipe.recipeName}</span>
                    <Badge variant="soft" size="1" className="plan-detail-page__recipe-badge">
                      serves {recipe.baseServings}
                    </Badge>
                  </Link>
                  <IconButton
                    type="button"
                    variant="ghost"
                    color="red"
                    size="3"
                    aria-label={`Remove ${recipe.recipeName}`}
                    disabled={isPendingOnly(recipe)}
                    onClick={() => handleRemove(recipe)}
                  >
                    <TrashIcon />
                  </IconButton>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>


      <Outlet />
    </div>
  );
}

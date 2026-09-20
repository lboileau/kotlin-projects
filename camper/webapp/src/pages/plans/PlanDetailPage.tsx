import { useEffect, useRef, useState } from 'react';
import { Link, Outlet, useParams } from 'react-router-dom';
import { Badge, Button, IconButton, Separator, Text, TextField } from '@radix-ui/themes';
import { ListBulletIcon, Pencil2Icon, PlusIcon, Share1Icon, TrashIcon } from '@radix-ui/react-icons';
import { BottomBar } from '../../components/BottomBar';
import { PageLoader } from '../../components/PageLoader';
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
import { useSharePlan } from './useSharePlan';
import './PlanDetailPage.css';

export function PlanDetailPage() {
  const { planId } = useParams<{ planId: string }>();
  useMealPlanSync(planId);
  const { data: plan, isLoading, isError, error, refetch } = usePlan(planId);
  const updatePlan = useUpdatePlan(planId ?? '');
  const sharePlan = useSharePlan(planId, plan?.name);
  const addRecipe = useAddRecipeToPlan(planId);
  const removeRecipe = useRemoveRecipeFromPlan(planId);

  const notFound = isError && error instanceof ApiError && error.status === 404;
  // Checked the same way as `notFound` — before the "is there stale data
  // to keep showing" branch below — so a background refetch that comes
  // back 403 (e.g. the owner removed this user while the page was open)
  // wins over whatever was cached, the same way a 404 does.
  const forbidden = isError && error instanceof ApiError && error.status === 403;

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
    if ((notFound || forbidden) && planId && getSelectedPlanId() === planId) {
      clearSelectedPlanId();
    }
  }, [notFound, forbidden, planId]);

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

  if (forbidden) {
    return (
      <div className="plan-detail-page">
        <PageHeader title="Plan" backTo="/plans" />
        <div className="plan-detail-page__not-found">
          <Text size="4" weight="medium">
            You don&apos;t have access to this plan
          </Text>
          <Text color="gray" size="2">
            It may have been shared with a different account, or you were removed.
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
        <PageLoader area="plans" label="Loading plan" />
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
          <>
            <button
              type="button"
              className="header-icon-button"
              aria-label="Share plan"
              disabled={sharePlan.isSharing}
              onClick={() => void sharePlan.share()}
            >
              <Share1Icon />
            </button>
            <SheetLink to="edit" className="header-icon-button" aria-label="Edit plan">
              <Pencil2Icon />
            </SheetLink>
          </>
        }
      />

      {sharePlan.fallbackUrl && (
        <div className="plan-detail-page__share-fallback">
          <Text size="2" color="gray">
            Copy this link to share the plan:
          </Text>
          <TextField.Root
            value={sharePlan.fallbackUrl}
            readOnly
            size="3"
            aria-label="Share link"
            onFocus={(event) => event.currentTarget.select()}
          />
        </div>
      )}

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

      <BottomBar>
        <Button asChild size="3" variant="solid" className="plan-detail-page__shopping-button">
          <Link to={`/plans/${planId}/shopping`}>
            <ListBulletIcon /> Shopping list
          </Link>
        </Button>
      </BottomBar>

      <Outlet />
    </div>
  );
}

import { Link, Navigate, useNavigate } from 'react-router-dom';
import { Badge, Button, Skeleton, Text } from '@radix-ui/themes';
import { ChevronRightIcon } from '@radix-ui/react-icons';
import { PageHeader } from '../../components/PageHeader';
import { QueryErrorState } from '../../components/QueryErrorState';
import { usePlans } from '../../queries/plans';
import { getSelectedPlanId, setSelectedPlanId } from '../../lib/selectedPlan';
import { planShareMeta } from '../../lib/planShareMeta';
import './ShoppingRedirect.css';

/**
 * /shopping — the last-selected plan's shopping list. With no plan selected
 * yet, it lists the user's plans so one tap opens that plan's list.
 */
export function ShoppingRedirect() {
  const planId = getSelectedPlanId();

  if (planId) {
    return <Navigate to={`/plans/${planId}/shopping`} replace />;
  }

  return <ChoosePlan />;
}

function ChoosePlan() {
  const navigate = useNavigate();
  const { data: plans, isLoading, isError, refetch } = usePlans();
  const hasPlans = !!plans && plans.length > 0;

  function handleSelect(id: string) {
    setSelectedPlanId(id);
    navigate(`/plans/${id}/shopping`, { replace: true });
  }

  return (
    <div className="shopping-redirect">
      <PageHeader title="Shopping" />

      {isLoading && (
        <div className="shopping-redirect__list" aria-busy="true" aria-label="Loading plans">
          <Skeleton height="64px" aria-hidden="true" />
          <Skeleton height="64px" aria-hidden="true" />
        </div>
      )}

      {isError && !plans && <QueryErrorState message="Couldn't load your plans." onRetry={() => void refetch()} />}

      {hasPlans && (
        <div className="shopping-redirect__list">
          <Text color="gray" size="2" as="p" className="shopping-redirect__hint">
            Choose a plan to shop for.
          </Text>
          {plans.map((plan) => {
            const { shared, metaText } = planShareMeta(plan);
            const recipeCountLabel =
              plan.recipeCount === 0 ? 'No recipes yet' : `${plan.recipeCount} ${plan.recipeCount === 1 ? 'recipe' : 'recipes'}`;
            return (
              <button key={plan.id} type="button" className="shopping-redirect__row" onClick={() => handleSelect(plan.id)}>
                <span className="shopping-redirect__row-main">
                  <span className="shopping-redirect__row-name-line">
                    <span className="shopping-redirect__row-name">{plan.name}</span>
                    {shared && (
                      <Badge variant="soft" color="gray" size="1">
                        Shared
                      </Badge>
                    )}
                  </span>
                  <Text as="span" size="2" color="gray">
                    {metaText ? `${recipeCountLabel} · ${metaText}` : recipeCountLabel}
                  </Text>
                </span>
                <ChevronRightIcon aria-hidden="true" />
              </button>
            );
          })}
        </div>
      )}

      {plans && !hasPlans && (
        <div className="shopping-redirect__body">
          <Text color="gray" size="2">
            Create a plan first, then its shopping list shows up here.
          </Text>
          <Button asChild size="3">
            <Link to="/plans/new">New plan</Link>
          </Button>
        </div>
      )}
    </div>
  );
}

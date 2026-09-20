import { Outlet, Link } from 'react-router-dom';
import { Badge, Button, Heading, Skeleton, Text } from '@radix-ui/themes';
import { ChevronRightIcon, PlusIcon } from '@radix-ui/react-icons';
import { PageHeader } from '../../components/PageHeader';
import { SheetLink } from '../../components/SheetLink';
import { QueryErrorState } from '../../components/QueryErrorState';
import { BottomBar } from '../../components/BottomBar';
import { usePlans } from '../../queries/plans';
import { formatRelativeTime } from '../../lib/relativeTime';
import './PlansPage.css';

function recipeCountLabel(recipeCount: number): string {
  if (recipeCount === 0) return 'No recipes yet';
  return `${recipeCount} ${recipeCount === 1 ? 'recipe' : 'recipes'}`;
}

export function PlansPage() {
  const { data: plans, isLoading, isError, refetch } = usePlans();
  const hasPlans = !!plans && plans.length > 0;
  // Whether there's already data to show — used below so a background
  // refetch error (window focus) falls through to the normal render
  // instead of blanking an already-loaded list.
  const hasData = !!plans;

  return (
    <div className="plans-page">
      <PageHeader title="Plans" />
      <div className="plans-page__body">
        {isLoading && (
          <div aria-busy="true" aria-label="Loading plans">
            <Skeleton className="plans-page__skeleton-row" aria-hidden="true" />
            <Skeleton className="plans-page__skeleton-row" aria-hidden="true" />
            <Skeleton className="plans-page__skeleton-row" aria-hidden="true" />
          </div>
        )}

        {!isLoading && isError && !hasData && (
          <QueryErrorState message="Couldn't load your plans." onRetry={() => void refetch()} />
        )}

        {!isLoading && (hasData || !isError) && !hasPlans && (
          <div className="plans-page__empty">
            <Heading as="h2" size="4">
              No plans yet
            </Heading>
            <Text color="gray" size="2">
              Create a plan to start adding recipes and building a shopping list.
            </Text>
            <Button asChild size="3" variant="solid">
              <SheetLink to="new">
                <PlusIcon /> New plan
              </SheetLink>
            </Button>
          </div>
        )}

        {!isLoading &&
          (hasData || !isError) &&
          hasPlans &&
          plans.map((plan) => (
            <Link key={plan.id} to={`/plans/${plan.id}`} className="plans-page__row">
              <div className="plans-page__row-main">
                <div className="plans-page__row-name">{plan.name}</div>
                <div className="plans-page__row-meta">
                  <Badge variant="soft">
                    {plan.servings} {plan.servings === 1 ? 'serving' : 'servings'}
                  </Badge>
                  <Text size="1" color="gray">
                    {recipeCountLabel(plan.recipeCount)}
                  </Text>
                  <Text size="1" color="gray">
                    Updated {formatRelativeTime(plan.updatedAt)}
                  </Text>
                </div>
              </div>
              <ChevronRightIcon color="var(--gray-9)" />
            </Link>
          ))}
      </div>

      {hasPlans && (
        <BottomBar>
          <Button asChild size="3" variant="solid" className="plans-page__new-plan">
            <SheetLink to="new">
              <PlusIcon /> New plan
            </SheetLink>
          </Button>
        </BottomBar>
      )}

      <Outlet />
    </div>
  );
}

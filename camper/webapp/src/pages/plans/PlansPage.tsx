import { Outlet, Link } from 'react-router-dom';
import { Badge, Button, Heading, Skeleton, Text } from '@radix-ui/themes';
import { ChevronRightIcon, PlusIcon } from '@radix-ui/react-icons';
import { PageHeader } from '../../components/PageHeader';
import { SheetLink } from '../../components/SheetLink';
import { usePlans } from '../../queries/plans';
import { formatRelativeTime } from '../../lib/relativeTime';
import './PlansPage.css';

export function PlansPage() {
  const { data: plans, isLoading } = usePlans();
  const hasPlans = !!plans && plans.length > 0;

  return (
    <div className="plans-page">
      <PageHeader title="Plans" />
      <div className="plans-page__body">
        {isLoading && (
          <>
            <Skeleton className="plans-page__skeleton-row" />
            <Skeleton className="plans-page__skeleton-row" />
            <Skeleton className="plans-page__skeleton-row" />
          </>
        )}

        {!isLoading && !hasPlans && (
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
          hasPlans &&
          plans.map((plan) => (
            <Link key={plan.id} to={`/plans/${plan.id}`} className="plans-page__row">
              <div>
                <div className="plans-page__row-name">{plan.name}</div>
                <div className="plans-page__row-meta">
                  <Badge variant="soft">
                    {plan.servings} {plan.servings === 1 ? 'serving' : 'servings'}
                  </Badge>
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
        <div className="plans-page__fab-region">
          <Button asChild size="4" variant="solid" radius="full" className="plans-page__fab">
            <SheetLink to="new" aria-label="New plan">
              <PlusIcon /> New plan
            </SheetLink>
          </Button>
        </div>
      )}

      <Outlet />
    </div>
  );
}

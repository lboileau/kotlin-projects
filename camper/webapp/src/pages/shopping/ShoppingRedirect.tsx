import { Link, Navigate } from 'react-router-dom';
import { Button, Text } from '@radix-ui/themes';
import { PageHeader } from '../../components/PageHeader';
import { getSelectedPlanId } from '../../lib/selectedPlan';
import './ShoppingRedirect.css';

/** /shopping — the last-selected plan's shopping list, or an empty state pointing to Plans. */
export function ShoppingRedirect() {
  const planId = getSelectedPlanId();

  if (planId) {
    return <Navigate to={`/plans/${planId}/shopping`} replace />;
  }

  return (
    <div className="shopping-redirect">
      <PageHeader title="Shopping" />
      <div className="shopping-redirect__body">
        <Text color="gray" size="2">
          Pick a plan to see its shopping list.
        </Text>
        <Button asChild size="3">
          <Link to="/plans">Go to Plans</Link>
        </Button>
      </div>
    </div>
  );
}

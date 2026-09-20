import { useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Badge, Skeleton, Text } from '@radix-ui/themes';
import { CheckIcon } from '@radix-ui/react-icons';
import { Sheet } from '../../components/Sheet';
import { useSheet } from '../../components/useSheet';
import { QueryErrorState } from '../../components/QueryErrorState';
import { usePlans } from '../../queries/plans';
import { setSelectedPlanId } from '../../lib/selectedPlan';
import { planShareMeta } from '../../lib/planShareMeta';
import './SwitchPlanSheet.css';

export function SwitchPlanSheet() {
  const { planId } = useParams<{ planId: string }>();
  const sheet = useSheet(`/plans/${planId}/shopping`);
  const navigate = useNavigate();
  const { data: plans, isLoading, isError, refetch } = usePlans();
  // Whether there's already data to show — used below so a background
  // refetch error (window focus) falls through to the normal render
  // instead of blanking an already-loaded list.
  const hasData = !!plans;

  // This sheet doesn't fetch the plan it's switching away from — it
  // reuses the already-fetched `mine` list, which is the cheapest signal
  // for "is that plan still accessible" (both deleted and lost-access
  // drop it from this list once invalidated). If it's gone, the parent
  // ShoppingPage's own 403/404 already shows the right state — this just
  // makes sure the sheet on top of it doesn't linger.
  useEffect(() => {
    if (plans && planId && !plans.some((plan) => plan.id === planId)) {
      sheet.close();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [plans, planId]);

  function handleSelect(id: string) {
    setSelectedPlanId(id);
    navigate(`/plans/${id}/shopping`, { replace: true });
  }

  return (
    <Sheet {...sheet.sheetProps} title="Switch plan">
      {isLoading && (
        <div aria-busy="true" aria-label="Loading plans">
          <Skeleton height="48px" aria-hidden="true" />
        </div>
      )}

      {!isLoading && isError && !hasData && (
        <QueryErrorState message="Couldn't load your plans." onRetry={() => void refetch()} />
      )}

      {!isLoading && (hasData || !isError) && (!plans || plans.length === 0) && (
        <Text color="gray" size="2">
          You don&apos;t have any plans yet.
        </Text>
      )}

      {!isLoading && (hasData || !isError) && plans && plans.length > 0 && (
        <div className="switch-plan-sheet__list">
          {plans.map((plan) => {
            const current = plan.id === planId;
            const { shared } = planShareMeta(plan);
            return (
              <button
                key={plan.id}
                type="button"
                className="switch-plan-sheet__row"
                disabled={current}
                onClick={() => handleSelect(plan.id)}
              >
                <span className="switch-plan-sheet__row-name">{plan.name}</span>
                <span className="switch-plan-sheet__row-badges">
                  {shared && (
                    <Badge variant="soft" color="gray" size="1">
                      Shared
                    </Badge>
                  )}
                  {current && (
                    <Badge variant="soft">
                      <CheckIcon /> Current
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

import { useNavigate, useParams } from 'react-router-dom';
import { Badge, Skeleton, Text } from '@radix-ui/themes';
import { CheckIcon } from '@radix-ui/react-icons';
import { Sheet } from '../../components/Sheet';
import { useSheet } from '../../components/useSheet';
import { QueryErrorState } from '../../components/QueryErrorState';
import { usePlans } from '../../queries/plans';
import { setSelectedPlanId } from '../../lib/selectedPlan';
import './SwitchPlanSheet.css';

export function SwitchPlanSheet() {
  const { planId } = useParams<{ planId: string }>();
  const sheet = useSheet(`/plans/${planId}/shopping`);
  const navigate = useNavigate();
  const { data: plans, isLoading, isError, refetch } = usePlans();

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

      {!isLoading && isError && (
        <QueryErrorState message="Couldn't load your plans." onRetry={() => void refetch()} />
      )}

      {!isLoading && !isError && (!plans || plans.length === 0) && (
        <Text color="gray" size="2">
          You don&apos;t have any plans yet.
        </Text>
      )}

      {!isLoading && !isError && plans && plans.length > 0 && (
        <div className="switch-plan-sheet__list">
          {plans.map((plan) => {
            const current = plan.id === planId;
            return (
              <button
                key={plan.id}
                type="button"
                className="switch-plan-sheet__row"
                disabled={current}
                onClick={() => handleSelect(plan.id)}
              >
                <span className="switch-plan-sheet__row-name">{plan.name}</span>
                {current && (
                  <Badge variant="soft">
                    <CheckIcon /> Current
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

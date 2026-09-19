import { Outlet, useParams } from 'react-router-dom';
import { PlaceholderPage } from '../../components/Placeholder';

export function PlanDetailPage() {
  const { planId } = useParams<{ planId: string }>();
  return (
    <>
      <PlaceholderPage title={planId ? `Plan ${planId}` : 'Plan'} backTo="/plans" />
      <Outlet />
    </>
  );
}

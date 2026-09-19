import { useParams } from 'react-router-dom';
import { PlaceholderSheet } from '../../components/Placeholder';

export function SwitchPlanSheet() {
  const { planId } = useParams<{ planId: string }>();
  return <PlaceholderSheet title="Switch plan" parentPath={`/plans/${planId}/shopping`} />;
}

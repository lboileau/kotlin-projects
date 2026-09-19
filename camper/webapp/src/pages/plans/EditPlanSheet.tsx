import { useParams } from 'react-router-dom';
import { PlaceholderSheet } from '../../components/Placeholder';

export function EditPlanSheet() {
  const { planId } = useParams<{ planId: string }>();
  return <PlaceholderSheet title="Edit plan" parentPath={`/plans/${planId}`} />;
}

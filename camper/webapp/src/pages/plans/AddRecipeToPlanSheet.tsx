import { useParams } from 'react-router-dom';
import { PlaceholderSheet } from '../../components/Placeholder';

export function AddRecipeToPlanSheet() {
  const { planId } = useParams<{ planId: string }>();
  return <PlaceholderSheet title="Add recipes" parentPath={`/plans/${planId}`} fullHeight />;
}

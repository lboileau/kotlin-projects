import { useParams } from 'react-router-dom';
import { PlaceholderSheet } from '../../components/Placeholder';

export function AddToPlanSheet() {
  const { recipeId } = useParams<{ recipeId: string }>();
  return <PlaceholderSheet title="Add to plan" parentPath={`/recipes/${recipeId}`} />;
}

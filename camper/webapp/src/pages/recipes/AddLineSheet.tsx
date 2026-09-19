import { useParams } from 'react-router-dom';
import { PlaceholderSheet } from '../../components/Placeholder';

export function AddLineSheet() {
  const { recipeId } = useParams<{ recipeId: string }>();
  return <PlaceholderSheet title="Add ingredient" parentPath={`/recipes/${recipeId}`} />;
}

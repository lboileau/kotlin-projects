import { useParams } from 'react-router-dom';
import { PlaceholderPage } from '../../components/Placeholder';

export function EditRecipePage() {
  const { recipeId } = useParams<{ recipeId: string }>();
  return <PlaceholderPage title="Edit recipe" backTo={recipeId ? `/recipes/${recipeId}` : '/recipes'} />;
}

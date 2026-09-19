import { Outlet, useParams } from 'react-router-dom';
import { PlaceholderPage } from '../../components/Placeholder';

export function RecipeDetailPage() {
  const { recipeId } = useParams<{ recipeId: string }>();
  return (
    <>
      <PlaceholderPage title={recipeId ? `Recipe ${recipeId}` : 'Recipe'} backTo="/recipes" />
      <Outlet />
    </>
  );
}

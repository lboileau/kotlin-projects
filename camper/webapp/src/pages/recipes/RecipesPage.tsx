import { Outlet } from 'react-router-dom';
import { PlaceholderPage } from '../../components/Placeholder';

export function RecipesPage() {
  return (
    <>
      <PlaceholderPage title="Recipes" />
      <Outlet />
    </>
  );
}

import { Outlet } from 'react-router-dom';
import { PlaceholderPage } from '../../components/Placeholder';

export function IngredientsPage() {
  return (
    <>
      <PlaceholderPage title="Ingredients" backTo="/recipes" />
      <Outlet />
    </>
  );
}

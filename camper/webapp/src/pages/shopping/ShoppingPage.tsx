import { Outlet } from 'react-router-dom';
import { PlaceholderPage } from '../../components/Placeholder';

export function ShoppingPage() {
  return (
    <>
      <PlaceholderPage title="Shopping" />
      <Outlet />
    </>
  );
}

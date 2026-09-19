import { Outlet } from 'react-router-dom';
import { PlaceholderPage } from '../../components/Placeholder';

export function PlansPage() {
  return (
    <>
      <PlaceholderPage title="Plans" />
      <Outlet />
    </>
  );
}

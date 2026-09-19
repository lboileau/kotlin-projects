import { Outlet } from 'react-router-dom';
import { TabBar } from './TabBar';
import './AppShell.css';

/**
 * Layout route for every guarded page: a scrollable content area plus a
 * pinned bottom tab bar. Desktop centers a max-width column; the tab bar
 * stays constrained to it. Sheets render over this via their own portal
 * (each page renders its own <Outlet/> for its child sheet routes).
 */
export function AppShell() {
  return (
    <div className="app-shell">
      <main className="app-shell__scroll">
        <Outlet />
      </main>
      <TabBar />
    </div>
  );
}

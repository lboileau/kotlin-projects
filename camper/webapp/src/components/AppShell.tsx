import { Outlet } from 'react-router-dom';
import { TabBar } from './TabBar';
import { SyncProvider } from '../sync/SyncProvider';
import './AppShell.css';

/**
 * Layout route for every guarded page: a scrollable content area plus a
 * pinned bottom tab bar. Desktop centers a max-width column; the tab bar
 * stays constrained to it. Sheets render over this via their own portal
 * (each page renders its own <Outlet/> for its child sheet routes).
 *
 * Wrapped in SyncProvider here (not higher up, e.g. main.tsx) so the one
 * app-wide STOMP client only exists while signed in — this component
 * only ever renders once RequireAuth has confirmed that.
 */
export function AppShell() {
  return (
    <SyncProvider>
      <div className="app-shell">
        <main className="app-shell__scroll">
          <Outlet />
        </main>
        <TabBar />
      </div>
    </SyncProvider>
  );
}

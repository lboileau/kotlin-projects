import { Suspense, useEffect, useRef } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { TabBar } from './TabBar';
import { RouteFallback } from './RouteFallback';
import { SyncProvider } from '../sync/SyncProvider';
import { prefetchOtherTabs } from '../routePrefetch';
import './AppShell.css';

/**
 * Layout route for every guarded page: a scrollable content area plus a
 * pinned bottom tab bar. Desktop centers a max-width column; the tab bar
 * stays constrained to it. Sheets render over this via their own portal
 * (each page renders its own <Outlet/> for its child sheet routes).
 *
 * Wrapped in SyncProvider here (not higher up, e.g. main.tsx) so the one
 * app-wide STOMP client only exists while signed in — this component
 * only ever renders once RequireAuth has confirmed that. The Suspense
 * boundary here is what every lazy route in router.tsx renders through.
 */
export function AppShell() {
  const location = useLocation();
  const mainRef = useRef<HTMLElement>(null);

  useEffect(() => {
    prefetchOtherTabs(location.pathname);
    // Only once, right after the shell first mounts — not on every
    // navigation, which would re-schedule the same idle prefetches
    // repeatedly as the user moves around.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    // For screen-reader users, a route change needs to move focus
    // somewhere sensible (an SPA navigation doesn't get the browser's
    // usual "new page" focus reset) — but only when nothing else already
    // claimed it. A sheet with an autofocused input grabs focus in its
    // own effect, which — being on a descendant — runs before this one,
    // so `activeElement` is already that input by the time this checks;
    // this only steps in for an ordinary page navigation.
    const active = document.activeElement;
    if (!active || active === document.body) {
      mainRef.current?.focus();
    }
  }, [location.pathname]);

  return (
    <SyncProvider>
      <div className="app-shell">
        <main ref={mainRef} tabIndex={-1} className="app-shell__scroll">
          <Suspense fallback={<RouteFallback />}>
            <Outlet />
          </Suspense>
        </main>
        <TabBar />
      </div>
    </SyncProvider>
  );
}

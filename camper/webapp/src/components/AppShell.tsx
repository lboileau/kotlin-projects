import { Suspense, useEffect, useLayoutEffect, useRef, useState } from 'react';
import { Outlet, useLocation, useNavigationType, type NavigationType } from 'react-router-dom';
import { TabBar } from './TabBar';
import { RouteFallback } from './RouteFallback';
import { usePagePath } from './usePagePath';
import { SyncProvider } from '../sync/SyncProvider';
import { prefetchOtherTabs } from '../routePrefetch';
import { areaForPath, type Area } from '../lib/areas';
import { getHistoryIndex } from '../lib/historyIndex';
import { recordNavEntry, scrollByEntry, scrollByPage } from '../lib/navHistory';
import './AppShell.css';

/** How the incoming page arrives: deeper slides in from the right, back from the left, a sideways move fades. */
type PageTransition = 'none' | 'forward' | 'back' | 'fade';

interface ShownPage {
  pagePath: string;
  area: Area | null;
  transition: PageTransition;
}

function depthOf(pathname: string): number {
  return pathname.split('/').filter(Boolean).length;
}

function transitionBetween(from: ShownPage, to: { pagePath: string; area: Area | null }, type: NavigationType): PageTransition {
  // Another tab is a sideways move however it was reached, Back included.
  if (from.area !== to.area) return 'fade';
  if (type === 'POP') return 'back';
  const change = depthOf(to.pagePath) - depthOf(from.pagePath);
  if (change < 0) return 'back';
  // Same depth: a sibling (Recipes/Ingredients, another plan's shopping list), not a step deeper.
  if (change === 0) return 'fade';
  return 'forward';
}

/**
 * Layout route for every guarded page: a scrollable content area plus a
 * pinned bottom tab bar. Desktop centers a max-width column; the tab bar
 * stays constrained to it. Sheets render over this via their own portal
 * (each page renders its own <Outlet/> for its child sheet routes).
 *
 * Also owns everything that answers "where am I": the area (`data-area`,
 * which drives the identity colour in styles/areas.css), the navigation
 * memory behind the back button (lib/navHistory.ts), the
 * direction a new page arrives from, and scroll restoration.
 *
 * Wrapped in SyncProvider here (not higher up, e.g. main.tsx) so the one
 * app-wide STOMP client only exists while signed in — this component
 * only ever renders once RequireAuth has confirmed that. The Suspense
 * boundary here is what every lazy route in router.tsx renders through.
 */
export function AppShell() {
  const location = useLocation();
  const navigationType = useNavigationType();
  const pagePath = usePagePath();
  const area = areaForPath(location.pathname);
  const mainRef = useRef<HTMLElement>(null);

  // Derived during render (not in an effect) so the new page's first paint
  // already carries its transition. Opening or closing a sheet keeps the
  // same page path, so it neither animates nor remounts the page.
  const [shown, setShown] = useState<ShownPage>({ pagePath, area, transition: 'none' });
  if (shown.pagePath !== pagePath) {
    setShown({ pagePath, area, transition: transitionBetween(shown, { pagePath, area }, navigationType) });
  }

  useEffect(() => {
    prefetchOtherTabs(location.pathname);
    // Only once, right after the shell first mounts — not on every
    // navigation, which would re-schedule the same idle prefetches
    // repeatedly as the user moves around.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    const index = getHistoryIndex();
    const pathname = location.pathname.length > 1 ? location.pathname.replace(/\/$/, '') : location.pathname;
    if (typeof index === 'number') recordNavEntry(index, { pagePath, isSheet: pathname !== pagePath });
  }, [location.key, location.pathname, pagePath]);

  // <main> is keyed by page, so every page starts in a fresh scroller at the
  // top. Going Back/Forward, or returning to a tab, puts it back where it
  // was. The page may still be loading its data and too short to scroll that
  // far yet, so keep trying for a moment.
  useLayoutEffect(() => {
    const restoreForTab = (location.state as { restoreScroll?: boolean } | null)?.restoreScroll === true;
    const target =
      navigationType === 'POP' ? scrollByEntry.get(location.key) : restoreForTab ? scrollByPage.get(pagePath) : undefined;
    if (!target) return;

    let frame = 0;
    let attempts = 0;
    function restore() {
      const main = mainRef.current;
      if (!main) return;
      main.scrollTop = target!;
      attempts += 1;
      if (main.scrollTop < target! - 1 && attempts < 30) frame = requestAnimationFrame(restore);
    }
    restore();
    return () => cancelAnimationFrame(frame);
    // Once per page: a sheet opening or closing over it must not move it.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [shown.pagePath]);

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

  function handleScroll() {
    const top = mainRef.current?.scrollTop ?? 0;
    scrollByEntry.set(location.key, top);
    scrollByPage.set(pagePath, top);
  }

  return (
    <SyncProvider>
      <div className="app-shell" data-area={area ?? undefined}>
        <main
          key={shown.pagePath}
          ref={mainRef}
          tabIndex={-1}
          className="app-shell__scroll"
          data-transition={shown.transition}
          onScroll={handleScroll}
        >
          <Suspense fallback={<RouteFallback />}>
            <Outlet />
          </Suspense>
        </main>
        <TabBar />
      </div>
    </SyncProvider>
  );
}

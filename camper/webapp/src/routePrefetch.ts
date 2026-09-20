// Idle-time prefetch for the two tab areas the user *isn't* currently
// on, so switching tabs later never has to wait on a chunk download in
// practice — by the time they tap, it's already sitting in the module
// cache from the dynamic import() below.

const SHOPPING_UNDER_PLAN = /^\/plans\/[^/]+\/shopping/;

// `navigator.connection` (Network Information API) isn't in the standard
// DOM lib types, so it's typed narrowly here rather than widened to `any`.
interface NavigatorConnection {
  saveData?: boolean;
}

function saveDataEnabled(): boolean {
  const connection = (navigator as Navigator & { connection?: NavigatorConnection }).connection;
  return connection?.saveData === true;
}

const TAB_AREA_LOADERS: Record<string, () => Promise<unknown>> = {
  plans: () => import('./pages/plans'),
  shopping: () => import('./pages/shopping'),
  recipes: () => import('./pages/recipes'),
};

function currentTabArea(pathname: string): string | null {
  if (pathname.startsWith('/shopping') || SHOPPING_UNDER_PLAN.test(pathname)) return 'shopping';
  if (pathname.startsWith('/plans')) return 'plans';
  if (pathname.startsWith('/recipes') || pathname.startsWith('/ingredients')) return 'recipes';
  return null;
}

function scheduleIdle(run: () => void): void {
  if (typeof window.requestIdleCallback === 'function') {
    window.requestIdleCallback(run);
  } else {
    window.setTimeout(run, 200);
  }
}

/** Call once, right after the shell mounts — not on every navigation. */
export function prefetchOtherTabs(currentPathname: string): void {
  // Respect the user's explicit "use less data" preference: warming tabs
  // they haven't asked for isn't worth the bytes when they've said so.
  if (saveDataEnabled()) return;

  const current = currentTabArea(currentPathname);

  for (const [area, load] of Object.entries(TAB_AREA_LOADERS)) {
    if (area !== current) scheduleIdle(() => void load());
  }

  // Ingredients lives under the Recipes tab (segmented control), not its
  // own tab, but is still worth warming unless that tab's already loaded.
  if (current !== 'recipes') {
    scheduleIdle(() => void import('./pages/ingredients'));
  }
}

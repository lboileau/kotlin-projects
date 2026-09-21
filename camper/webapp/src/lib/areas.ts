/**
 * The app's areas: the three tabs plus Account. An area is WHERE the user
 * is, and everything that says so reads it from here — the highlighted tab,
 * the identity colour (`styles/areas.css`), the header icon, the loading
 * icon, and whether the header's back arrow may follow browser history
 * (`lib/navHistory.ts`).
 *
 * The area comes from the URL alone, so a link decides which tab the user
 * ends up in by the path it points at. That is why a recipe opened from a
 * plan lives at /plans/:planId/recipes/:recipeId and one opened from the
 * shopping list at /plans/:planId/shopping/recipes/:recipeId: following a
 * link never moves the user to a different tab.
 */
export type Area = 'recipes' | 'plans' | 'shopping' | 'account';
export type TabArea = Exclude<Area, 'account'>;

const SHOPPING_UNDER_PLAN = /^\/plans\/[^/]+\/shopping(\/|$)/;

export function areaForPath(pathname: string): Area | null {
  if (pathname.startsWith('/recipes') || pathname.startsWith('/ingredients')) return 'recipes';
  if (pathname.startsWith('/shopping') || SHOPPING_UNDER_PLAN.test(pathname)) return 'shopping';
  if (pathname.startsWith('/plans') || pathname.startsWith('/join')) return 'plans';
  if (pathname.startsWith('/account')) return 'account';
  return null;
}

/** The title in the static header of every screen in the area. */
export const AREA_TITLE: Record<Area, string> = {
  recipes: 'Recipes',
  plans: 'Plans',
  shopping: 'Shopping',
  account: 'Account',
};

/** Where tapping a tab that is already active goes. */
export const TAB_ROOT: Record<TabArea, string> = {
  recipes: '/recipes',
  plans: '/plans',
  shopping: '/shopping',
};

/**
 * What the back button calls a destination ("‹ Plans"). Deliberately the
 * kind of screen rather than its name: the name would need that screen's
 * data, and the back button has to render before anything has loaded.
 */
export function labelForPath(pathname: string): string {
  if (/^\/plans\/[^/]+\/shopping\/?$/.test(pathname) || pathname === '/shopping') return 'Shopping list';
  if (/\/recipes\/[^/]+\/edit\/?$/.test(pathname)) return 'Edit recipe';
  if (/\/recipes\/[^/]+\/?$/.test(pathname) && !pathname.endsWith('/recipes/new')) return 'Recipe';
  if (/^\/plans\/[^/]+\/?$/.test(pathname)) return 'Plan';
  if (pathname.startsWith('/plans')) return 'Plans';
  if (pathname.startsWith('/ingredients')) return 'Ingredients';
  if (pathname.startsWith('/recipes')) return 'Recipes';
  if (pathname.startsWith('/account')) return 'Account';
  return 'Back';
}

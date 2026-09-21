import { NavLink, useLocation } from 'react-router-dom';
import { AREA_ICON } from './areaIcons';
import { TAB_ROOT, areaForPath, type TabArea } from '../lib/areas';
import { getSelectedPlanId } from '../lib/selectedPlan';
import './TabBar.css';

const TABS: { area: TabArea; label: string }[] = [
  { area: 'recipes', label: 'Recipes' },
  { area: 'plans', label: 'Plans' },
  { area: 'shopping', label: 'Shopping' },
];

/**
 * Where a tab goes: always its base screen, whether or not it is the tab you
 * are in — the recipe list, the selected plan, the selected plan's shopping
 * list. A tab never drops you back into the middle of whatever you were last
 * doing in it; getting back there is what the screens' own links are for.
 *
 * Plans and Shopping link straight to the selected plan rather than through
 * the /plans and /shopping redirects, so tapping the tab while already there
 * is a no-op instead of a new history entry. With no plan selected those
 * routes show what there is to show (the plans to pick from, or the empty
 * state).
 */
function tabTarget(area: TabArea): string {
  const selectedPlanId = getSelectedPlanId();
  if (!selectedPlanId) return TAB_ROOT[area];
  if (area === 'plans') return `/plans/${selectedPlanId}`;
  if (area === 'shopping') return `/plans/${selectedPlanId}/shopping`;
  return TAB_ROOT[area];
}

/**
 * Persistent bottom tab bar. The active tab is the current area
 * (`lib/areas.ts`), not a plain prefix match, so /ingredients counts as
 * Recipes and everything under /plans/:id/shopping counts as Shopping.
 */
export function TabBar() {
  const location = useLocation();
  const currentArea = areaForPath(location.pathname);

  return (
    <nav className="tab-bar" aria-label="Primary">
      {TABS.map(({ area, label }) => {
        const active = area === currentArea;
        const Icon = AREA_ICON[area];
        return (
          <NavLink
            key={area}
            to={tabTarget(area)}
            // Coming back to a tab's base screen puts its scroll position back
            // (AppShell); tapping the tab you are in starts at the top.
            state={active ? undefined : { restoreScroll: true }}
            className={`tab-bar__item${active ? ' tab-bar__item--active' : ''}`}
            aria-current={active ? 'page' : undefined}
          >
            <span className="tab-bar__pill">
              <Icon className="tab-bar__icon" aria-hidden="true" />
            </span>
            <span className="tab-bar__label">{label}</span>
          </NavLink>
        );
      })}
    </nav>
  );
}

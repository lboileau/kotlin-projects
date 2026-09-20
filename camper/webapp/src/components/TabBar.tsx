import { NavLink, useLocation } from 'react-router-dom';
import { BookmarkIcon, CalendarIcon, ListBulletIcon } from '@radix-ui/react-icons';
import { getSelectedPlanId } from '../lib/selectedPlan';
import './TabBar.css';

const SHOPPING_UNDER_PLAN = /^\/plans\/[^/]+\/shopping/;

const TABS = [
  {
    to: '/recipes',
    label: 'Recipes',
    Icon: BookmarkIcon,
    isActive: (path: string) => path.startsWith('/recipes') || path.startsWith('/ingredients'),
  },
  {
    to: '/plans',
    label: 'Plans',
    Icon: CalendarIcon,
    isActive: (path: string) => path.startsWith('/plans') && !SHOPPING_UNDER_PLAN.test(path),
  },
  {
    to: '/shopping',
    label: 'Shopping',
    Icon: ListBulletIcon,
    isActive: (path: string) => path.startsWith('/shopping') || SHOPPING_UNDER_PLAN.test(path),
  },
] as const;

/**
 * Persistent bottom tab bar. Active matching is custom (not plain prefix
 * matching) so /ingredients counts as Recipes and /plans/:id/shopping
 * counts as Shopping rather than Plans.
 */
export function TabBar() {
  const location = useLocation();

  // The Plans tab returns to the plan being worked on, so switching between
  // Plans and Shopping stays on the same plan. Tapping it again while already
  // on that plan goes up to the list of plans.
  const selectedPlanId = getSelectedPlanId();
  const selectedPlanPath = selectedPlanId ? `/plans/${selectedPlanId}` : null;
  const onSelectedPlan =
    selectedPlanPath !== null &&
    location.pathname.startsWith(selectedPlanPath) &&
    !SHOPPING_UNDER_PLAN.test(location.pathname);
  const plansTarget = selectedPlanPath !== null && !onSelectedPlan ? selectedPlanPath : '/plans';

  return (
    <nav className="tab-bar" aria-label="Primary">
      {TABS.map(({ to, label, Icon, isActive }) => {
        const active = isActive(location.pathname);
        return (
          <NavLink
            key={to}
            to={to === '/plans' ? plansTarget : to}
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

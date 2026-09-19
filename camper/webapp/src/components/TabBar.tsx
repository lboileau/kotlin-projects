import { NavLink, useLocation } from 'react-router-dom';
import { BookmarkIcon, CalendarIcon, ListBulletIcon } from '@radix-ui/react-icons';
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

  return (
    <nav className="tab-bar" aria-label="Primary">
      {TABS.map(({ to, label, Icon, isActive }) => {
        const active = isActive(location.pathname);
        return (
          <NavLink
            key={to}
            to={to}
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

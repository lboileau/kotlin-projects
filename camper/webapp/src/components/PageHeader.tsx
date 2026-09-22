import type { ComponentType, ReactNode } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { ChevronLeftIcon, Cross1Icon, PersonIcon } from '@radix-ui/react-icons';
import { AREA_ICON } from './areaIcons';
import { CollapsingHeader } from './CollapsingHeader';
import { useBack } from './useBack';
import { AREA_TITLE, areaForPath } from '../lib/areas';
import { usePageTitle } from '../lib/usePageTitle';
import './PageHeader.css';

interface HeaderBarProps {
  /** The tab's name, or a root screen's own title ("Ingredients"). */
  title: string;
  /** True when this title is the page's <h1> (root screens). */
  heading: boolean;
  icon?: ComponentType<{ className?: string }>;
  showAccount?: boolean;
}

/**
 * The static header every screen has, and the only part of a screen that
 * wears the tab's colour (styles/areas.css): the tab's icon, a title, and the
 * account button. It is the same on every screen of a tab, at any depth, so
 * the top of the screen always says which tab this is and never changes as
 * you move around inside it. Everything a screen can DO lives below it.
 */
export function HeaderBar({ title, heading, icon, showAccount = true }: HeaderBarProps) {
  const area = areaForPath(useLocation().pathname);
  const Icon = icon ?? (area ? AREA_ICON[area] : null);
  const Title = heading ? 'h1' : 'div';

  return (
    <div className="page-header__bar">
      <div className="page-header__leading">
        {Icon && (
          <span className="page-header__chip" aria-hidden="true">
            <Icon />
          </span>
        )}
        <Title className="page-header__title">{title}</Title>
      </div>
      {showAccount && (
        <Link to="/account" className="page-header__account" aria-label="Account">
          <PersonIcon />
        </Link>
      )}
    </div>
  );
}

interface PageHeaderProps {
  /** The screen's own title. Always the document title; shown where the screen's kind calls for it (below). */
  title: string;
  /**
   * The screen's parent in the hierarchy. Omit it for a tab's root screen.
   * It is where the back button goes when history can't be followed (see
   * `useBack`), not necessarily where it goes.
   */
  backTo?: string;
  /** A form the user is filling in: a close (✕) instead of a back button. */
  task?: boolean;
  /** Let the back button follow history into another tab. Only for Account. */
  backAcrossAreas?: boolean;
  /** Root screens only: the icon in the header's chip. Defaults to the tab's icon. */
  icon?: ComponentType<{ className?: string }>;
  /** The page shows its title in a `PageHero` below, so the toolbar leaves it out. */
  titleInHero?: boolean;
  /** The screen's own actions, at the right of the toolbar row. */
  actions?: ReactNode;
  /** Set false to hide the account button (e.g. on the Account page itself). */
  showAccount?: boolean;
  /**
   * Tuck the bar and `away` out of sight on scroll down, back on scroll up,
   * leaving the toolbar (back, the screen's actions) and `children` pinned
   * (see CollapsingHeader). On by default for every screen.
   */
  collapseOnScroll?: boolean;
  /** More that goes away with the bar on scroll (the Recipes/Ingredients toggle). */
  away?: ReactNode;
  /** What stays pinned under it (a search box and filters). */
  children?: ReactNode;
}

/**
 * A screen's sticky top: the static `HeaderBar`, and under it, for any
 * screen that is not a tab's root, a plain toolbar row — a back button that
 * names where it goes ("‹ Recipes"), or a ✕ for a form (`task`), then the
 * screen's title unless a `PageHero` shows it, then the screen's actions.
 * The toolbar is ordinary page chrome in the app's accent; only the bar
 * above it is coloured.
 *
 * Also sets document.title to match — every page using this gets that for free.
 */
export function PageHeader({
  title,
  backTo,
  task = false,
  backAcrossAreas = false,
  icon,
  titleInHero = false,
  actions,
  showAccount = true,
  collapseOnScroll = true,
  away,
  children,
}: PageHeaderProps) {
  usePageTitle(title);
  const area = areaForPath(useLocation().pathname);
  const isRoot = backTo === undefined;

  const toolbar = !isRoot && (
    <div className="page-header__toolbar">
      <div className="page-header__leading">
        {/* A form closes back to wherever it was opened from, even another tab (a recipe edited from a plan). */}
        <BackButton parentPath={backTo} task={task} acrossAreas={backAcrossAreas || task} />
        {!titleInHero && <h1 className="page-header__toolbar-title">{title}</h1>}
      </div>
      {actions && <div className="page-header__trailing">{actions}</div>}
    </div>
  );

  return (
    <CollapsingHeader
      collapseOnScroll={collapseOnScroll}
      away={
        <>
          <HeaderBar
            title={isRoot || !area ? title : AREA_TITLE[area]}
            heading={isRoot}
            icon={isRoot ? icon : undefined}
            showAccount={showAccount}
          />
          {away}
        </>
      }
    >
      {toolbar}
      {children}
    </CollapsingHeader>
  );
}

function BackButton({ parentPath, task, acrossAreas }: { parentPath: string; task: boolean; acrossAreas: boolean }) {
  const { label, goBack } = useBack(parentPath, { acrossAreas });

  if (task) {
    return (
      <button type="button" className="header-icon-button" aria-label="Close" onClick={goBack}>
        <Cross1Icon />
      </button>
    );
  }

  return (
    <button type="button" className="page-header__back" aria-label={label === 'Back' ? 'Back' : `Back to ${label}`} onClick={goBack}>
      <ChevronLeftIcon />
      <span className="page-header__back-label">{label}</span>
    </button>
  );
}

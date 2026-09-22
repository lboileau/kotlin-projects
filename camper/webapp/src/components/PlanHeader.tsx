import { useLayoutEffect, useRef, useState, type ReactNode } from 'react';
import { useLocation } from 'react-router-dom';
import { Skeleton } from '@radix-ui/themes';
import { ChevronDownIcon } from '@radix-ui/react-icons';
import { HeaderBar } from './PageHeader';
import { SheetLink } from './SheetLink';
import { useScrolledDown } from './useScrollDirection';
import { AREA_TITLE, areaForPath } from '../lib/areas';
import './PlanHeader.css';

interface PlanHeaderProps {
  /** Undefined while the plan is loading: the row keeps its shape and shows a placeholder. */
  planName: string | undefined;
  actions?: ReactNode;
  /** A second row under the plan name (the shopping list's progress). */
  children?: ReactNode;
  /**
   * Tuck the plan row away while the user scrolls down the list, leaving the
   * bar and `children`, and bring it back as soon as they scroll up. For the
   * shopping list, where every row of list on screen counts.
   */
  collapseOnScroll?: boolean;
}

/**
 * The sticky top of the two screens that show one plan: the Plans tab and
 * the Shopping tab. The static `HeaderBar`, then a row with the plan's name
 * as a dropdown that opens the `switch` sheet (SwitchPlanSheet, a child
 * route of both pages) to change plan or make a new one, and the screen's
 * actions. Neither screen has a back button — each is its tab's one root
 * view, and the plan is changed in place.
 *
 * Rendered in every state of those pages, loading included, so the top of
 * the screen never changes shape as the plan arrives. The plan's name is the
 * page's <h1>.
 *
 * Collapsing slides the rows up behind the bar with a transform rather than
 * changing the header's height. The header is sticky and in the page's flow,
 * so a height change would move the whole list under the user's finger
 * mid-scroll (and flip the scroll direction that caused it); a transform
 * leaves the layout alone, and the strip it vacates is see-through.
 */
export function PlanHeader({ planName, actions, children, collapseOnScroll = false }: PlanHeaderProps) {
  const area = areaForPath(useLocation().pathname);
  const headerRef = useRef<HTMLElement>(null);
  const topRowRef = useRef<HTMLDivElement>(null);
  const collapsed = useScrolledDown(headerRef, collapseOnScroll);

  // How far to slide: the plan row plus the gap under it, measured because
  // the row's height follows the text size.
  const [slide, setSlide] = useState(0);
  useLayoutEffect(() => {
    const row = topRowRef.current;
    if (!row || !collapseOnScroll) return;
    const measure = () => {
      const gap = parseFloat(getComputedStyle(row.parentElement!).rowGap) || 0;
      setSlide(row.offsetHeight + gap);
    };
    measure();
    const observer = new ResizeObserver(measure);
    observer.observe(row);
    return () => observer.disconnect();
  }, [collapseOnScroll]);

  // Publishes the header's *visible* height as `--plan-header-height` on the
  // page, so anything else that sticks (the shopping list's category bands)
  // sits just under it. Collapsing is a transform, so the layout height never
  // changes — the visible height is that minus the slide while collapsed.
  useLayoutEffect(() => {
    const header = headerRef.current;
    if (!header) return;
    const root = document.documentElement;
    const publish = () => root.style.setProperty('--plan-header-height', `${header.offsetHeight - (collapsed ? slide : 0)}px`);
    publish();
    const observer = new ResizeObserver(publish);
    observer.observe(header);
    return () => {
      observer.disconnect();
      root.style.removeProperty('--plan-header-height');
    };
  }, [collapsed, slide]);

  return (
    <header ref={headerRef} className={`page-header${collapseOnScroll ? ' plan-header--collapsible' : ''}`}>
      <HeaderBar title={area ? AREA_TITLE[area] : 'Plan'} heading={false} />
      <div
        className="plan-header__rows"
        style={collapsed ? { transform: `translateY(-${slide}px)` } : undefined}
      >
        {/* `inert` while tucked away: out of sight must also mean out of the tab order. */}
        <div ref={topRowRef} className="plan-header__top" inert={collapsed}>
          <SheetLink
            to="switch"
            className="plan-header__plan"
            aria-label={planName ? `${planName}, switch plan` : 'Switch plan'}
          >
            <h1 className="plan-header__plan-name">{planName ?? <Skeleton>Loading plan</Skeleton>}</h1>
            <ChevronDownIcon aria-hidden="true" />
          </SheetLink>
          {actions && <div className="plan-header__actions">{actions}</div>}
        </div>
        {children}
      </div>
    </header>
  );
}

import type { ReactNode } from 'react';
import { useLocation } from 'react-router-dom';
import { Skeleton } from '@radix-ui/themes';
import { ChevronDownIcon } from '@radix-ui/react-icons';
import { CollapsingHeader } from './CollapsingHeader';
import { HeaderBar } from './PageHeader';
import { SheetLink } from './SheetLink';
import { AREA_TITLE, areaForPath } from '../lib/areas';
import './PlanHeader.css';

interface PlanHeaderProps {
  /** Undefined while the plan is loading: the row keeps its shape and shows a placeholder. */
  planName: string | undefined;
  actions?: ReactNode;
  /** A second row under the plan name (the shopping list's progress). */
  children?: ReactNode;
  /**
   * Tuck the plan row away on scroll down too (the bar always goes), leaving
   * only the children. For long lists — the shopping list, where every row of list
   * on screen counts; the plan page keeps its row (name, share, edit).
   */
  collapseTopRow?: boolean;
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
 * Collapsing (CollapsingHeader) slides the bar — and on the shopping list
 * the plan row too — up out of sight on scroll down, with a transform rather
 * than a height change so the list never moves under the user's finger.
 */
export function PlanHeader({ planName, actions, children, collapseTopRow = false }: PlanHeaderProps) {
  const area = areaForPath(useLocation().pathname);

  const topRow = (
    <div className="plan-header__top">
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
  );

  return (
    <CollapsingHeader
      collapseOnScroll
      away={
        <>
          <HeaderBar title={area ? AREA_TITLE[area] : 'Plan'} heading={false} />
          {collapseTopRow && <div className="plan-header__rows plan-header__rows--away">{topRow}</div>}
        </>
      }
    >
      {(!collapseTopRow || children) && (
        <div className={`plan-header__rows${collapseTopRow ? ' plan-header__rows--pinned' : ''}`}>
          {!collapseTopRow && topRow}
          {children}
        </div>
      )}
    </CollapsingHeader>
  );
}

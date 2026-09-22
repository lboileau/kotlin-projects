import type { ReactNode } from 'react';
import { useLocation } from 'react-router-dom';
import { Skeleton } from '@radix-ui/themes';
import { AREA_ICON } from './areaIcons';
import { areaForPath } from '../lib/areas';
import './PageHero.css';

interface PageHeroProps {
  /** What kind of thing this screen shows ("Recipe", "Plan"). Shown small, above the title, beside the area's icon. */
  eyebrow: string;
  /** Undefined while loading: a placeholder of the same height. */
  title: string | undefined;
  /**
   * One control, right-justified across from the title and aligned to its
   * first line (the recipe page's favourite heart). The title keeps the rest
   * of the row and still wraps — it is never cut to one line. A screen that
   * shows this once loaded should pass a same-sized placeholder while it
   * loads, or the title shifts sideways when the control appears.
   */
  action?: ReactNode;
  /** The screen's at-a-glance facts, under the title. */
  children?: ReactNode;
}

/**
 * The top block of a detail screen: what kind of thing this is, its name in
 * full (it wraps, where the old one-line bar title was cut short), and the
 * facts worth a glance. Use with `<PageHeader titleInHero />`; this renders
 * the page's <h1>. Rendered while loading too (no title yet), so the page
 * keeps its shape as the data arrives.
 */
export function PageHero({ eyebrow, title, action, children }: PageHeroProps) {
  const area = areaForPath(useLocation().pathname);
  const Icon = area ? AREA_ICON[area] : null;

  return (
    <div className="page-hero">
      <div className="page-hero__eyebrow">
        {Icon && <Icon aria-hidden="true" />}
        <span>{eyebrow}</span>
      </div>
      {/* The row is always a flex row with the <h1> as its only growing
          child, so a hero with no action lays out exactly as it did when
          the <h1> was a plain block: full width, same wrapping. */}
      <div className="page-hero__title-row">
        <h1 className="page-hero__title">{title ?? <Skeleton>Loading</Skeleton>}</h1>
        {action && <div className="page-hero__action">{action}</div>}
      </div>
      {children && <div className="page-hero__meta">{children}</div>}
    </div>
  );
}

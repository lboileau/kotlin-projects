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
export function PageHero({ eyebrow, title, children }: PageHeroProps) {
  const area = areaForPath(useLocation().pathname);
  const Icon = area ? AREA_ICON[area] : null;

  return (
    <div className="page-hero">
      <div className="page-hero__eyebrow">
        {Icon && <Icon aria-hidden="true" />}
        <span>{eyebrow}</span>
      </div>
      <h1 className="page-hero__title">{title ?? <Skeleton>Loading</Skeleton>}</h1>
      {children && <div className="page-hero__meta">{children}</div>}
    </div>
  );
}

import './PageLoader.css';

export type LoaderArea = 'recipes' | 'plans' | 'shopping';

interface PageLoaderProps {
  area: LoaderArea;
  /** Announced to screen readers, e.g. "Loading recipes". */
  label: string;
}

/**
 * Per-area loading indicator: a thin outline icon whose strokes draw
 * themselves in a slow loop. One icon per tab so a loading screen already
 * says where you are. Every shape carries `pathLength={1}` so the same
 * dash animation works whatever the shape's real length. Reduced motion
 * shows the finished icon with a soft pulse instead.
 */
export function PageLoader({ area, label }: PageLoaderProps) {
  return (
    <div className="page-loader" role="status" aria-label={label}>
      <svg
        className="page-loader__icon"
        viewBox="0 0 64 64"
        width="72"
        height="72"
        fill="none"
        stroke="currentColor"
        strokeWidth="2.5"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden="true"
      >
        {area === 'recipes' && <RecipesIcon />}
        {area === 'plans' && <PlansIcon />}
        {area === 'shopping' && <ShoppingIcon />}
      </svg>
    </div>
  );
}

/** An open recipe book: two pages, a spine, and a few lines of text. */
function RecipesIcon() {
  return (
    <>
      <path pathLength={1} d="M32 18c-5-4-12-5-20-4v34c8-1 15 0 20 4" />
      <path pathLength={1} d="M32 18c5-4 12-5 20-4v34c-8-1-15 0-20 4" />
      <path pathLength={1} d="M32 18v34" />
      <path pathLength={1} className="page-loader__detail" d="M18 24c3 0 6 .5 9 2" />
      <path pathLength={1} className="page-loader__detail" d="M18 31c3 0 6 .5 9 2" />
      <path pathLength={1} className="page-loader__detail" d="M37 26c3-1.5 6-2 9-2" />
      <path pathLength={1} className="page-loader__detail" d="M37 33c3-1.5 6-2 9-2" />
    </>
  );
}

/** A week on a calendar: the frame, two binder rings, and a row of days with one ticked. */
function PlansIcon() {
  return (
    <>
      <rect pathLength={1} x="12" y="16" width="40" height="36" rx="6" />
      <path pathLength={1} d="M12 26h40" />
      <path pathLength={1} d="M22 11v9" />
      <path pathLength={1} d="M42 11v9" />
      <path pathLength={1} className="page-loader__detail" d="M20 35h4" />
      <path pathLength={1} className="page-loader__detail" d="M30 35h4" />
      <path pathLength={1} className="page-loader__detail" d="M40 35h4" />
      <path pathLength={1} className="page-loader__detail" d="M20 44h4" />
      <path pathLength={1} className="page-loader__detail" d="M29 44l2.5 2.5L37 41" />
    </>
  );
}

/** A shopping basket: the handle, the basket body, and its weave. */
function ShoppingIcon() {
  return (
    <>
      <path pathLength={1} d="M22 28l7-14" />
      <path pathLength={1} d="M42 28l-7-14" />
      <path pathLength={1} d="M10 28h44" />
      <path pathLength={1} d="M14 28l4 20a4 4 0 0 0 4 3h20a4 4 0 0 0 4-3l4-20" />
      <path pathLength={1} className="page-loader__detail" d="M25 35v9" />
      <path pathLength={1} className="page-loader__detail" d="M32 35v9" />
      <path pathLength={1} className="page-loader__detail" d="M39 35v9" />
    </>
  );
}

import { useLocation, useMatches } from 'react-router-dom';

// router.tsx nests every guarded route as RequireAuth > AppShell > page >
// sheet, and the two layout routes are matches too, so the page is the
// third match.
const PAGE_MATCH_INDEX = 2;

/**
 * Pathname of the current page, without any sheet open over it:
 * `/plans/abc` for both `/plans/abc` and `/plans/abc/edit`. Sheets are child
 * routes of their page, so this is the page route's own match.
 */
export function usePagePath(): string {
  const matches = useMatches();
  const location = useLocation();
  const pathname = matches[PAGE_MATCH_INDEX]?.pathname ?? location.pathname;
  return pathname.length > 1 ? pathname.replace(/\/$/, '') : pathname;
}

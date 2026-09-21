import { useLocation, useNavigate } from 'react-router-dom';
import { areaForPath, labelForPath } from '../lib/areas';
import { getHistoryIndex } from '../lib/historyIndex';
import { getNavEntry } from '../lib/navHistory';
import { usePagePath } from './usePagePath';

interface UseBackOptions {
  /**
   * Follow history even into a different area. Only for a screen that
   * belongs to no tab and is reached from anywhere (Account), whose "parent"
   * is simply wherever the user was.
   */
  acrossAreas?: boolean;
}

/**
 * The header's back button: where it goes and what it is called.
 *
 * It goes back in history when the previous page is in the same area, so
 * the arrow, a swipe and the browser's Back all land in the same place and
 * the scroll position comes back with it. Otherwise (a deep link, a reload
 * with no history, or the previous page being another tab) it replaces to
 * `parentPath`, the screen's parent in the hierarchy. Either way the button
 * stays inside the current tab.
 */
export function useBack(parentPath: string, options?: UseBackOptions): { label: string; goBack: () => void } {
  const navigate = useNavigate();
  const location = useLocation();
  const pagePath = usePagePath();

  // Entries for this same page (a sheet that was open over it, or the page
  // under the sheet that is open now) are not "back": skip past them to the
  // page the user was on before this one.
  const index = getHistoryIndex();
  let steps = 0;
  let previous = null;
  if (typeof index === 'number') {
    for (let i = index - 1; i >= 0; i -= 1) {
      const entry = getNavEntry(i);
      if (!entry) break;
      if (entry.pagePath === pagePath) continue;
      if (!entry.isSheet) {
        previous = entry;
        steps = index - i;
      }
      break;
    }
  }

  const followsHistory =
    previous !== null &&
    (options?.acrossAreas || areaForPath(previous.pagePath) === areaForPath(location.pathname));

  return {
    label: labelForPath(followsHistory && previous ? previous.pagePath : parentPath),
    goBack: () => {
      if (followsHistory) navigate(-steps);
      else navigate(parentPath, { replace: true });
    },
  };
}

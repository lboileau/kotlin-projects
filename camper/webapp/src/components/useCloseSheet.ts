import { useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { getHistoryIndex } from '../lib/historyIndex';

/**
 * Closes a route-driven sheet: goes back in history when there's a real
 * previous entry in this tab to return to, or replaces to its parent
 * page when there isn't (deep link, reload, or a sign-in redirect chain
 * that only ever replaced) — see `lib/historyIndex.ts` for why this is
 * decided from the history entry's index rather than `location.key`.
 */
export function useCloseSheet(parentPath: string): () => void {
  const navigate = useNavigate();

  return useCallback(() => {
    const index = getHistoryIndex();
    if (typeof index === 'number' && index > 0) {
      navigate(-1);
    } else {
      navigate(parentPath, { replace: true });
    }
  }, [navigate, parentPath]);
}

import { useEffect } from 'react';

const APP_NAME = 'Meal Planner';

/** Sets document.title for the current screen — the next screen's own call overwrites it, so no unmount cleanup is needed. */
export function usePageTitle(title: string): void {
  useEffect(() => {
    document.title = title ? `${title} · ${APP_NAME}` : APP_NAME;
  }, [title]);
}

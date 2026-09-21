import { useLayoutEffect, useRef, type ReactNode } from 'react';
import './BottomBar.css';

interface BottomBarProps {
  children: ReactNode;
}

/**
 * Docked action bar, sticky above the tab bar — the shared "always
 * reachable without scrolling" treatment for a screen's primary action
 * (recipe detail's "Add to plan", plan detail's "Shopping list", plans
 * home's "New plan"). Not rendered in loading/error/not-found states —
 * callers only mount it once their real content (and the action's real
 * target) is ready.
 *
 * Relies on its containing page using the same shell as everywhere else
 * in the app (`display: flex; flex-direction: column; min-height: 100%`
 * on the page, `flex: 1` on the scrollable body above this bar) — together
 * these push the bar to the bottom of the screen even when there's too
 * little content to scroll, not just once there's enough to.
 */
export function BottomBar({ children }: BottomBarProps) {
  const ref = useRef<HTMLDivElement>(null);

  // Publishes the bar's height as `--bottom-bar-height` so toasts sit above
  // it (Toast.css). A toast used to cover the bar's button exactly, and a
  // tap meant for Undo a moment after the toast expired landed on the button
  // underneath instead. Measured, not assumed: the bar is one button on most
  // screens and two rows on a draft recipe.
  useLayoutEffect(() => {
    const bar = ref.current;
    if (!bar) return;
    const root = document.documentElement;
    const publish = () => root.style.setProperty('--bottom-bar-height', `${bar.offsetHeight}px`);
    publish();
    const observer = new ResizeObserver(publish);
    observer.observe(bar);
    return () => {
      observer.disconnect();
      root.style.removeProperty('--bottom-bar-height');
    };
  }, []);

  return (
    <div ref={ref} className="bottom-bar">
      {children}
    </div>
  );
}

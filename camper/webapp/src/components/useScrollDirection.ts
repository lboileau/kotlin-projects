import { useEffect, useState, type RefObject } from 'react';

// How far a scroll has to travel in one direction before it counts: enough
// that a resting thumb or the end of a fling doesn't flip the header.
const TRAVEL_PX = 16;
// Always expanded this close to the top of the list.
const TOP_ZONE_PX = 24;

/**
 * True once the user has scrolled down the nearest scrolling ancestor of
 * `ref`, false again as soon as they scroll back up (or reach the top) — the
 * "hide on scroll down, show on scroll up" signal. Overscroll is ignored: on
 * iOS the rubber-band at either end reports scroll positions outside the
 * real range, and the bounce back from the bottom would otherwise read as a
 * scroll up.
 */
export function useScrolledDown(ref: RefObject<HTMLElement | null>, enabled: boolean): boolean {
  const [scrolledDown, setScrolledDown] = useState(false);

  useEffect(() => {
    if (!enabled) return;
    const scroller = ref.current?.closest<HTMLElement>('.app-shell__scroll');
    if (!scroller) return;

    let anchor = scroller.scrollTop;
    let last = scroller.scrollTop;

    function handleScroll() {
      const max = scroller!.scrollHeight - scroller!.clientHeight;
      const top = scroller!.scrollTop;
      if (top < 0 || top > max) return;

      if (top <= TOP_ZONE_PX) {
        setScrolledDown(false);
        anchor = top;
      } else {
        // The anchor is where the current run in one direction started.
        if ((top > last && anchor > last) || (top < last && anchor < last)) anchor = last;
        if (top - anchor > TRAVEL_PX) setScrolledDown(true);
        else if (anchor - top > TRAVEL_PX) setScrolledDown(false);
      }
      last = top;
    }

    scroller.addEventListener('scroll', handleScroll, { passive: true });
    return () => scroller.removeEventListener('scroll', handleScroll);
  }, [ref, enabled]);

  return enabled && scrolledDown;
}

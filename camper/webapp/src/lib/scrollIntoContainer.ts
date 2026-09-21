/**
 * Scrolls `element` to the top of its nearest scrolling ancestor, and only
 * that ancestor. Use this instead of `element.scrollIntoView()`, which
 * scrolls EVERY ancestor it can — including `<body>`, which `overflow:
 * hidden` stops the user from scrolling but not a script. In this app that
 * shifted the whole frame up a little on every call (header and Save pushed
 * off the top, tab bar floating mid-screen) with no way to scroll it back,
 * since the document is never meant to scroll (styles/global.css).
 *
 * Leaves room for a sticky `.page-header` at the top of the scroller.
 */
function nearestScroller(element: HTMLElement): HTMLElement | null {
  let scroller = element.parentElement;
  while (scroller && scroller !== document.body) {
    const overflowY = getComputedStyle(scroller).overflowY;
    if ((overflowY === 'auto' || overflowY === 'scroll') && scroller.scrollHeight > scroller.clientHeight) return scroller;
    scroller = scroller.parentElement;
  }
  return null;
}

export function scrollIntoContainer(element: HTMLElement, margin = 8): void {
  const scroller = nearestScroller(element);
  if (!scroller) return;

  const stickyHeader = scroller.querySelector<HTMLElement>('.page-header');
  const offset = (stickyHeader?.offsetHeight ?? 0) + margin;
  const top = scroller.scrollTop + element.getBoundingClientRect().top - scroller.getBoundingClientRect().top - offset;
  scroller.scrollTo({ top: Math.max(0, top), behavior: 'smooth' });
}

/**
 * Scrolls the nearest scrolling ancestor just far enough to show `element`,
 * and not at all if it is already in view — `scrollIntoView({ block:
 * 'nearest' })`, without touching <body> (see above) and clear of the
 * scroller's sticky `.page-header` and docked `.bottom-bar`.
 */
export function revealInContainer(element: HTMLElement, margin = 12): void {
  const scroller = nearestScroller(element);
  if (!scroller) return;

  const frame = scroller.getBoundingClientRect();
  const top = frame.top + (scroller.querySelector<HTMLElement>('.page-header')?.offsetHeight ?? 0) + margin;
  const bottom = frame.bottom - (scroller.querySelector<HTMLElement>('.bottom-bar')?.offsetHeight ?? 0) - margin;
  const rect = element.getBoundingClientRect();

  let delta = 0;
  if (rect.bottom > bottom) delta = rect.bottom - bottom;
  else if (rect.top < top) delta = rect.top - top;
  if (delta !== 0) scroller.scrollTo({ top: scroller.scrollTop + delta, behavior: 'smooth' });
}

import { useLayoutEffect, useRef, useState, type ReactNode } from 'react';
import { useScrolledDown } from './useScrollDirection';
import './CollapsingHeader.css';

interface CollapsingHeaderProps {
  /** Whether scrolling down tucks the `away` part out of sight (and scrolling up brings it back). */
  collapseOnScroll: boolean;
  /** The top part of the header: slides up out of sight on scroll down. */
  away: ReactNode;
  /** The rest: stays pinned to the top of the screen. */
  children?: ReactNode;
  className?: string;
}

/**
 * A screen's sticky top, in two parts: what goes away on scroll down (the
 * static bar, and whatever else the screen can spare mid-scroll) and what
 * stays (a search box, a progress row). Sliding is a transform: the header's
 * layout height never changes, so the list never jumps under the user's
 * finger and the scroll direction that caused the collapse can't flip. The
 * strip the slid part vacates is see-through and lets taps through
 * (`pointer-events`), and the tucked-away part is `inert` so out of sight is
 * out of the tab order too.
 *
 * Publishes the header's *visible* height as `--page-top-height` on `<html>`
 * so anything else that sticks (the shopping list's category bands) can sit
 * just under it.
 */
export function CollapsingHeader({ collapseOnScroll, away, children, className }: CollapsingHeaderProps) {
  const headerRef = useRef<HTMLElement>(null);
  const awayRef = useRef<HTMLDivElement>(null);
  const collapsed = useScrolledDown(headerRef, collapseOnScroll);

  // How far to slide: the away part's height, measured because it follows the text size.
  const [slide, setSlide] = useState(0);
  useLayoutEffect(() => {
    const part = awayRef.current;
    if (!part || !collapseOnScroll) return;
    const measure = () => setSlide(part.offsetHeight);
    measure();
    const observer = new ResizeObserver(measure);
    observer.observe(part);
    return () => observer.disconnect();
  }, [collapseOnScroll]);

  useLayoutEffect(() => {
    const header = headerRef.current;
    if (!header) return;
    const root = document.documentElement;
    const publish = () => root.style.setProperty('--page-top-height', `${header.offsetHeight - (collapsed ? slide : 0)}px`);
    publish();
    const observer = new ResizeObserver(publish);
    observer.observe(header);
    return () => {
      observer.disconnect();
      root.style.removeProperty('--page-top-height');
    };
  }, [collapsed, slide]);

  return (
    <header
      ref={headerRef}
      className={`page-header${collapseOnScroll ? ' collapsing-header' : ''}${className ? ` ${className}` : ''}`}
    >
      <div className="collapsing-header__slide" style={collapsed ? { transform: `translateY(-${slide}px)` } : undefined}>
        <div ref={awayRef} className="collapsing-header__away" inert={collapsed}>
          {away}
        </div>
        {children}
      </div>
    </header>
  );
}

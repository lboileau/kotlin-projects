import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getHistoryIndex } from '../lib/historyIndex';
import { prefersReducedMotion } from '../lib/prefersReducedMotion';

// Must stay in sync with the exit animation durations in Sheet.css.
const EXIT_ANIMATION_MS = 180;

export interface CloseTarget {
  to: string;
  replace?: boolean;
  /**
   * Skip `canClose`. For closing from inside the very work `canClose` guards,
   * once it has finished: the guard reads render state (`isPending`), which
   * is still the pre-completion value in the same tick the work resolves.
   */
  force?: boolean;
}

export interface UseSheetOptions {
  /** Return false to block the close (e.g. a mutation is still in flight). */
  canClose?: () => boolean;
  /** Called instead of closing when `canClose` returns false. */
  onBlockedClose?: () => void;
}

export interface UseSheetResult {
  /** Spread onto `<Sheet {...sheet.sheetProps} title=... />`. */
  sheetProps: { open: boolean; onClose: () => void };
  /**
   * Closes the sheet. With no argument, goes back when there is in-app
   * history and replaces to the parent otherwise (the normal case). Pass
   * a target to land somewhere else instead, e.g. after a delete or a
   * successful create: `close({ to: '/plans', replace: true })`.
   */
  close: (target?: CloseTarget) => void;
}

/**
 * Owns a sheet's open/closing state, so `Sheet` stays purely
 * presentational and its exit animation can play before the route
 * actually changes underneath it (a route unmount is instant and would
 * otherwise cut the animation off completely).
 *
 * `close()` is the single path for every close — the sheet's own X
 * button / overlay tap / Escape (wired through `sheetProps.onClose`) and
 * any page-level programmatic close (e.g. after a successful mutation,
 * optionally navigating elsewhere via a target) both call this same
 * function, so they always animate the same way. It's idempotent
 * (calling it again while already closing is a no-op). A browser-back
 * close is not routed through here at all: the route unmounts the sheet
 * immediately as part of that native transition, which is expected to
 * stay instant.
 *
 * `canClose`/`onBlockedClose` let a sheet refuse to close while some
 * async work is in flight (e.g. an import), without a page-level wrapper
 * around `close`.
 */
export function useSheet(parentPath: string, options?: UseSheetOptions): UseSheetResult {
  const navigate = useNavigate();
  const [open, setOpen] = useState(true);
  const closingRef = useRef(false);
  const timerRef = useRef<number | undefined>(undefined);
  // Options carry callbacks that may close over per-render state (e.g.
  // `isPending`); keep the latest ones in a ref so `close` — created
  // fresh each render as a plain function, not memoized — always reads
  // current values without needing to be in anyone's dependency array.
  // Refs can't be written during render, so this is synced via effect
  // instead — safe here because it always runs (and commits) before any
  // event handler that could call `close` has a chance to fire.
  const optionsRef = useRef(options);
  useEffect(() => {
    optionsRef.current = options;
  });

  useEffect(() => {
    return () => {
      // If this unmounts mid-animation for some unrelated reason (e.g. a
      // totally different navigation happened first), the scheduled
      // finish() must not still fire and navigate a second time.
      if (timerRef.current !== undefined) window.clearTimeout(timerRef.current);
    };
  }, []);

  function close(target?: CloseTarget) {
    const opts = optionsRef.current;
    if (!target?.force && opts?.canClose && !opts.canClose()) {
      opts.onBlockedClose?.();
      return;
    }

    if (closingRef.current) return;
    closingRef.current = true;

    function finish() {
      if (target) {
        navigate(target.to, { replace: target.replace ?? false });
        return;
      }
      const index = getHistoryIndex();
      if (typeof index === 'number' && index > 0) {
        navigate(-1);
      } else {
        navigate(parentPath, { replace: true });
      }
    }

    if (prefersReducedMotion()) {
      finish();
      return;
    }

    setOpen(false);
    timerRef.current = window.setTimeout(finish, EXIT_ANIMATION_MS);
  }

  return { sheetProps: { open, onClose: () => close() }, close };
}

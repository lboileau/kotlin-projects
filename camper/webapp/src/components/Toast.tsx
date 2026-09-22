import { useSyncExternalStore } from 'react';
import { createPortal } from 'react-dom';
import { Callout, Theme } from '@radix-ui/themes';
import { Cross2Icon } from '@radix-ui/react-icons';
import { dismissToast, getToastsSnapshot, subscribeToasts, type ToastEntry } from '../lib/toastStore';
import './Toast.css';

function ToastItem({ entry }: { entry: ToastEntry }) {
  return (
    <Callout.Root
      color={entry.tone === 'error' ? 'red' : undefined}
      variant="surface"
      className="toast-region__item"
    >
      <Callout.Text className="toast-region__text">{entry.message}</Callout.Text>
      <div className="toast-region__actions">
        {entry.action && (
          <button
            type="button"
            className="toast-region__action"
            onClick={() => {
              entry.action?.onClick();
              dismissToast(entry.id);
            }}
          >
            {entry.action.label}
          </button>
        )}
        <button
          type="button"
          className="toast-region__dismiss"
          aria-label="Dismiss"
          onClick={() => dismissToast(entry.id)}
        >
          <Cross2Icon />
        </button>
      </div>
    </Callout.Root>
  );
}

/**
 * App-wide toast region, dropping down over the header bar. Rendered once at
 * the app root; call `toast.error(msg)` / `toast.info(msg)` from
 * `lib/toastStore` to show one, from anywhere (including outside React,
 * e.g. the query client's mutation cache).
 *
 * Two separate live regions, not one shared one: `polite` for info so it
 * doesn't interrupt, `assertive` for errors so it does. Both containers
 * stay mounted (empty divs render nothing visible) rather than being
 * added only once a toast exists — a live region needs to already be
 * present in the DOM before its content changes for some screen readers
 * to reliably announce it.
 *
 * Portalled to `document.body` (nested `<Theme>` for tokens, same as
 * `Sheet.tsx`) rather than rendered in place: the ROOT `<Theme>` in
 * main.tsx gets `position: relative; z-index: 0` from Radix Themes' own
 * `[data-is-root-theme='true']` CSS, which makes it establish a stacking
 * context — every ordinary descendant, including this region, is then
 * confined inside that z-index: 0 context no matter how high a z-index
 * it sets locally (`z-index: 1000` here is completely invisible to any
 * comparison happening outside that context). A Sheet or Select/
 * DropdownMenu is portalled to `document.body` too, escaping the root
 * theme entirely, so it always won that comparison outright — hence the
 * toast rendering behind them regardless of its own z-index. Portalling
 * the toast out the same way puts it in the same un-trapped comparison
 * as those, where 1000 finally means what it says.
 */
export function ToastRegion() {
  const entries = useSyncExternalStore(subscribeToasts, getToastsSnapshot);
  const infoEntries = entries.filter((entry) => entry.tone === 'info');
  const errorEntries = entries.filter((entry) => entry.tone === 'error');

  return createPortal(
    <Theme hasBackground={false}>
      <div className="toast-region">
        <div className="toast-region__live-group" aria-live="polite">
          {infoEntries.map((entry) => (
            <ToastItem key={entry.id} entry={entry} />
          ))}
        </div>
        <div className="toast-region__live-group" aria-live="assertive" role="alert">
          {errorEntries.map((entry) => (
            <ToastItem key={entry.id} entry={entry} />
          ))}
        </div>
      </div>
    </Theme>,
    document.body,
  );
}

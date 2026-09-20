import { useSyncExternalStore } from 'react';
import { Callout } from '@radix-ui/themes';
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
 * App-wide toast region, positioned above the tab bar. Rendered once at
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
 */
export function ToastRegion() {
  const entries = useSyncExternalStore(subscribeToasts, getToastsSnapshot);
  const infoEntries = entries.filter((entry) => entry.tone === 'info');
  const errorEntries = entries.filter((entry) => entry.tone === 'error');

  return (
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
  );
}

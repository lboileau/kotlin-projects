import { useSyncExternalStore } from 'react';
import { Callout } from '@radix-ui/themes';
import { Cross2Icon } from '@radix-ui/react-icons';
import { dismissToast, getToastsSnapshot, subscribeToasts } from '../lib/toastStore';
import './Toast.css';

/**
 * App-wide toast region, positioned above the tab bar. Rendered once at
 * the app root; call `toast.error(msg)` / `toast.info(msg)` from
 * `lib/toastStore` to show one, from anywhere (including outside React,
 * e.g. the query client's mutation cache).
 */
export function ToastRegion() {
  const entries = useSyncExternalStore(subscribeToasts, getToastsSnapshot);

  if (entries.length === 0) return null;

  return (
    <div className="toast-region" role="status" aria-live="polite">
      {entries.map((entry) => (
        <Callout.Root
          key={entry.id}
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
      ))}
    </div>
  );
}

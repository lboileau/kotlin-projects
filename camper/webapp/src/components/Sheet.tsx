import type { ReactNode } from 'react';
import * as Dialog from '@radix-ui/react-dialog';
import { Theme } from '@radix-ui/themes';
import { Cross1Icon } from '@radix-ui/react-icons';
import './Sheet.css';

interface SheetProps {
  title: string;
  /** From useSheet() — false plays the exit animation before the route actually changes. */
  open: boolean;
  onClose: () => void;
  /** Grows to ~90dvh height instead of hugging its content (e.g. the recipe picker). */
  fullHeight?: boolean;
  children: ReactNode;
}

/**
 * Bottom-anchored sheet over the Radix Dialog primitive. Purely
 * presentational — `useSheet` owns `open` and what closing means
 * (go back vs. replace to the parent), so every close (the sheet's own
 * X button / overlay tap / Escape, or a page calling `close()` directly
 * after a successful mutation) goes through the exact same animated
 * path with nothing sheet-specific for a page to wire up.
 */
export function Sheet({ title, open, onClose, fullHeight = false, children }: SheetProps) {
  return (
    <Dialog.Root
      open={open}
      onOpenChange={(next) => {
        if (!next) onClose();
      }}
    >
      <Dialog.Portal>
        {/*
          Dialog.Portal renders outside the app tree, into document.body —
          past the root Theme's DOM subtree, so none of the accent, gray
          or panel color tokens it defines would otherwise reach this
          content. A bare nested Theme re-attaches to that context
          (inherits accentColor/grayColor/radius/scaling from the
          ambient ThemeContext) without painting its own opaque
          background or imposing any size — hasBackground={false} is
          also Radix's own default for a nested Theme with no explicit
          appearance, but it's set explicitly here so this doesn't
          silently regress on a Radix Themes upgrade.
        */}
        <Theme hasBackground={false}>
          <Dialog.Overlay className="sheet-overlay" />
          <Dialog.Content
            className={`sheet-content${fullHeight ? ' sheet-content--full' : ''}`}
          >
            <div className="sheet-content__handle" aria-hidden="true" />
            <div className="sheet-content__header">
              <Dialog.Title className="sheet-content__title">{title}</Dialog.Title>
              <Dialog.Close asChild>
                <button type="button" className="sheet-content__close" aria-label="Close">
                  <Cross1Icon />
                </button>
              </Dialog.Close>
            </div>
            <Dialog.Description className="sr-only">{title}</Dialog.Description>
            <div className="sheet-content__body">{children}</div>
          </Dialog.Content>
        </Theme>
      </Dialog.Portal>
    </Dialog.Root>
  );
}

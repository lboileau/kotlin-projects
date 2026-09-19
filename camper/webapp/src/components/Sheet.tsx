import type { ReactNode } from 'react';
import * as Dialog from '@radix-ui/react-dialog';
import { Theme } from '@radix-ui/themes';
import { Cross1Icon } from '@radix-ui/react-icons';
import './Sheet.css';

interface SheetProps {
  title: string;
  onClose: () => void;
  /** Grows to ~90dvh height instead of hugging its content (e.g. the recipe picker). */
  fullHeight?: boolean;
  children: ReactNode;
}

/**
 * Bottom-anchored sheet over the Radix Dialog primitive. Always rendered
 * because its route matched — see `useCloseSheet` for how it decides
 * whether closing should go back in history or replace to its parent.
 */
export function Sheet({ title, onClose, fullHeight = false, children }: SheetProps) {
  return (
    <Dialog.Root
      open
      onOpenChange={(open) => {
        if (!open) onClose();
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

import { forwardRef, type ComponentPropsWithoutRef } from 'react';
import { IconButton } from '@radix-ui/themes';
import './RowActionButton.css';

type RowActionButtonProps = Omit<ComponentPropsWithoutRef<typeof IconButton>, 'size' | 'variant'> & {
  /** No background, just the icon: for secondary row actions (info, delete). */
  quiet?: boolean;
};

/**
 * The icon button at the end of a list row, the same 44px target and icon
 * size everywhere. A row's main action (Recipes' add to plan) is a soft
 * square; secondary ones are `quiet` — the icon alone, no background — in
 * the accent for info and `color="red"` for "take it away" (remove from a
 * plan, remove a manual item, delete a line), so a long list isn't a column
 * of coloured squares. One component so the three tabs' rows can't drift
 * apart again — they had three different styles.
 */
export const RowActionButton = forwardRef<HTMLButtonElement, RowActionButtonProps>(function RowActionButton(
  { className, type = 'button', quiet = false, ...props },
  ref,
) {
  return (
    <IconButton
      ref={ref}
      type={type}
      size="3"
      variant={quiet ? 'ghost' : 'soft'}
      className={`row-action-button${quiet ? ' row-action-button--quiet' : ''}${className ? ` ${className}` : ''}`}
      {...props}
    />
  );
});

import type { KeyboardEvent } from 'react';

/**
 * `onKeyDown` for a long form whose Save button is the only way to submit.
 * Browsers submit a form when Enter is pressed in any text input ("implicit
 * submission"), and `enterKeyHint="next"` only relabels the phone keyboard's
 * key — so Enter in the Name field used to save a recipe with nothing else
 * filled in. Here Enter in a text input moves to the next field instead.
 *
 * Inputs that handle Enter themselves (the ingredient picker, a line's
 * quantity) run first and call preventDefault; those are left alone.
 * Textareas keep Enter as a newline.
 */
export function enterMovesOn(event: KeyboardEvent<HTMLFormElement>): void {
  if (event.key !== 'Enter' || event.defaultPrevented) return;
  const target = event.target;
  if (!(target instanceof HTMLInputElement)) return;
  event.preventDefault();

  const fields = Array.from(
    event.currentTarget.querySelectorAll<HTMLElement>('input:not([disabled]), textarea:not([disabled])'),
  );
  const next = fields[fields.indexOf(target) + 1];
  next?.focus();
}

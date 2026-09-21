// iOS only opens the on-screen keyboard for a focus that happens inside the
// tap itself. A sheet's field is focused when the sheet mounts — a moment
// after the tap that navigated to it — so on a phone it took focus (its value
// even showed as selected) with no keyboard. iOS does keep an open keyboard
// up when focus moves from one field to another, so: call this from the tap
// handler to focus a throwaway, invisible field at once, and let the sheet's
// own autoFocus take over from it when it mounts.

const GIVE_UP_AFTER_MS = 1000;

export function primeKeyboard(): void {
  const field = document.createElement('input');
  field.type = 'text';
  field.inputMode = 'text';
  field.tabIndex = -1;
  field.setAttribute('aria-hidden', 'true');
  // Fixed at the top so focusing it scrolls nothing; 16px so iOS doesn't zoom.
  field.style.cssText =
    'position:fixed;top:0;left:0;width:1px;height:1px;padding:0;border:0;opacity:0;font-size:16px;pointer-events:none;';

  const remove = () => {
    window.clearTimeout(timer);
    field.remove();
  };
  // Gone as soon as the real field takes focus, or if nothing ever does.
  field.addEventListener('blur', remove, { once: true });
  const timer = window.setTimeout(remove, GIVE_UP_AFTER_MS);

  document.body.appendChild(field);
  field.focus({ preventScroll: true });
}

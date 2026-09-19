/**
 * React Router's `location.key` is NOT a reliable "was this the entry
 * point" signal: it is `'default'` only for the entry the page loaded
 * on, and ANY navigation — including `replace` — mints a fresh key.
 * Concretely: a signed-out user deep-links to a sheet -> RequireAuth
 * `replace`s to `/sign-in?next=...` (fresh key) -> sign-in `replace`s
 * back to the sheet (fresh key again) -> the sheet's key looks like it
 * was reached by in-app navigation even though the user never pushed
 * anything, so going back would leave the app.
 *
 * The history entry's index doesn't have this problem: `replace` keeps
 * the current index, `push` increments it, and — critically — the
 * browser persists `history.state` (and therefore the index) across a
 * reload of the same entry. So "is there really a previous entry to go
 * back to in this tab" is answered by `index > 0`, not by the key.
 * React Router's own `createBrowserHistory` tracks the exact same
 * `state.idx` internally, so reading it here mirrors what the router
 * already relies on.
 */
export function getHistoryIndex(): number | null {
  const state = window.history.state as { idx?: unknown } | null;
  const idx = state?.idx;
  return typeof idx === 'number' ? idx : null;
}

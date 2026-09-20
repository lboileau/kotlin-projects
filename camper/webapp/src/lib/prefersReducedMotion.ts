/** Reads live each call (not cached) — cheap, and correctly reflects a user toggling the OS setting mid-session. */
export function prefersReducedMotion(): boolean {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

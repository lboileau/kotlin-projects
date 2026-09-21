/**
 * A fake slow connection, to feel what loading states and optimistic updates
 * are like on a bad network. Open any page with `?debug=slow` to delay every
 * API request by 2 seconds, or `?debug=slow&ms=5000` for another delay. It
 * stays on for the browser session (so it survives navigation and reloads);
 * `?debug=off` turns it off. The delay is added before the request is sent,
 * in `api/http.ts`, so it covers every call the app makes. The live-sync
 * WebSocket is not delayed.
 */
const FLAG = 'meal-planner.debug-slow-ms';
const DEFAULT_DELAY_MS = 2000;

let delayMs = 0;

export function installSlowNetwork(): void {
  try {
    const params = new URLSearchParams(window.location.search);
    const param = params.get('debug');
    if (param === 'slow') {
      const requested = Number(params.get('ms'));
      sessionStorage.setItem(FLAG, String(requested > 0 ? requested : DEFAULT_DELAY_MS));
    }
    if (param === 'off') sessionStorage.removeItem(FLAG);
    delayMs = Number(sessionStorage.getItem(FLAG)) || 0;
  } catch {
    delayMs = 0;
  }
  if (delayMs > 0) console.info(`[debug] Slow network on: every API request is delayed ${delayMs}ms. ?debug=off turns it off.`);
}

/** Resolves after the debug delay, or immediately when it is off. */
export function slowNetworkDelay(): Promise<void> {
  if (delayMs <= 0) return Promise.resolve();
  return new Promise((resolve) => window.setTimeout(resolve, delayMs));
}

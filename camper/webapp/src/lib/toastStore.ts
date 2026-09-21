// Tiny pub-sub store for the app-wide toast region. Kept as a plain
// module (no React) so it can be called from anywhere — including
// TanStack Query's mutation cache, which is created outside React.

export interface ToastAction {
  label: string;
  onClick: () => void;
}

export interface ToastEntry {
  id: number;
  tone: 'error' | 'info';
  message: string;
  action?: ToastAction;
}

let toasts: ToastEntry[] = [];
let nextId = 1;
const listeners = new Set<() => void>();

function emit(): void {
  for (const listener of listeners) listener();
}

export function subscribeToasts(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function getToastsSnapshot(): ToastEntry[] {
  return toasts;
}

export function dismissToast(id: number): void {
  toasts = toasts.filter((entry) => entry.id !== id);
  emit();
}

function pushToast(tone: ToastEntry['tone'], message: string, action?: ToastAction): void {
  const id = nextId++;
  // A newer toast replaces any other of the same tone still showing,
  // rather than stacking on top of it — a rapid flow (e.g. adding several
  // ingredients back to back) can otherwise fire one info toast every
  // second or two, piling several up before any of them expire. Info and
  // error stay independent, matching the two separate live regions. The
  // replaced toast's own dismiss timeout still fires later, harmlessly —
  // `dismissToast` filtering an id that's already gone is a no-op.
  // A toast that carries an action (Undo, Open) is not replaced by a newer
  // toast: removing two recipes in a row must not take away the first one's
  // Undo. They are capped at two on screen, oldest dropped first, so removing
  // five recipes quickly does not stack five.
  const kept = toasts.filter((entry) => entry.tone !== tone || entry.action);
  const keptActions = kept.filter((entry) => entry.action);
  const overflow = new Set(keptActions.slice(0, Math.max(0, keptActions.length - (action ? 1 : 2))).map((entry) => entry.id));
  toasts = [...kept.filter((entry) => !overflow.has(entry.id)), { id, tone, message, action }];
  emit();
  // A toast with an action stays longer: there has to be time to read it, decide, and reach Undo.
  window.setTimeout(() => dismissToast(id), action ? 7000 : tone === 'error' ? 6000 : 4000);
}

export const toast = {
  error(message: string, action?: ToastAction): void {
    pushToast('error', message, action);
  },
  info(message: string, action?: ToastAction): void {
    pushToast('info', message, action);
  },
};

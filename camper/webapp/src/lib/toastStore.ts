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
  toasts = [...toasts, { id, tone, message, action }];
  emit();
  window.setTimeout(() => dismissToast(id), tone === 'error' ? 6000 : 4000);
}

export const toast = {
  error(message: string, action?: ToastAction): void {
    pushToast('error', message, action);
  },
  info(message: string, action?: ToastAction): void {
    pushToast('info', message, action);
  },
};

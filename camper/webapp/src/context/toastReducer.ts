// Pure reducer for the global toast/snackbar system.
// Extracted from ToastContext so it can be unit-tested in isolation.

export type ToastVariant = 'success' | 'error' | 'info';

export interface ToastAction {
  label: string;
  onClick: () => void;
}

export interface ToastOptions {
  durationMs?: number;
  action?: ToastAction;
  id?: string;
}

export interface ToastItem {
  id: string;
  message: string;
  variant: ToastVariant;
  durationMs: number;
  action?: ToastAction;
  paused: boolean;
}

export const DEFAULT_DURATION = 4000;
export const MAX_VISIBLE = 4;

export type ToastStateAction =
  | { type: 'ADD'; payload: ToastItem }
  | { type: 'DISMISS'; payload: { id: string } }
  | { type: 'PAUSE'; payload: { id: string } }
  | { type: 'RESUME'; payload: { id: string } };

export interface ToastState {
  toasts: ToastItem[];
}

export function toastReducer(state: ToastState, action: ToastStateAction): ToastState {
  switch (action.type) {
    case 'ADD': {
      const next = [...state.toasts, action.payload];
      if (next.length > MAX_VISIBLE) {
        // Search only the existing slots (all but the newly-appended last element)
        // for the oldest non-paused toast to evict.  Fall back to the absolute
        // oldest if every existing toast is paused.
        const existingCount = next.length - 1;
        let evictIdx = -1;
        for (let i = 0; i < existingCount; i++) {
          if (!next[i].paused) { evictIdx = i; break; }
        }
        next.splice(evictIdx !== -1 ? evictIdx : 0, 1);
      }
      return { toasts: next };
    }
    case 'DISMISS': {
      return { toasts: state.toasts.filter(t => t.id !== action.payload.id) };
    }
    case 'PAUSE': {
      return {
        toasts: state.toasts.map(t =>
          t.id === action.payload.id ? { ...t, paused: true } : t,
        ),
      };
    }
    case 'RESUME': {
      return {
        toasts: state.toasts.map(t =>
          t.id === action.payload.id ? { ...t, paused: false } : t,
        ),
      };
    }
    default:
      return state;
  }
}

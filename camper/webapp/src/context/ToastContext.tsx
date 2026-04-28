import {
  createContext,
  useContext,
  useReducer,
  useRef,
  useCallback,
  useEffect,
  type ReactNode,
} from 'react';
import {
  toastReducer,
  DEFAULT_DURATION,
  type ToastItem,
  type ToastVariant,
  type ToastOptions,
} from './toastReducer';

// ─── Public API ─────────────────────────────────────────────────────────────

export interface ToastApi {
  success: (msg: string, opts?: ToastOptions) => string;
  error:   (msg: string, opts?: ToastOptions) => string;
  info:    (msg: string, opts?: ToastOptions) => string;
  dismiss: (id: string) => void;
}

// ─── Internal context value (also used by <Toast /> renderer) ───────────────

interface ToastContextValue extends ToastApi {
  toasts: ToastItem[];
  pause:  (id: string) => void;
  resume: (id: string) => void;
}

export const ToastContext = createContext<ToastContextValue | null>(null);

// ─── Provider ────────────────────────────────────────────────────────────────

export function ToastProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(toastReducer, { toasts: [] });

  // Timer tracking: one entry per active toast
  const timers    = useRef(new Map<string, ReturnType<typeof setTimeout>>());
  const startedAt = useRef(new Map<string, number>());  // when last timer started
  const remaining = useRef(new Map<string, number>());  // ms left when last started

  // Schedule (or reschedule) a dismiss timer for a toast.
  const scheduleTimer = useCallback((id: string, ms: number) => {
    const existing = timers.current.get(id);
    if (existing !== undefined) clearTimeout(existing);

    startedAt.current.set(id, Date.now());
    remaining.current.set(id, ms);

    const handle = setTimeout(() => {
      dispatch({ type: 'DISMISS', payload: { id } });
      timers.current.delete(id);
      startedAt.current.delete(id);
      remaining.current.delete(id);
    }, ms);

    timers.current.set(id, handle);
  }, []);

  // Add a toast and start its auto-dismiss timer.
  const addToast = useCallback(
    (message: string, variant: ToastVariant, opts?: ToastOptions): string => {
      const id = opts?.id ?? crypto.randomUUID();
      const durationMs = opts?.durationMs ?? DEFAULT_DURATION;
      const item: ToastItem = {
        id,
        message,
        variant,
        durationMs,
        action: opts?.action,
        paused: false,
      };
      dispatch({ type: 'ADD', payload: item });
      scheduleTimer(id, durationMs);
      return id;
    },
    [scheduleTimer],
  );

  const success = useCallback((msg: string, opts?: ToastOptions) => addToast(msg, 'success', opts), [addToast]);
  const error   = useCallback((msg: string, opts?: ToastOptions) => addToast(msg, 'error',   opts), [addToast]);
  const info    = useCallback((msg: string, opts?: ToastOptions) => addToast(msg, 'info',    opts), [addToast]);

  const dismiss = useCallback((id: string) => {
    const handle = timers.current.get(id);
    if (handle !== undefined) clearTimeout(handle);
    timers.current.delete(id);
    startedAt.current.delete(id);
    remaining.current.delete(id);
    dispatch({ type: 'DISMISS', payload: { id } });
  }, []);

  // Pause auto-dismiss on hover — record remaining time.
  const pause = useCallback((id: string) => {
    const handle = timers.current.get(id);
    if (handle !== undefined) {
      clearTimeout(handle);
      timers.current.delete(id);
      const started = startedAt.current.get(id);
      const rem     = remaining.current.get(id);
      if (started !== undefined && rem !== undefined) {
        remaining.current.set(id, Math.max(0, rem - (Date.now() - started)));
      }
    }
    dispatch({ type: 'PAUSE', payload: { id } });
  }, []);

  // Resume auto-dismiss after hover — use remaining time.
  const resume = useCallback((id: string) => {
    const rem = remaining.current.get(id) ?? DEFAULT_DURATION;
    scheduleTimer(id, rem);
    dispatch({ type: 'RESUME', payload: { id } });
  }, [scheduleTimer]);

  // Clean up timer refs for toasts the reducer evicted via MAX_VISIBLE overflow.
  useEffect(() => {
    const activeIds = new Set(state.toasts.map(t => t.id));
    timers.current.forEach((handle, id) => {
      if (!activeIds.has(id)) {
        clearTimeout(handle);
        timers.current.delete(id);
        startedAt.current.delete(id);
        remaining.current.delete(id);
      }
    });
  }, [state.toasts]);

  // Clear ALL pending timers on provider unmount.
  useEffect(() => {
    return () => {
      // eslint-disable-next-line react-hooks/exhaustive-deps
      timers.current.forEach(handle => clearTimeout(handle));
    };
  }, []);

  return (
    <ToastContext.Provider
      value={{ toasts: state.toasts, success, error, info, dismiss, pause, resume }}
    >
      {children}
    </ToastContext.Provider>
  );
}

// ─── Hooks ───────────────────────────────────────────────────────────────────

/** Public hook — call from feature components to fire toasts. */
export function useToast(): ToastApi {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within <ToastProvider>');
  return { success: ctx.success, error: ctx.error, info: ctx.info, dismiss: ctx.dismiss };
}

/** Internal hook — used only by the <Toast /> renderer. */
export function useToastContext(): ToastContextValue {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToastContext must be used within <ToastProvider>');
  return ctx;
}

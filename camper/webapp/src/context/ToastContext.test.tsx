import { describe, it, expect, vi, afterEach } from 'vitest';
import { render, screen, fireEvent, act, waitFor } from '@testing-library/react';
import { ToastProvider, useToast } from './ToastContext';
import { Toast } from '../components/ui/Toast';
import { MAX_VISIBLE, DEFAULT_DURATION } from './toastReducer';

// ─── afterEach: restore real timers in case a test left fake timers on ───────

afterEach(() => {
  vi.clearAllTimers();
  vi.useRealTimers();
});

// ─── Helpers ─────────────────────────────────────────────────────────────────

function TestApp({
  msg = 'Hello toast',
  variant = 'success' as 'success' | 'error' | 'info',
  durationMs,
  actionLabel,
  onAction,
}: {
  msg?: string;
  variant?: 'success' | 'error' | 'info';
  durationMs?: number;
  actionLabel?: string;
  onAction?: () => void;
}) {
  return (
    <ToastProvider>
      <ToastConsumer
        msg={msg}
        variant={variant}
        durationMs={durationMs}
        actionLabel={actionLabel}
        onAction={onAction}
      />
      <Toast />
    </ToastProvider>
  );
}

function ToastConsumer({
  msg,
  variant,
  durationMs,
  actionLabel,
  onAction,
}: {
  msg: string;
  variant: 'success' | 'error' | 'info';
  durationMs?: number;
  actionLabel?: string;
  onAction?: () => void;
}) {
  const toast = useToast();
  return (
    <button
      onClick={() =>
        toast[variant](msg, {
          durationMs,
          action: actionLabel && onAction ? { label: actionLabel, onClick: onAction } : undefined,
        })
      }
    >
      Add toast
    </button>
  );
}

function MultiConsumer({ count }: { count: number }) {
  const toast = useToast();
  return (
    <>
      {Array.from({ length: count }, (_, i) => (
        <button key={i + 1} onClick={() => toast.success(`Toast ${i + 1}`)}>
          {`add-${i + 1}`}
        </button>
      ))}
    </>
  );
}

// ─── Tests ────────────────────────────────────────────────────────────────────

describe('ToastContext', () => {
  it('success() renders one toast with the given message', () => {
    render(<TestApp msg="hi there" />);
    fireEvent.click(screen.getByRole('button', { name: 'Add toast' }));
    expect(screen.getByText('hi there')).toBeInTheDocument();
  });

  it('auto-dismisses after durationMs (vi.useFakeTimers)', async () => {
    vi.useFakeTimers();
    render(<TestApp msg="bye" durationMs={2000} />);
    fireEvent.click(screen.getByRole('button', { name: 'Add toast' }));
    expect(screen.getByText('bye')).toBeInTheDocument();

    await act(async () => { vi.advanceTimersByTime(2000); });

    expect(screen.queryByText('bye')).not.toBeInTheDocument();
  });

  it(`stacking ${MAX_VISIBLE} + 1 toasts evicts the first`, () => {
    render(
      <ToastProvider>
        <MultiConsumer count={MAX_VISIBLE + 1} />
        <Toast />
      </ToastProvider>,
    );

    for (let i = 1; i <= MAX_VISIBLE + 1; i++) {
      fireEvent.click(screen.getByRole('button', { name: `add-${i}` }));
    }

    // First toast should be gone; last should be visible
    expect(screen.queryByText('Toast 1')).not.toBeInTheDocument();
    expect(screen.getByText(`Toast ${MAX_VISIBLE + 1}`)).toBeInTheDocument();
    // Total visible ≤ MAX_VISIBLE
    expect(screen.getAllByRole('alert').length).toBeLessThanOrEqual(MAX_VISIBLE);
  });

  it('toast with action renders the action button; clicking dismisses + invokes handler', () => {
    const handler = vi.fn();
    render(<TestApp msg="Do something" actionLabel="Undo" onAction={handler} />);
    fireEvent.click(screen.getByRole('button', { name: 'Add toast' }));

    const actionBtn = screen.getByRole('button', { name: 'Undo' });
    expect(actionBtn).toBeInTheDocument();
    fireEvent.click(actionBtn);

    expect(handler).toHaveBeenCalledOnce();
    expect(screen.queryByText('Do something')).not.toBeInTheDocument();
  });

  it('hovering pauses dismissal; leaving resumes (vi.useFakeTimers)', async () => {
    vi.useFakeTimers();
    render(<TestApp msg="hover me" durationMs={DEFAULT_DURATION} />);
    fireEvent.click(screen.getByRole('button', { name: 'Add toast' }));

    const toastEl = screen.getByRole('alert');

    // Hover — advance past the full duration; toast should still be visible
    fireEvent.mouseEnter(toastEl);
    await act(async () => { vi.advanceTimersByTime(DEFAULT_DURATION + 100); });
    expect(screen.getByText('hover me')).toBeInTheDocument();

    // Un-hover — resume; advance enough for remaining to fire
    fireEvent.mouseLeave(toastEl);
    await act(async () => { vi.advanceTimersByTime(DEFAULT_DURATION + 100); });
    expect(screen.queryByText('hover me')).not.toBeInTheDocument();
  });

  it('dismiss(id) removes the matching toast immediately', async () => {
    render(<TestApp msg="dismissible" />);
    fireEvent.click(screen.getByRole('button', { name: 'Add toast' }));
    expect(screen.getByText('dismissible')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Dismiss notification' }));

    await waitFor(() => {
      expect(screen.queryByText('dismissible')).not.toBeInTheDocument();
    });
  });
});

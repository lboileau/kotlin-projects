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

  it('error() renders a toast with the error CSS class', () => {
    render(<TestApp msg="Something failed" variant="error" />);
    fireEvent.click(screen.getByRole('button', { name: 'Add toast' }));
    const toastEl = screen.getByRole('alert');
    expect(toastEl).toHaveClass('toast-item--error');
    expect(screen.getByText('Something failed')).toBeInTheDocument();
  });

  it('auto-dismisses after durationMs (vi.useFakeTimers)', async () => {
    vi.useFakeTimers();
    render(<TestApp msg="bye" durationMs={2000} />);
    fireEvent.click(screen.getByRole('button', { name: 'Add toast' }));
    expect(screen.getByText('bye')).toBeInTheDocument();

    await act(async () => { vi.advanceTimersByTime(2000); });

    expect(screen.queryByText('bye')).not.toBeInTheDocument();
  });

  it(`stacking ${MAX_VISIBLE} + 1 toasts evicts exactly the first; ${MAX_VISIBLE} remain`, () => {
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
    // Exactly MAX_VISIBLE toasts remain — not fewer
    expect(screen.getAllByRole('alert').length).toBe(MAX_VISIBLE);
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

  it('hovering pauses dismissal; leaving resumes with remaining time (vi.useFakeTimers)', async () => {
    vi.useFakeTimers();
    render(<TestApp msg="hover me" durationMs={DEFAULT_DURATION} />);
    fireEvent.click(screen.getByRole('button', { name: 'Add toast' }));

    const toastEl = screen.getByRole('alert');

    // Advance half the duration — toast still visible
    await act(async () => { vi.advanceTimersByTime(DEFAULT_DURATION / 2); });
    expect(screen.getByText('hover me')).toBeInTheDocument();

    // Hover — timer paused; remaining ≈ DEFAULT_DURATION / 2
    fireEvent.mouseEnter(toastEl);

    // Advance past the ORIGINAL expiry — if pause doesn't work, the toast would
    // be gone; with pause, it must still be present
    await act(async () => { vi.advanceTimersByTime(DEFAULT_DURATION); });
    expect(screen.getByText('hover me')).toBeInTheDocument(); // paused — still there

    // Un-hover — resume with remaining ≈ DEFAULT_DURATION / 2
    fireEvent.mouseLeave(toastEl);

    // Advance slightly less than remaining — should still be visible
    // (proves the timer uses remaining time, not full DEFAULT_DURATION)
    await act(async () => { vi.advanceTimersByTime(DEFAULT_DURATION / 2 - 100); });
    expect(screen.getByText('hover me')).toBeInTheDocument(); // not yet expired

    // Advance past remaining — now dismissed
    await act(async () => { vi.advanceTimersByTime(200); });
    expect(screen.queryByText('hover me')).not.toBeInTheDocument();
  });

  it('close button removes the matching toast immediately', async () => {
    render(<TestApp msg="dismissible" />);
    fireEvent.click(screen.getByRole('button', { name: 'Add toast' }));
    expect(screen.getByText('dismissible')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Dismiss notification' }));

    await waitFor(() => {
      expect(screen.queryByText('dismissible')).not.toBeInTheDocument();
    });
  });

  it('useToast().dismiss(id) programmatically removes the matching toast', async () => {
    let capturedId = '';
    function DismissConsumer() {
      const toast = useToast();
      return (
        <>
          <button onClick={() => { capturedId = toast.success('to dismiss'); }}>Add</button>
          <button onClick={() => toast.dismiss(capturedId)}>Dismiss it</button>
        </>
      );
    }
    render(<ToastProvider><DismissConsumer /><Toast /></ToastProvider>);

    fireEvent.click(screen.getByRole('button', { name: 'Add' }));
    expect(screen.getByText('to dismiss')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Dismiss it' }));
    await waitFor(() => {
      expect(screen.queryByText('to dismiss')).not.toBeInTheDocument();
    });
  });

  it('useToast() throws when called outside <ToastProvider>', () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    function BadComponent() {
      useToast();
      return null;
    }
    expect(() => render(<BadComponent />)).toThrow('useToast must be used within <ToastProvider>');
    consoleSpy.mockRestore();
  });
});

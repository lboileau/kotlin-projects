import { describe, it, expect, vi } from 'vitest';
import {
  toastReducer,
  MAX_VISIBLE,
  DEFAULT_DURATION,
  type ToastState,
  type ToastItem,
} from './toastReducer';

function makeToast(id: string, overrides?: Partial<ToastItem>): ToastItem {
  return {
    id,
    message: `Toast ${id}`,
    variant: 'info',
    durationMs: DEFAULT_DURATION,
    paused: false,
    ...overrides,
  };
}

const empty: ToastState = { toasts: [] };

describe('toastReducer', () => {
  it('ADD appends a toast and assigns its id', () => {
    const toast = makeToast('a1');
    const next = toastReducer(empty, { type: 'ADD', payload: toast });
    expect(next.toasts).toHaveLength(1);
    expect(next.toasts[0].id).toBe('a1');
    expect(next.toasts[0].message).toBe('Toast a1');
  });

  it('DISMISS removes the toast with the given id', () => {
    const state: ToastState = { toasts: [makeToast('x'), makeToast('y')] };
    const next = toastReducer(state, { type: 'DISMISS', payload: { id: 'x' } });
    expect(next.toasts).toHaveLength(1);
    expect(next.toasts[0].id).toBe('y');
  });

  it('DISMISS is a no-op for an unknown id', () => {
    const state: ToastState = { toasts: [makeToast('x')] };
    const next = toastReducer(state, { type: 'DISMISS', payload: { id: 'nope' } });
    expect(next.toasts).toHaveLength(1);
  });

  it(`ADD past MAX_VISIBLE (${MAX_VISIBLE}) evicts the oldest non-paused toast`, () => {
    // Fill up to MAX_VISIBLE
    let state: ToastState = empty;
    for (let i = 1; i <= MAX_VISIBLE; i++) {
      state = toastReducer(state, { type: 'ADD', payload: makeToast(`t${i}`) });
    }
    expect(state.toasts).toHaveLength(MAX_VISIBLE);

    // Adding one more should evict the first (oldest) non-paused toast
    state = toastReducer(state, { type: 'ADD', payload: makeToast('t5') });
    expect(state.toasts).toHaveLength(MAX_VISIBLE);
    // Oldest (t1) should be gone; newest (t5) should be present
    expect(state.toasts.map(t => t.id)).not.toContain('t1');
    expect(state.toasts.map(t => t.id)).toContain('t5');
  });

  it('ADD past MAX_VISIBLE falls back to evicting oldest when all are paused', () => {
    // Fill with all paused
    const pausedToasts: ToastItem[] = Array.from({ length: MAX_VISIBLE }, (_, i) =>
      makeToast(`p${i}`, { paused: true }),
    );
    const state: ToastState = { toasts: pausedToasts };

    const next = toastReducer(state, { type: 'ADD', payload: makeToast('pNew') });
    expect(next.toasts).toHaveLength(MAX_VISIBLE);
    // Oldest (p0) evicted
    expect(next.toasts.map(t => t.id)).not.toContain('p0');
    expect(next.toasts.map(t => t.id)).toContain('pNew');
  });

  it('PAUSE sets paused=true for the matching toast and leaves others unchanged', () => {
    const state: ToastState = { toasts: [makeToast('a'), makeToast('b')] };
    const next = toastReducer(state, { type: 'PAUSE', payload: { id: 'a' } });
    const a = next.toasts.find(t => t.id === 'a')!;
    const b = next.toasts.find(t => t.id === 'b')!;
    expect(a.paused).toBe(true);
    expect(b.paused).toBe(false);
  });

  it('RESUME sets paused=false for the matching toast', () => {
    const state: ToastState = {
      toasts: [makeToast('a', { paused: true }), makeToast('b', { paused: true })],
    };
    const next = toastReducer(state, { type: 'RESUME', payload: { id: 'a' } });
    const a = next.toasts.find(t => t.id === 'a')!;
    const b = next.toasts.find(t => t.id === 'b')!;
    expect(a.paused).toBe(false);
    expect(b.paused).toBe(true); // unchanged
  });

  it('ADD preserves the action field intact through the reducer', () => {
    const onClick = vi.fn();
    const toast = makeToast('a1', { action: { label: 'Undo', onClick } });
    const next = toastReducer(empty, { type: 'ADD', payload: toast });
    expect(next.toasts[0].action).toBeDefined();
    expect(next.toasts[0].action?.label).toBe('Undo');
    expect(next.toasts[0].action?.onClick).toBe(onClick);
  });
});

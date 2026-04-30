/**
 * W15 — GearPacksPanel: gear pack apply summary + Undo
 *
 * Verifies:
 *  (1) Apply pack succeeds → toast.success called with 'Added N items from <packName>'
 *      and action: { label: 'Undo', onClick: <fn> }
 *  (2) Click Undo → api.deleteItem called for each item ID, in order
 *  (3) After Undo: onItemsChanged called AND toast.info 'Undid N items from <packName>'
 *  (4) Without clicking Undo, no api.deleteItem calls happen
 *  (5) Inline applySuccess flash UI is absent (regression guard against re-introduction)
 *
 * Per W12 lesson: also includes one full-component integration test exercising the
 * production handleApply handler (not a wrapper).
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { GearPacksPanel } from './GearPacksPanel';
import type { GearPackSummary, Item } from '../api/client';

// ─── Mock useToast (vi.hoisted so vi.mock can close over it) ──────────────────

const mockToast = vi.hoisted(() => ({
  success: vi.fn(),
  error:   vi.fn(),
  info:    vi.fn(),
  dismiss: vi.fn(),
}));

vi.mock('../context/ToastContext', () => ({
  useToast: () => mockToast,
}));

// ─── Mock api ─────────────────────────────────────────────────────────────────

const mockApi = vi.hoisted(() => ({
  getGearPacks:   vi.fn(),
  applyGearPack:  vi.fn(),
  deleteItem:     vi.fn(),
}));

vi.mock('../api/client', async (importOriginal) => {
  const original = await importOriginal<typeof import('../api/client')>();
  return { ...original, api: mockApi };
});

// ─── Fixtures ─────────────────────────────────────────────────────────────────

const PACK: GearPackSummary = {
  id: 'pack-1',
  name: 'Cooking Pack',
  description: 'Essential kitchen gear',
  itemCount: 3,
  createdBy: 'user-1',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

function makeItem(id: string): Item {
  return {
    id,
    planId: 'plan-1',
    userId: null,
    name: `Item ${id}`,
    category: 'kitchen',
    quantity: 1,
    packed: false,
    gearPackId: 'pack-1',
    gearPackName: 'Cooking Pack',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  };
}

const ITEMS = [makeItem('item-1'), makeItem('item-2'), makeItem('item-3')];

const APPLY_RESULT = { appliedCount: 3, items: ITEMS };

function makeProps(overrides?: Partial<Parameters<typeof GearPacksPanel>[0]>) {
  return {
    planId: 'plan-1',
    memberCount: 4,
    canEdit: true,
    onItemsChanged: vi.fn(),
    currentUserId: 'user-1',
    ...overrides,
  };
}

/** Renders GearPacksPanel, expands it, and waits for the pack card to appear. */
async function renderAndExpand(overrides?: Partial<Parameters<typeof GearPacksPanel>[0]>) {
  const user = userEvent.setup();
  const props = makeProps(overrides);
  const { rerender } = render(<GearPacksPanel {...props} />);

  // Expand the panel
  await user.click(screen.getByRole('button', { name: /gear packs/i }));

  // Wait for the pack name to appear (getGearPacks resolved)
  await screen.findByText(PACK.name);

  return { user, props, rerender };
}

// ─── Tests ────────────────────────────────────────────────────────────────────

describe('GearPacksPanel — apply + Undo (W15)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockApi.getGearPacks.mockResolvedValue([PACK]);
    mockApi.applyGearPack.mockResolvedValue(APPLY_RESULT);
    mockApi.deleteItem.mockResolvedValue(undefined);
  });

  // (1) Apply pack → toast.success with correct message and Undo action
  it('apply pack → toast.success with item count and Undo action label', async () => {
    const { user } = await renderAndExpand();

    await user.click(screen.getByRole('button', { name: 'Apply' }));

    await waitFor(() => {
      expect(mockToast.success).toHaveBeenCalledOnce();
    });

    const [message, opts] = mockToast.success.mock.calls[0] as [string, { action?: { label: string; onClick: () => void } }];
    expect(message).toBe('Added 3 items from Cooking Pack');
    expect(opts).toEqual(
      expect.objectContaining({
        action: expect.objectContaining({ label: 'Undo' }),
      }),
    );
  });

  // (2) Click Undo → api.deleteItem called for each item ID in order
  it('invoking the Undo action calls api.deleteItem for each item ID in order', async () => {
    const { user } = await renderAndExpand();

    await user.click(screen.getByRole('button', { name: 'Apply' }));

    await waitFor(() => expect(mockToast.success).toHaveBeenCalledOnce());

    const [, opts] = mockToast.success.mock.calls[0] as [string, { action: { label: string; onClick: () => void } }];
    const undoOnClick = opts.action.onClick;

    // Invoke the undo handler
    await undoOnClick();

    await waitFor(() => {
      expect(mockApi.deleteItem).toHaveBeenCalledTimes(3);
      expect(mockApi.deleteItem).toHaveBeenNthCalledWith(1, 'item-1');
      expect(mockApi.deleteItem).toHaveBeenNthCalledWith(2, 'item-2');
      expect(mockApi.deleteItem).toHaveBeenNthCalledWith(3, 'item-3');
    });
  });

  // (3) After Undo: onItemsChanged called AND toast.info fired
  it('after Undo: onItemsChanged called and toast.info fires with Undid message', async () => {
    const onItemsChanged = vi.fn();
    const { user } = await renderAndExpand({ onItemsChanged });

    await user.click(screen.getByRole('button', { name: 'Apply' }));

    await waitFor(() => expect(mockToast.success).toHaveBeenCalledOnce());

    const [, opts] = mockToast.success.mock.calls[0] as [string, { action: { label: string; onClick: () => void } }];
    const undoOnClick = opts.action.onClick;

    await undoOnClick();

    await waitFor(() => {
      // onItemsChanged called once after apply and once after undo = 2 total
      expect(onItemsChanged).toHaveBeenCalledTimes(2);
      expect(mockToast.info).toHaveBeenCalledOnce();
      expect(mockToast.info).toHaveBeenCalledWith('Undid 3 items from Cooking Pack');
    });
  });

  // (4) Without clicking Undo, no deleteItem calls happen
  it('without invoking Undo, api.deleteItem is never called', async () => {
    const { user } = await renderAndExpand();

    await user.click(screen.getByRole('button', { name: 'Apply' }));

    await waitFor(() => expect(mockToast.success).toHaveBeenCalledOnce());

    // Do NOT invoke action.onClick
    expect(mockApi.deleteItem).not.toHaveBeenCalled();
  });

  // (5) Inline applySuccess flash UI is absent (regression guard)
  it('inline applySuccess flash is not rendered after apply', async () => {
    const { user } = await renderAndExpand();

    await user.click(screen.getByRole('button', { name: 'Apply' }));

    await waitFor(() => expect(mockToast.success).toHaveBeenCalledOnce());

    // The old class and text must not appear in the DOM
    expect(document.querySelector('.gear-packs-success')).toBeNull();
    expect(screen.queryByText(/items to your gear list/i)).not.toBeInTheDocument();
  });
});

// ─── Full-component integration test (W12 lesson) ────────────────────────────

describe('GearPacksPanel — full integration: apply wires to api.applyGearPack + toast (W15)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockApi.getGearPacks.mockResolvedValue([PACK]);
    mockApi.applyGearPack.mockResolvedValue(APPLY_RESULT);
    mockApi.deleteItem.mockResolvedValue(undefined);
  });

  it('full component: clicking Apply calls api.applyGearPack with planId and groupSize', async () => {
    const user = userEvent.setup();
    render(
      <GearPacksPanel
        planId="plan-1"
        memberCount={4}
        canEdit
        onItemsChanged={vi.fn()}
        currentUserId="user-1"
      />,
    );

    // Expand panel
    await user.click(screen.getByRole('button', { name: /gear packs/i }));
    await screen.findByText(PACK.name);

    // Click Apply
    await user.click(screen.getByRole('button', { name: 'Apply' }));

    await waitFor(() => {
      expect(mockApi.applyGearPack).toHaveBeenCalledOnce();
      expect(mockApi.applyGearPack).toHaveBeenCalledWith('pack-1', {
        planId: 'plan-1',
        groupSize: 4,
      });
      expect(mockToast.success).toHaveBeenCalledOnce();
    });
  });

  it('full component: Undo handler loops api.deleteItem and fires toast.info', async () => {
    const user = userEvent.setup();
    render(
      <GearPacksPanel
        planId="plan-1"
        memberCount={4}
        canEdit
        onItemsChanged={vi.fn()}
        currentUserId="user-1"
      />,
    );

    await user.click(screen.getByRole('button', { name: /gear packs/i }));
    await screen.findByText(PACK.name);
    await user.click(screen.getByRole('button', { name: 'Apply' }));

    await waitFor(() => expect(mockToast.success).toHaveBeenCalledOnce());

    const [, opts] = mockToast.success.mock.calls[0] as [string, { action: { label: string; onClick: () => void } }];
    await opts.action.onClick();

    await waitFor(() => {
      expect(mockApi.deleteItem).toHaveBeenCalledTimes(3);
      expect(mockToast.info).toHaveBeenCalledWith('Undid 3 items from Cooking Pack');
    });
  });
});

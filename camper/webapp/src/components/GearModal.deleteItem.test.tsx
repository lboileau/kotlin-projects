/**
 * W23 — GearModal item delete: hover-revealed icon → ConfirmModal
 *
 * Verifies:
 *  (1) Clicking the delete button does NOT immediately call onDelete.
 *  (2) A ConfirmModal appears after clicking the delete button.
 *  (3) Confirming the modal calls onDelete with the correct item.
 *  (4) Cancelling the modal does NOT call onDelete.
 *  (5) canEdit=false renders no delete button (prop guard).
 *  (6) Full GearModal integration: confirming delete calls api.deleteItem with the correct item id.
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ItemRow, GearModal } from './GearModal';
import type { CategoryDef } from './GearModal';
import type { Item } from '../api/client';

// ─── Mock useToast (required by GearModal's AddItemForm, not by ItemRow directly) ─

const mockToast = vi.hoisted(() => ({
  success: vi.fn(),
  error:   vi.fn(),
  info:    vi.fn(),
  dismiss: vi.fn(),
}));

vi.mock('../context/ToastContext', () => ({
  useToast: () => mockToast,
}));

// ─── Mock api (required for full GearModal integration test) ──────────────────

const mockApi = vi.hoisted(() => ({
  getItems:   vi.fn(),
  deleteItem: vi.fn(),
}));

vi.mock('../api/client', async (importOriginal) => {
  const original = await importOriginal<typeof import('../api/client')>();
  return { ...original, api: mockApi };
});

// ─── Fixtures ─────────────────────────────────────────────────────────────────

const CATEGORIES: CategoryDef[] = [
  { value: 'camp', label: 'Camp', icon: '△' },
  { value: 'kitchen', label: 'Kitchen', icon: '◉' },
];

const ITEM: Item = {
  id: 'item-1',
  name: 'Tent pegs',
  category: 'camp',
  quantity: 4,
  packed: false,
  planId: 'plan-1',
  userId: null,
  gearPackId: null,
  gearPackName: null,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

function makeProps(overrides?: Partial<Parameters<typeof ItemRow>[0]>) {
  return {
    item: ITEM,
    canEdit: true,
    categories: CATEGORIES,
    onTogglePacked: vi.fn(),
    onDelete: vi.fn().mockResolvedValue(undefined),
    onUpdate: vi.fn(),
    ...overrides,
  };
}

// ─── Tests ────────────────────────────────────────────────────────────────────

describe('ItemRow — delete via ConfirmModal (W23)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('clicking the delete button does NOT immediately call onDelete', async () => {
    const user = userEvent.setup();
    const onDelete = vi.fn().mockResolvedValue(undefined);
    render(<ItemRow {...makeProps({ onDelete })} />);

    const deleteBtn = screen.getByRole('button', { name: /Remove Tent pegs/i });
    await user.click(deleteBtn);

    // onDelete should NOT have been called yet
    expect(onDelete).not.toHaveBeenCalled();
  });

  it('shows ConfirmModal after clicking the delete button', async () => {
    const user = userEvent.setup();
    render(<ItemRow {...makeProps()} />);

    const deleteBtn = screen.getByRole('button', { name: /Remove Tent pegs/i });
    await user.click(deleteBtn);

    // ConfirmModal should now be visible with title, action buttons
    expect(screen.getByRole('heading', { name: /Remove this item\?/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^Remove$/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Cancel/i })).toBeInTheDocument();
    // The modal message mentions the item name (verify via confirm-modal container)
    expect(document.querySelector('.confirm-modal__message')?.textContent).toContain('Tent pegs');
  });

  it('confirming calls onDelete with the correct item', async () => {
    const user = userEvent.setup();
    const onDelete = vi.fn().mockResolvedValue(undefined);
    render(<ItemRow {...makeProps({ onDelete })} />);

    await user.click(screen.getByRole('button', { name: /Remove Tent pegs/i }));
    await user.click(screen.getByRole('button', { name: /^Remove$/i }));

    await waitFor(() => {
      expect(onDelete).toHaveBeenCalledOnce();
      expect(onDelete).toHaveBeenCalledWith(ITEM);
    });
  });

  it('cancelling the confirm modal does NOT call onDelete', async () => {
    const user = userEvent.setup();
    const onDelete = vi.fn().mockResolvedValue(undefined);
    render(<ItemRow {...makeProps({ onDelete })} />);

    await user.click(screen.getByRole('button', { name: /Remove Tent pegs/i }));

    // Modal is open — cancel
    await user.click(screen.getByRole('button', { name: /Cancel/i }));

    expect(onDelete).not.toHaveBeenCalled();
    // Modal should be dismissed
    expect(screen.queryByText(/Remove this item\?/i)).not.toBeInTheDocument();
  });

  it('canEdit=false renders no delete button', () => {
    render(<ItemRow {...makeProps({ canEdit: false })} />);
    expect(screen.queryByRole('button', { name: /Remove/i })).not.toBeInTheDocument();
  });
});

// ─── Full GearModal integration test ─────────────────────────────────────────

describe('GearModal — full integration: delete flow wires to api.deleteItem (W23)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockApi.getItems.mockResolvedValue([ITEM]);
    mockApi.deleteItem.mockResolvedValue(undefined);
  });

  it('full GearModal: confirming delete calls api.deleteItem with correct item id', async () => {
    const user = userEvent.setup();
    render(
      <GearModal
        isOpen
        planId="plan-1"
        planOwnerId="owner-1"
        currentUserId="owner-1"
        members={[]}
        onClose={vi.fn()}
      />
    );

    // Wait for the item to appear after initial load
    await screen.findByText(ITEM.name);

    // Click the delete (Remove) button on the item row
    await user.click(screen.getByRole('button', { name: new RegExp(`Remove ${ITEM.name}`, 'i') }));

    // Confirm the ConfirmModal
    await user.click(screen.getByRole('button', { name: /^Remove$/i }));

    await waitFor(() => {
      expect(mockApi.deleteItem).toHaveBeenCalledWith(ITEM.id);
    });
  });
});

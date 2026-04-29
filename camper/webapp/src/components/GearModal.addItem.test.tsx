/**
 * W3 — GearModal AddItemForm keep-open behaviour
 *
 * Verifies:
 *  (1) Submit gear item → name cleared, quantity preserved, name input refocused.
 *  (2) Toast fired with the added name and 1500 ms duration.
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AddItemForm } from './GearModal';
import type { CategoryDef } from './GearModal';

// ─── Mock useToast ─────────────────────────────────────────────────────────────

const mockToast = vi.hoisted(() => ({
  success: vi.fn(),
  error:   vi.fn(),
  info:    vi.fn(),
  dismiss: vi.fn(),
}));

vi.mock('../context/ToastContext', () => ({
  useToast: () => mockToast,
}));

// ─── Fixtures ─────────────────────────────────────────────────────────────────

const categories: CategoryDef[] = [
  { value: 'camp', label: 'Camp', icon: '△' },
  { value: 'kitchen', label: 'Kitchen', icon: '◉' },
];

function makeProps(overrides?: Partial<Parameters<typeof AddItemForm>[0]>) {
  return {
    categories,
    placeholder: 'Add gear item...',
    onAdd: vi.fn().mockResolvedValue(undefined),
    ...overrides,
  };
}

// ─── Tests ────────────────────────────────────────────────────────────────────

describe('AddItemForm — keep-open (W3)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('submit: name cleared, quantity preserved, name input NOT reset to 1', async () => {
    const user = userEvent.setup();
    render(<AddItemForm {...makeProps()} />);

    const nameInput = screen.getByPlaceholderText('Add gear item...');

    // Increment qty to 3 using the "+" qty button (type="button")
    const qtyIncBtn = screen.getAllByRole('button', { name: '+' })
      .find(b => b.getAttribute('type') === 'button')!;
    await user.click(qtyIncBtn);
    await user.click(qtyIncBtn);
    // qty should be 3 now
    expect(screen.getByText('3')).toBeInTheDocument();

    // Type a name
    await user.type(nameInput, 'Tent pegs');

    // Submit via Enter key on the name input
    await user.keyboard('{Enter}');

    // Name is cleared after submit and input is refocused
    await waitFor(() => {
      expect(nameInput).toHaveValue('');
      expect(document.activeElement).toBe(nameInput);
    });

    // Quantity is preserved at 3 — not reset to 1 (W3)
    expect(screen.getByText('3')).toBeInTheDocument();
  });

  it('submit: toast fired with added name and 1500 ms duration', async () => {
    const user = userEvent.setup();
    render(<AddItemForm {...makeProps()} />);

    const nameInput = screen.getByPlaceholderText('Add gear item...');
    await user.type(nameInput, 'Sleeping bag');

    // Submit via the submit button (type="submit")
    const submitBtn = screen.getAllByRole('button', { name: '+' })
      .find(b => b.getAttribute('type') === 'submit')!;
    await user.click(submitBtn);

    await waitFor(() => {
      expect(mockToast.success).toHaveBeenCalledWith(
        'Added "Sleeping bag"',
        { durationMs: 1500 },
      );
    });
  });
});

/**
 * W3 — AddMemberModal keep-open behaviour
 *
 * Verifies:
 *  (1) Single-row submit success → emails reset to [''], modal stays open, first input refocused.
 *  (2) Done button calls onClose.
 *  (3) Partial-failure path still filters to failed rows (existing behaviour preserved).
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AddMemberModal } from './AddMemberModal';

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

// ─── Props factory ────────────────────────────────────────────────────────────

function makeProps(overrides?: Partial<{ onAdd: ReturnType<typeof vi.fn>; onClose: ReturnType<typeof vi.fn> }>) {
  return {
    isOpen: true,
    onClose: vi.fn(),
    onAdd: vi.fn().mockResolvedValue(undefined),
    ...overrides,
  };
}

// ─── Tests ────────────────────────────────────────────────────────────────────

describe('AddMemberModal — keep-open (W3)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('single-row success: emails reset, modal stays open, first input focused', async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();
    render(<AddMemberModal {...makeProps({ onClose })} />);

    const emailInput = screen.getByPlaceholderText('fellow.adventurer@email.com');

    // Type an email and submit
    await user.type(emailInput, 'alice@example.com');
    await user.click(screen.getByRole('button', { name: /Send Invitation/i }));

    // Modal is still open — input is reset to empty
    await waitFor(() => {
      expect(screen.getByPlaceholderText('fellow.adventurer@email.com')).toHaveValue('');
    });

    // First input is refocused (impl uses setTimeout(0), waitFor retries until it fires)
    await waitFor(() => {
      expect(document.activeElement).toBe(
        screen.getByPlaceholderText('fellow.adventurer@email.com'),
      );
    });

    // onClose was NOT called
    expect(onClose).not.toHaveBeenCalled();

    // Modal title is still visible (modal didn't close)
    expect(screen.getByText('Invite Adventurers')).toBeInTheDocument();
  });

  it('Done button calls onClose', async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();
    render(<AddMemberModal {...makeProps({ onClose })} />);

    await user.click(screen.getByRole('button', { name: 'Done' }));

    expect(onClose).toHaveBeenCalledOnce();
  });

  it('partial-failure: failed rows kept, succeeded rows removed', async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();

    // onAdd: fail for 'fail@example.com', succeed for others
    const onAdd = vi.fn().mockImplementation(async (email: string) => {
      if (email === 'fail@example.com') throw new Error('User not found');
    });

    render(<AddMemberModal {...makeProps({ onAdd, onClose })} />);

    // Add a second row
    await user.click(screen.getByRole('button', { name: /\+ Add another email/i }));

    const inputs = screen.getAllByPlaceholderText('fellow.adventurer@email.com');
    await user.type(inputs[0], 'alice@example.com');
    await user.type(inputs[1], 'fail@example.com');

    await user.click(screen.getByRole('button', { name: /Send Invitations/i }));

    // Wait for submit to complete — only the failed row remains
    await waitFor(() => {
      const remaining = screen.getAllByPlaceholderText('fellow.adventurer@email.com');
      expect(remaining).toHaveLength(1);
      expect(remaining[0]).toHaveValue('fail@example.com');
    });

    // Error displayed for the failed row
    expect(screen.getByText('User not found')).toBeInTheDocument();

    // onClose was NOT called (partial failure)
    expect(onClose).not.toHaveBeenCalled();
  });
});

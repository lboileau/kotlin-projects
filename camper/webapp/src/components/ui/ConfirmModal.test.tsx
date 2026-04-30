/**
 * W7 — ConfirmModal primitive
 *
 * Tests for the shared ConfirmModal component:
 *  1. Renders nothing when isOpen={false}.
 *  2. Renders title, message, and both buttons when isOpen={true}.
 *  3. onConfirm is called when the confirm button is clicked.
 *  4. onCancel is called when the cancel button is clicked.
 *  5. onCancel is called on Escape key press.
 *  6. onCancel is called on overlay click.
 *  7. Async: confirm button shows loading + disabled state while onConfirm Promise is pending.
 *  8. Cancel button is disabled while async onConfirm is pending.
 *  9. Escape and overlay-click are blocked while async onConfirm is pending.
 * 10. tone='danger' applies the danger variant to the confirm button.
 * 11. tone='default' applies the primary variant to the confirm button.
 * 12. Auto-focus lands on the cancel button when the modal opens.
 */
import { describe, it, expect, vi } from 'vitest';
import { render, screen, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ConfirmModal } from './ConfirmModal';

const BASE_PROPS = {
  title: 'Delete Item?',
  message: 'This action cannot be undone.',
  confirmLabel: 'Delete',
  onConfirm: vi.fn(),
  onCancel: vi.fn(),
};

function renderModal(overrides: Partial<typeof BASE_PROPS & { isOpen?: boolean; tone?: 'danger' | 'default'; cancelLabel?: string }> = {}) {
  const props = { isOpen: true, ...BASE_PROPS, ...overrides };
  return render(<ConfirmModal {...props} />);
}

describe('ConfirmModal (W7)', () => {
  it('renders nothing when isOpen={false}', () => {
    renderModal({ isOpen: false });
    expect(screen.queryByRole('heading')).not.toBeInTheDocument();
    expect(screen.queryByText('Delete Item?')).not.toBeInTheDocument();
  });

  it('renders title, message, and both buttons when isOpen={true}', () => {
    renderModal();
    expect(screen.getByText('Delete Item?')).toBeInTheDocument();
    expect(screen.getByText('This action cannot be undone.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Delete' })).toBeInTheDocument();
  });

  it('calls onConfirm when the confirm button is clicked', async () => {
    const onConfirm = vi.fn();
    renderModal({ onConfirm });
    await userEvent.click(screen.getByRole('button', { name: 'Delete' }));
    expect(onConfirm).toHaveBeenCalledOnce();
  });

  it('calls onCancel when the cancel button is clicked', async () => {
    const onCancel = vi.fn();
    renderModal({ onCancel });
    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(onCancel).toHaveBeenCalledOnce();
  });

  it('calls onCancel when Escape is pressed', async () => {
    const onCancel = vi.fn();
    renderModal({ onCancel });
    await userEvent.keyboard('{Escape}');
    expect(onCancel).toHaveBeenCalledOnce();
  });

  it('calls onCancel when the overlay is clicked', async () => {
    const onCancel = vi.fn();
    renderModal({ onCancel });
    // Click directly on the overlay backdrop (not on the modal content)
    const overlay = document.querySelector('.modal-overlay')!;
    await userEvent.click(overlay, { skipHover: true });
    expect(onCancel).toHaveBeenCalled();
  });

  it('confirm button is disabled and shows loading state while async onConfirm is pending', async () => {
    let resolvePromise!: () => void;
    const onConfirm = vi.fn(
      () => new Promise<void>(resolve => { resolvePromise = resolve; }),
    );
    renderModal({ onConfirm });

    const confirmBtn = screen.getByRole('button', { name: 'Delete' });
    await userEvent.click(confirmBtn);

    // While pending: button must be disabled and have loading class
    expect(confirmBtn).toBeDisabled();
    expect(confirmBtn).toHaveClass('btn--loading');

    // Settle the promise; button should recover
    await act(async () => { resolvePromise(); });
    expect(confirmBtn).not.toBeDisabled();
    expect(confirmBtn).not.toHaveClass('btn--loading');
  });

  it('cancel button is disabled while async onConfirm is pending', async () => {
    let resolvePromise!: () => void;
    const onConfirm = vi.fn(
      () => new Promise<void>(resolve => { resolvePromise = resolve; }),
    );
    renderModal({ onConfirm });

    await userEvent.click(screen.getByRole('button', { name: 'Delete' }));

    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();

    await act(async () => { resolvePromise(); });
    expect(screen.getByRole('button', { name: 'Cancel' })).not.toBeDisabled();
  });

  it('Escape and overlay-click are blocked while async onConfirm is pending', async () => {
    let resolvePromise!: () => void;
    const onCancel = vi.fn();
    const onConfirm = vi.fn(
      () => new Promise<void>(resolve => { resolvePromise = resolve; }),
    );
    renderModal({ onConfirm, onCancel });

    await userEvent.click(screen.getByRole('button', { name: 'Delete' }));

    // Escape should NOT call onCancel while pending (Modal closable=false)
    await userEvent.keyboard('{Escape}');
    expect(onCancel).not.toHaveBeenCalled();

    // Overlay click should NOT call onCancel while pending
    const overlay = document.querySelector('.modal-overlay')!;
    await userEvent.click(overlay, { skipHover: true });
    expect(onCancel).not.toHaveBeenCalled();

    await act(async () => { resolvePromise(); });
  });

  it('applies danger variant to confirm button when tone="danger"', () => {
    renderModal({ tone: 'danger' });
    expect(screen.getByRole('button', { name: 'Delete' })).toHaveClass('btn--danger');
  });

  it('applies primary variant to confirm button when tone="default"', () => {
    renderModal({ tone: 'default' });
    expect(screen.getByRole('button', { name: 'Delete' })).toHaveClass('btn--primary');
  });

  it('auto-focus lands on the cancel button when the modal opens', () => {
    renderModal();
    expect(screen.getByRole('button', { name: 'Cancel' })).toHaveFocus();
  });
});

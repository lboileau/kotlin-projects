/**
 * W5 — Shopping list info button (recipe usage popover)
 *
 * Tests for the reusable InfoPopover component:
 *  1. Hovering the trigger opens the popover.
 *  2. Clicking pins it open — mouse leave does NOT close when pinned.
 *  3. Outside click closes the popover.
 *  4. Escape key closes the popover.
 *  5. All items render in the list.
 *  6. aria-expanded toggles correctly.
 */
import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { InfoPopover } from './InfoPopover';

const ITEMS = ['Spaghetti Bolognese', 'Campfire Pasta'];
const LABEL = 'Recipes using this item';

function renderPopover(items = ITEMS) {
  return render(<InfoPopover items={items} ariaLabel={LABEL} />);
}

describe('InfoPopover (W5)', () => {
  it('hovering the trigger opens the popover', async () => {
    renderPopover();

    const trigger = screen.getByRole('button', { name: LABEL });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

    fireEvent.mouseEnter(trigger.closest('.info-popover')!);

    expect(screen.getByRole('dialog', { name: LABEL })).toBeInTheDocument();
  });

  it('clicking pins the popover open so mouse leave does not close it', async () => {
    const user = userEvent.setup();
    renderPopover();

    const trigger = screen.getByRole('button', { name: LABEL });
    const container = trigger.closest('.info-popover')!;

    // Hover to open
    fireEvent.mouseEnter(container);
    expect(screen.getByRole('dialog')).toBeInTheDocument();

    // Click to pin
    await user.click(trigger);
    expect(screen.getByRole('dialog')).toBeInTheDocument();

    // Mouse leave should NOT close because pinned
    fireEvent.mouseLeave(container);
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('outside click closes the popover', async () => {
    const user = userEvent.setup();
    render(
      <div>
        <InfoPopover items={ITEMS} ariaLabel={LABEL} />
        <button data-testid="outside">outside</button>
      </div>,
    );

    const trigger = screen.getByRole('button', { name: LABEL });
    const container = trigger.closest('.info-popover')!;

    // Open via hover
    fireEvent.mouseEnter(container);
    expect(screen.getByRole('dialog')).toBeInTheDocument();

    // Click outside
    await user.click(screen.getByTestId('outside'));

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('Escape key closes the popover', async () => {
    const user = userEvent.setup();
    renderPopover();

    const trigger = screen.getByRole('button', { name: LABEL });
    const container = trigger.closest('.info-popover')!;

    // Open via hover
    fireEvent.mouseEnter(container);
    expect(screen.getByRole('dialog')).toBeInTheDocument();

    // Press Escape
    await user.keyboard('{Escape}');

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('all items render in the popover list', () => {
    renderPopover();

    const trigger = screen.getByRole('button', { name: LABEL });
    fireEvent.mouseEnter(trigger.closest('.info-popover')!);

    for (const item of ITEMS) {
      expect(screen.getByText(item)).toBeInTheDocument();
    }
  });

  it('aria-expanded toggles correctly', async () => {
    const user = userEvent.setup();
    renderPopover();

    const trigger = screen.getByRole('button', { name: LABEL });
    const container = trigger.closest('.info-popover')!;

    // Initially closed
    expect(trigger).toHaveAttribute('aria-expanded', 'false');

    // Open via hover
    fireEvent.mouseEnter(container);
    expect(trigger).toHaveAttribute('aria-expanded', 'true');

    // Close via mouse leave
    fireEvent.mouseLeave(container);
    expect(trigger).toHaveAttribute('aria-expanded', 'false');

    // Open via click
    await user.click(trigger);
    expect(trigger).toHaveAttribute('aria-expanded', 'true');

    // Second click closes (unpins)
    await user.click(trigger);
    expect(trigger).toHaveAttribute('aria-expanded', 'false');
  });
});

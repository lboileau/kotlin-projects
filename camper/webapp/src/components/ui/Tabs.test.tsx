/**
 * W24 — Tabs primitive
 *
 * Tests for the shared Tabs component (WAI-ARIA tablist pattern):
 *  1. Renders Tab children as buttons inside a [role="tablist"]
 *  2. Active tab has aria-selected="true" and tabIndex={0}; others have
 *     aria-selected="false" and tabIndex={-1}
 *  3. Clicking a tab calls onChange with that tab's value
 *  4. Arrow Right cycles to the next tab (calls onChange with next value)
 *  5. Arrow Left cycles to the previous tab, with wrap-around from the first
 *  6. Home jumps to the first tab
 *  7. End jumps to the last tab
 *  8. Enter on a focused tab activates it (native button behavior)
 *  9. Space on a focused tab activates it (native button behavior)
 * 10. Disabled tabs: skipped by arrow-key cycling AND not activated on click
 */
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Tabs, Tab } from './Tabs';

// ─── Helpers ─────────────────────────────────────────────────────────────────

function renderTabs(
  value: string,
  onChange: (v: string) => void = vi.fn(),
  opts: { withDisabled?: boolean } = {},
) {
  return render(
    <Tabs value={value} onChange={onChange} ariaLabel="Test tabs">
      <Tab value="a" label="Alpha" />
      <Tab value="b" label="Beta" disabled={opts.withDisabled} />
      <Tab value="c" label="Gamma" />
    </Tabs>,
  );
}

// ─── Tests ───────────────────────────────────────────────────────────────────

describe('Tabs (W24)', () => {
  it('renders Tab children as buttons inside the tablist', () => {
    renderTabs('a');
    const tablist = screen.getByRole('tablist');
    expect(tablist).toBeInTheDocument();

    const tabs = screen.getAllByRole('tab');
    expect(tabs).toHaveLength(3);
    expect(tabs[0]).toHaveTextContent('Alpha');
    expect(tabs[1]).toHaveTextContent('Beta');
    expect(tabs[2]).toHaveTextContent('Gamma');
  });

  it('active tab has aria-selected="true" and tabIndex=0; others have aria-selected="false" and tabIndex=-1', () => {
    renderTabs('b');
    const [alpha, beta, gamma] = screen.getAllByRole('tab');

    expect(alpha).toHaveAttribute('aria-selected', 'false');
    expect(alpha).toHaveAttribute('tabindex', '-1');

    expect(beta).toHaveAttribute('aria-selected', 'true');
    expect(beta).toHaveAttribute('tabindex', '0');

    expect(gamma).toHaveAttribute('aria-selected', 'false');
    expect(gamma).toHaveAttribute('tabindex', '-1');
  });

  it('clicking a tab calls onChange with that value', async () => {
    const onChange = vi.fn();
    renderTabs('a', onChange);

    await userEvent.click(screen.getByRole('tab', { name: 'Gamma' }));
    expect(onChange).toHaveBeenCalledOnce();
    expect(onChange).toHaveBeenCalledWith('c');
  });

  it('Arrow Right cycles to the next tab and wraps from last to first', async () => {
    const onChange = vi.fn();
    renderTabs('a', onChange);

    const alphaTab = screen.getByRole('tab', { name: 'Alpha' });
    alphaTab.focus();

    // Alpha → Beta
    await userEvent.keyboard('{ArrowRight}');
    expect(onChange).toHaveBeenCalledWith('b');

    // Gamma → wraps back to Alpha
    onChange.mockClear();
    const gammaTab = screen.getByRole('tab', { name: 'Gamma' });
    gammaTab.focus();
    await userEvent.keyboard('{ArrowRight}');
    expect(onChange).toHaveBeenCalledWith('a');
  });

  it('Arrow Left cycles to the previous tab with wrap-around from the first', async () => {
    const onChange = vi.fn();
    renderTabs('a', onChange);

    const alphaTab = screen.getByRole('tab', { name: 'Alpha' });
    alphaTab.focus();

    await userEvent.keyboard('{ArrowLeft}');
    // At the start → wraps around to the last tab
    expect(onChange).toHaveBeenCalledWith('c');
  });

  it('Home jumps to the first tab', async () => {
    const onChange = vi.fn();
    renderTabs('c', onChange);

    const gammaTab = screen.getByRole('tab', { name: 'Gamma' });
    gammaTab.focus();

    await userEvent.keyboard('{Home}');
    expect(onChange).toHaveBeenCalledWith('a');
  });

  it('End jumps to the last tab', async () => {
    const onChange = vi.fn();
    renderTabs('a', onChange);

    const alphaTab = screen.getByRole('tab', { name: 'Alpha' });
    alphaTab.focus();

    await userEvent.keyboard('{End}');
    expect(onChange).toHaveBeenCalledWith('c');
  });

  it('Enter on a focused tab activates it', async () => {
    const onChange = vi.fn();
    renderTabs('a', onChange);

    const betaTab = screen.getByRole('tab', { name: 'Beta' });
    betaTab.focus();

    await userEvent.keyboard('{Enter}');
    expect(onChange).toHaveBeenCalledWith('b');
  });

  it('Space on a focused tab activates it', async () => {
    const onChange = vi.fn();
    renderTabs('a', onChange);

    const gammaTab = screen.getByRole('tab', { name: 'Gamma' });
    gammaTab.focus();

    await userEvent.keyboard(' ');
    expect(onChange).toHaveBeenCalledWith('c');
  });

  it('disabled tabs are skipped by arrow-key cycling', async () => {
    const onChange = vi.fn();
    renderTabs('a', onChange, { withDisabled: true });
    // Tab order: Alpha(enabled), Beta(disabled), Gamma(enabled)

    const alphaTab = screen.getByRole('tab', { name: 'Alpha' });
    alphaTab.focus();

    // ArrowRight from Alpha should skip disabled Beta and land on Gamma
    await userEvent.keyboard('{ArrowRight}');
    expect(onChange).toHaveBeenCalledWith('c');
  });

  it('disabled tabs are not activated on click', async () => {
    const onChange = vi.fn();
    renderTabs('a', onChange, { withDisabled: true });

    // Attempt to click the disabled tab — browser won't fire click on disabled buttons
    const betaTab = screen.getByRole('tab', { name: 'Beta' });
    expect(betaTab).toBeDisabled();

    await userEvent.click(betaTab, { pointerEventsCheck: 0 });
    // onChange should NOT have been called at all
    expect(onChange).not.toHaveBeenCalled();
  });
});

/**
 * W16 — InteractableItem badge rendering tests
 *
 * Verifies that the badge prop renders correctly with the right text,
 * tone modifier classes, and tooltip attributes.
 *
 * (1) No badge when `badge` prop is absent.
 * (2) Badge text renders when `badge` prop is provided.
 * (3) `--complete` modifier class for complete tone.
 * (4) `--progress` modifier class for progress tone.
 * (5) `--empty` modifier class for empty tone.
 * (6) `badgeTooltip` populates `title` and `aria-label` on the badge span.
 * (7) Falls back to "${label}: ${badge.label}" aria-label when no badgeTooltip.
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { InteractableItem } from './InteractableItem';

describe('InteractableItem — badge rendering (W16)', () => {
  const defaultProps = {
    id: 'tent',
    label: 'Tents & Canoe Pairings',
    x: 10,
    y: 58,
    onClick: vi.fn(),
  };

  // ── (1) No badge when prop is absent ──────────────────────────────────────

  it('(1) renders without badge element when badge prop is absent', () => {
    render(
      <InteractableItem {...defaultProps}>
        <span>icon</span>
      </InteractableItem>,
    );
    expect(document.querySelector('.interactable-item__badge')).not.toBeInTheDocument();
  });

  // ── (2) Badge text renders ─────────────────────────────────────────────────

  it('(2) badge text "3/5" renders when badge prop is provided', () => {
    render(
      <InteractableItem {...defaultProps} badge={{ label: '3/5', tone: 'progress' }}>
        <span>icon</span>
      </InteractableItem>,
    );
    expect(screen.getByText('3/5')).toBeInTheDocument();
    expect(document.querySelector('.interactable-item__badge')).toBeInTheDocument();
  });

  // ── (3) complete tone modifier class ──────────────────────────────────────

  it('(3) applies --complete modifier class for complete tone', () => {
    render(
      <InteractableItem {...defaultProps} badge={{ label: '✓', tone: 'complete' }}>
        <span>icon</span>
      </InteractableItem>,
    );
    const badge = document.querySelector('.interactable-item__badge');
    expect(badge).toHaveClass('interactable-item__badge--complete');
    expect(badge).not.toHaveClass('interactable-item__badge--progress');
    expect(badge).not.toHaveClass('interactable-item__badge--empty');
  });

  // ── (4) progress tone modifier class ──────────────────────────────────────

  it('(4) applies --progress modifier class for progress tone', () => {
    render(
      <InteractableItem {...defaultProps} badge={{ label: '3/5', tone: 'progress' }}>
        <span>icon</span>
      </InteractableItem>,
    );
    const badge = document.querySelector('.interactable-item__badge');
    expect(badge).toHaveClass('interactable-item__badge--progress');
  });

  // ── (5) empty tone modifier class ─────────────────────────────────────────

  it('(5) applies --empty modifier class for empty tone', () => {
    render(
      <InteractableItem {...defaultProps} badge={{ label: '—', tone: 'empty' }}>
        <span>icon</span>
      </InteractableItem>,
    );
    const badge = document.querySelector('.interactable-item__badge');
    expect(badge).toHaveClass('interactable-item__badge--empty');
  });

  // ── (6) badgeTooltip populates title + aria-label ────────────────────────

  it('(6) badgeTooltip populates title and aria-label on the badge span', () => {
    render(
      <InteractableItem
        {...defaultProps}
        badge={{ label: '3/5', tone: 'progress' }}
        badgeTooltip="3 of 5 adventurers assigned"
      >
        <span>icon</span>
      </InteractableItem>,
    );
    const badge = document.querySelector('.interactable-item__badge');
    expect(badge).toHaveAttribute('title', '3 of 5 adventurers assigned');
    expect(badge).toHaveAttribute('aria-label', '3 of 5 adventurers assigned');
  });

  // ── (7) aria-label fallback when no badgeTooltip ─────────────────────────

  it('(7) falls back to "${label}: ${badge.label}" aria-label when no badgeTooltip', () => {
    render(
      <InteractableItem {...defaultProps} badge={{ label: '3/5', tone: 'progress' }}>
        <span>icon</span>
      </InteractableItem>,
    );
    const badge = document.querySelector('.interactable-item__badge');
    expect(badge).toHaveAttribute('aria-label', 'Tents & Canoe Pairings: 3/5');
  });
});

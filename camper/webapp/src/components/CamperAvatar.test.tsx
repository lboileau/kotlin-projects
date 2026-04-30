/**
 * W19 — Pending vs registered member visual distinction: CamperAvatar
 *
 * Verifies:
 *  (1) Pending avatar renders with `camper-avatar--pending` class
 *  (2) Pending avatar renders the envelope overlay (✉)
 *  (3) Pending avatar tooltip is prefixed with "Invited:"
 *  (4) Registered avatar renders without pending class or envelope
 *  (5) Failed status renders the existing failed treatment (regression check)
 */
import { describe, it, expect, vi } from 'vitest';
import { render } from '@testing-library/react';
import { CamperAvatar } from './CamperAvatar';

// ─── Mocks ────────────────────────────────────────────────────────────────────

vi.mock('./AvatarHair', () => ({
  AvatarHair: () => <g data-testid="avatar-hair" />,
}));

// ─── Helpers ──────────────────────────────────────────────────────────────────

/** Minimal pending avatar props (no name = pending, no registration yet). */
function pendingProps(overrides?: Partial<Parameters<typeof CamperAvatar>[0]>) {
  return {
    name: null,
    email: 'jane@example.com',
    invitationStatus: 'sent',
    index: 0,
    total: 1,
    ...overrides,
  };
}

/** Minimal registered avatar props. */
function registeredProps(overrides?: Partial<Parameters<typeof CamperAvatar>[0]>) {
  return {
    name: 'TrailJane',
    email: 'jane@example.com',
    invitationStatus: 'registered',
    index: 0,
    total: 1,
    ...overrides,
  };
}

// ─── Tests ────────────────────────────────────────────────────────────────────

describe('CamperAvatar — pending vs registered visuals (W19)', () => {
  it('(1) pending avatar renders with camper-avatar--pending class', () => {
    const { container } = render(<CamperAvatar {...pendingProps()} />);
    const root = container.firstElementChild as HTMLElement;
    expect(root).toHaveClass('camper-avatar--pending');
  });

  it('(2) pending avatar renders the envelope overlay chip (✉)', () => {
    const { container } = render(<CamperAvatar {...pendingProps()} />);
    const overlay = container.querySelector('.camper-avatar__envelope-overlay');
    expect(overlay).toBeInTheDocument();
    expect(overlay?.textContent).toBe('✉');
  });

  it('(3) pending avatar tooltip is prefixed with "Invited:"', () => {
    const { container } = render(<CamperAvatar {...pendingProps({ email: 'jane@example.com' })} />);
    const root = container.firstElementChild as HTMLElement;
    expect(root.getAttribute('title')).toBe('Invited: jane@example.com');
  });

  it('(4) registered avatar renders without pending class or envelope overlay', () => {
    const { container } = render(<CamperAvatar {...registeredProps()} />);
    const root = container.firstElementChild as HTMLElement;
    expect(root).not.toHaveClass('camper-avatar--pending');
    expect(container.querySelector('.camper-avatar__envelope-overlay')).not.toBeInTheDocument();
  });

  it('(5) failed status renders existing failed treatment — no envelope, shows Failed tooltip', () => {
    const { container } = render(
      <CamperAvatar
        {...pendingProps({
          invitationStatus: 'failed',
          email: 'jane@example.com',
        })}
      />,
    );
    const root = container.firstElementChild as HTMLElement;
    // Failed class applied
    expect(root).toHaveClass('camper-avatar--failed');
    // Tooltip is the "Failed to invite" message — not "Invited:"
    expect(root.getAttribute('title')).toBe('Failed to invite jane@example.com');
    // Envelope overlay is suppressed for failed invitations
    expect(container.querySelector('.camper-avatar__envelope-overlay')).not.toBeInTheDocument();
  });
});

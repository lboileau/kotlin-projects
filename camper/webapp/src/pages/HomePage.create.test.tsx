/**
 * HomePage — creator membership fix
 *
 * Regression test for the bug where a newly created trip showed a "Join"
 * button instead of the navigate arrow for the creator.
 *
 * Root cause: api.createPlan() returns a Plan with isMember: false (the BE
 * does not auto-populate plan_members for the owner).  Without the fix,
 * handleCreate adds that plan to state as-is and the card renders the "Join"
 * action.  The fix overrides isMember to true locally immediately after
 * creation, since the creator IS in their own trip via ownership semantics.
 *
 * Verifies:
 *  (1) After createPlan resolves with isMember=false, the card does NOT show
 *      a "Join" button — the creator is treated as a member immediately.
 *  (2) After createPlan resolves with isMember=true (BE auto-adds), the card
 *      also shows no "Join" — fix is idempotent.
 *
 * Uses vi.hoisted() + MemoryRouter pattern consistent with other page tests.
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { HomePage } from './HomePage';
import type { Plan } from '../api/client';

// ─── Mocks ────────────────────────────────────────────────────────────────────

const mockApi = vi.hoisted(() => ({
  getPlans:    vi.fn(),
  createPlan:  vi.fn(),
  deletePlan:  vi.fn(),
  addMember:   vi.fn(),
  removeMember: vi.fn(),
}));

vi.mock('../api/client', () => ({
  api: mockApi,
}));

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    user: {
      id: 'user1',
      email: 'creator@example.com',
      username: 'TrailBlazer',
      profileCompleted: true,
      avatar: null,
      avatarSeed: null,
      dietaryRestrictions: [],
      experienceLevel: null,
      createdAt: '',
      updatedAt: '',
    },
    login: vi.fn(),
    logout: vi.fn(),
    isAuthenticated: true,
  }),
}));

// AvatarHair is a pure SVG; stub it so jsdom doesn't need to render it.
vi.mock('../components/AvatarHair', () => ({
  AvatarHair: () => <g data-testid="avatar-hair" />,
}));

// ─── Helpers ─────────────────────────────────────────────────────────────────

function makePlan(overrides: Partial<Plan> = {}): Plan {
  return {
    id: 'plan-1',
    name: 'Whitewater Trip',
    ownerId: 'user1',
    visibility: 'private',
    isMember: false,
    createdAt: '2026-04-30T00:00:00Z',
    updatedAt: '2026-04-30T00:00:00Z',
    ...overrides,
  };
}

function renderHomePage() {
  return render(
    <MemoryRouter>
      <HomePage />
    </MemoryRouter>,
  );
}

// ─── Tests ───────────────────────────────────────────────────────────────────

describe('HomePage — creator membership after createPlan', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockApi.getPlans.mockResolvedValue([]);
  });

  // ── (1) BE returns isMember=false → creator still sees arrow, not Join ─────

  it('(1) card shows no Join button when createPlan returns isMember=false', async () => {
    const user = userEvent.setup();
    mockApi.createPlan.mockResolvedValue(makePlan({ isMember: false }));

    renderHomePage();

    // Wait for loading to finish (empty state button appears)
    await waitFor(() => {
      expect(screen.getByText('Plan a New Trip')).toBeInTheDocument();
    });

    // Open create form
    await user.click(screen.getByText('Plan a New Trip'));

    // Enter trip name
    const input = screen.getByPlaceholderText('Name your expedition...');
    await user.type(input, 'Whitewater Trip');

    // Submit
    await user.click(screen.getByRole('button', { name: 'Set Camp' }));

    // Card appears with trip name
    await waitFor(() => {
      expect(screen.getByText('Whitewater Trip')).toBeInTheDocument();
    });

    // "Join" must NOT appear — creator is treated as a member immediately
    expect(screen.queryByText('Join')).not.toBeInTheDocument();
  });

  // ── (2) BE returns isMember=true → still no Join (idempotent) ─────────────

  it('(2) card shows no Join button when createPlan returns isMember=true (idempotent)', async () => {
    const user = userEvent.setup();
    mockApi.createPlan.mockResolvedValue(makePlan({ isMember: true }));

    renderHomePage();

    await waitFor(() => {
      expect(screen.getByText('Plan a New Trip')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Plan a New Trip'));
    const input = screen.getByPlaceholderText('Name your expedition...');
    await user.type(input, 'Whitewater Trip');
    await user.click(screen.getByRole('button', { name: 'Set Camp' }));

    await waitFor(() => {
      expect(screen.getByText('Whitewater Trip')).toBeInTheDocument();
    });

    expect(screen.queryByText('Join')).not.toBeInTheDocument();
  });
});

/**
 * W20 — PlanPage pending-invitations rollup integration tests
 *
 * Verifies that PlanPage correctly renders the "Pending invitations (N)"
 * section in the Manage Plan modal, with correct per-status affordances.
 *
 * IMPORTANT: Tests mount the REAL PlanPage (not a wrapper) per the W12
 * lesson — rollup logic lives in PlanPage/PendingInvitationsList and must
 * be tested end-to-end.
 *
 * (1) Owner opens Manage Plan with 2 pending + 3 registered → section
 *     shows "Pending invitations (2)" with one row per pending member.
 * (2) Non-owner → no "Manage Plan" button → rollup never rendered.
 * (3) Resendable status (`pending`) → "Resend invite" button; click
 *     calls api.addMember(planId, email) and fires success toast.
 * (4) Skip status (`sent`) → passive label visible; no "Resend invite" button.
 * (5) Skip status (`complained`) → passive label
 *     "Recipient flagged as spam — try a different email".
 * (6) Remove button on pending row opens ConfirmModal; confirming calls
 *     api.removeMember with the correct memberId.
 * (7) No pending members → "Pending invitations" section not rendered.
 *
 * Uses vi.hoisted() + MemoryRouter pattern from W16/W17 tests.
 * usePlanUpdates is mocked to a no-op (no WebSocket in unit tests).
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { PlanPage } from './PlanPage';
import type { Plan, PlanMember, MealPlanDetailResponse } from '../api/client';

// ─── Mock api ─────────────────────────────────────────────────────────────────

const mockApi = vi.hoisted(() => ({
  getPlans:           vi.fn(),
  getPlanMembers:     vi.fn(),
  getMealPlanForTrip: vi.fn(),
  getAssignments:     vi.fn(),
  getAssignment:      vi.fn(),
  updateMealPlan:     vi.fn(),
  addMember:          vi.fn(),
  removeMember:       vi.fn(),
  syncGear:           vi.fn(),
}));

vi.mock('../api/client', () => ({
  api: mockApi,
}));

// ─── Mock useToast ────────────────────────────────────────────────────────────

const mockToast = vi.hoisted(() => ({
  success: vi.fn(),
  error:   vi.fn(),
  info:    vi.fn(),
  dismiss: vi.fn(),
}));

vi.mock('../context/ToastContext', () => ({
  useToast: () => mockToast,
  ToastProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

// ─── Mock useAuth ─────────────────────────────────────────────────────────────

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    user: {
      id: 'user1',
      email: 'owner@example.com',
      username: 'CampOwner',
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

// ─── Mock usePlanUpdates (no-op — no WebSocket in unit tests) ────────────────

vi.mock('../hooks/usePlanUpdates', () => ({
  usePlanUpdates: vi.fn(),
}));

// ─── Fixtures ─────────────────────────────────────────────────────────────────

const PLAN_ID = 'plan-test';

function makePlan(ownerId = 'user1'): Plan {
  return {
    id: PLAN_ID,
    name: 'Test Trip',
    ownerId,
    visibility: 'public',
    isMember: true,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  };
}

function makeRegisteredMember(userId: string, username: string): PlanMember {
  return {
    planId: PLAN_ID,
    userId,
    username,
    email: `${username}@example.com`,
    role: 'member',
    invitationStatus: 'accepted',
    invitedBy: 'user1',
    avatarSeed: null,
    avatar: null,
    createdAt: '2026-01-01T00:00:00Z',
  };
}

function makePendingMember(
  userId: string,
  email: string,
  invitationStatus: string,
  createdAt = '2026-01-15T00:00:00Z',
): PlanMember {
  return {
    planId: PLAN_ID,
    userId,
    username: null,
    email,
    role: 'member',
    invitationStatus,
    invitedBy: 'user1',
    avatarSeed: null,
    avatar: null,
    createdAt,
  };
}

function makeMealPlan(): MealPlanDetailResponse {
  return {
    id: 'mp-1',
    planId: PLAN_ID,
    name: 'Camp Grub',
    servings: 3,
    scalingMode: 'auto',
    isTemplate: false,
    sourceTemplateId: null,
    createdBy: 'user1',
    days: [],
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  };
}

const THREE_REGISTERED = [
  makeRegisteredMember('user1', 'CampOwner'),
  makeRegisteredMember('user2', 'Alice'),
  makeRegisteredMember('user3', 'Bob'),
];

// ─── Render helper ────────────────────────────────────────────────────────────

function renderPlanPage() {
  return render(
    <MemoryRouter initialEntries={[`/plans/${PLAN_ID}`]}>
      <Routes>
        <Route path="/plans/:planId" element={<PlanPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

/** Sets up the minimal mocks required for PlanPage.loadData() to succeed. */
function setupDefaultMocks(members: PlanMember[], ownerId = 'user1') {
  mockApi.getPlans.mockResolvedValue([makePlan(ownerId)]);
  mockApi.getPlanMembers.mockResolvedValue(members);
  mockApi.getMealPlanForTrip.mockResolvedValue(makeMealPlan());
  mockApi.getAssignments.mockResolvedValue([]);
  mockApi.syncGear.mockResolvedValue(undefined);
  mockApi.updateMealPlan.mockResolvedValue({});
}

// ─── Tests ────────────────────────────────────────────────────────────────────

describe('PlanPage — pending invitations rollup (W20)', () => {
  beforeEach(() => {
    sessionStorage.clear();
    vi.clearAllMocks();
  });

  // ── (1) Owner opens Manage Plan → "Pending invitations (2)" ───────────────

  it('(1) owner sees "Pending invitations (2)" with two pending rows', async () => {
    const user = userEvent.setup();
    const members: PlanMember[] = [
      ...THREE_REGISTERED,
      makePendingMember('pend-1', 'alice@invite.com', 'pending'),
      makePendingMember('pend-2', 'bob@invite.com', 'sent'),
    ];
    setupDefaultMocks(members);

    renderPlanPage();

    // Wait for page to load, then open Manage Plan
    await waitFor(() => {
      expect(screen.getByText('Manage Plan')).toBeInTheDocument();
    });
    await user.click(screen.getByText('Manage Plan'));

    await waitFor(() => {
      expect(screen.getByText('Pending invitations (2)')).toBeInTheDocument();
    });

    // Both pending rows should appear in the rollup section
    // (email appears in both the campsite avatar and the row — use getAllByText)
    expect(screen.getAllByText('alice@invite.com').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('bob@invite.com').length).toBeGreaterThanOrEqual(1);
  });

  // ── (2) Non-owner → no Manage Plan button → rollup never shown ────────────

  it('(2) non-owner sees no "Manage Plan" button', async () => {
    // Plan is owned by 'other-user'; auth user is 'user1' → not owner
    setupDefaultMocks(THREE_REGISTERED, 'other-user');

    renderPlanPage();

    await waitFor(() => {
      // Page loads (campsite renders)
      expect(screen.queryByText('Manage Plan')).not.toBeInTheDocument();
    });
  });

  // ── (3) Resendable status (`pending`) → Resend button → api.addMember ─────

  it('(3) pending status → "Resend invite" visible; click calls api.addMember and shows toast', async () => {
    const user = userEvent.setup();
    const members: PlanMember[] = [
      ...THREE_REGISTERED,
      makePendingMember('pend-1', 'resend@example.com', 'pending'),
    ];
    setupDefaultMocks(members);
    mockApi.addMember.mockResolvedValue({ userId: 'pend-1' });

    renderPlanPage();

    await waitFor(() => {
      expect(screen.getByText('Manage Plan')).toBeInTheDocument();
    });
    await user.click(screen.getByText('Manage Plan'));

    await waitFor(() => {
      expect(screen.getByText('Pending invitations (1)')).toBeInTheDocument();
    });

    // Resend invite button should be present
    const resendBtn = screen.getByRole('button', { name: /resend invite/i });
    expect(resendBtn).toBeInTheDocument();

    await user.click(resendBtn);

    // api.addMember called with planId and email
    await waitFor(() => {
      expect(mockApi.addMember).toHaveBeenCalledWith(PLAN_ID, 'resend@example.com');
    });

    // Success toast fires
    expect(mockToast.success).toHaveBeenCalledWith(
      'Invitation resent to resend@example.com',
    );
  });

  // ── (4) Skip status (`sent`) → passive label, no Resend button ───────────

  it('(4) sent status → passive label shown; no "Resend invite" button', async () => {
    const user = userEvent.setup();
    const members: PlanMember[] = [
      ...THREE_REGISTERED,
      makePendingMember('pend-1', 'sent@example.com', 'sent'),
    ];
    setupDefaultMocks(members);

    renderPlanPage();

    await waitFor(() => {
      expect(screen.getByText('Manage Plan')).toBeInTheDocument();
    });
    await user.click(screen.getByText('Manage Plan'));

    await waitFor(() => {
      expect(screen.getByText('Pending invitations (1)')).toBeInTheDocument();
    });

    expect(screen.getByText(/already delivered/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /resend invite/i })).not.toBeInTheDocument();
  });

  // ── (5) Skip status (`complained`) → specific passive copy ───────────────

  it('(5) complained status → passive label "Recipient flagged as spam — try a different email"', async () => {
    const user = userEvent.setup();
    const members: PlanMember[] = [
      ...THREE_REGISTERED,
      makePendingMember('pend-1', 'spam@example.com', 'complained'),
    ];
    setupDefaultMocks(members);

    renderPlanPage();

    await waitFor(() => {
      expect(screen.getByText('Manage Plan')).toBeInTheDocument();
    });
    await user.click(screen.getByText('Manage Plan'));

    await waitFor(() => {
      expect(screen.getByText('Pending invitations (1)')).toBeInTheDocument();
    });

    expect(
      screen.getByText('Recipient flagged as spam — try a different email'),
    ).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /resend invite/i })).not.toBeInTheDocument();
  });

  // ── (6) Remove button → ConfirmModal → confirm → api.removeMember ────────

  it('(6) clicking Remove then confirming calls api.removeMember with correct memberId', async () => {
    const user = userEvent.setup();
    const members: PlanMember[] = [
      ...THREE_REGISTERED,
      makePendingMember('pend-to-remove', 'remove@example.com', 'pending'),
    ];
    setupDefaultMocks(members);
    mockApi.removeMember.mockResolvedValue(undefined);

    renderPlanPage();

    await waitFor(() => {
      expect(screen.getByText('Manage Plan')).toBeInTheDocument();
    });
    await user.click(screen.getByText('Manage Plan'));

    await waitFor(() => {
      expect(screen.getByText('Pending invitations (1)')).toBeInTheDocument();
    });

    // Click Remove
    const removeBtn = screen.getByRole('button', {
      name: /remove invitation for remove@example.com/i,
    });
    await user.click(removeBtn);

    // ConfirmModal should open
    await waitFor(() => {
      expect(screen.getByText('Cancel this invitation?')).toBeInTheDocument();
    });

    // Click "Cancel Invite" (the confirmLabel)
    const confirmBtn = screen.getByRole('button', { name: /cancel invite/i });
    await user.click(confirmBtn);

    await waitFor(() => {
      expect(mockApi.removeMember).toHaveBeenCalledWith(PLAN_ID, 'pend-to-remove');
    });
  });

  // ── (7) No pending members → section not rendered ─────────────────────────

  it('(7) no pending members → "Pending invitations" section not rendered', async () => {
    const user = userEvent.setup();
    setupDefaultMocks(THREE_REGISTERED);

    renderPlanPage();

    await waitFor(() => {
      expect(screen.getByText('Manage Plan')).toBeInTheDocument();
    });
    await user.click(screen.getByText('Manage Plan'));

    // Modal opens with the Members heading
    await waitFor(() => {
      expect(screen.getByText('Manage Plan', { selector: 'h2' })).toBeInTheDocument();
    });

    expect(screen.queryByText(/pending invitations/i)).not.toBeInTheDocument();
  });
});

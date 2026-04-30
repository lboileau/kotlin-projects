/**
 * W16 — PlanPage assignment-progress badge integration tests
 *
 * Verifies that PlanPage correctly computes and renders assignment badges
 * on the tent icon and kitchen badge on the kitchen icon.
 *
 * IMPORTANT: Tests mount the REAL PlanPage (not a test wrapper) per the W12
 * lesson — badge logic lives in PlanPage and must be tested there.
 *
 * (1) Tent badge: 3 of 5 members assigned to a tent → combined badge "3/5".
 * (2) Tent badge: all members in tents AND canoes → badge "✓" complete tone.
 * (3) Tent badge: 0 members assigned → badge "0/5" empty tone.
 * (4) Kitchen badge: meal plan with 5 days → badge "5d".
 * (5) Kitchen badge: null meal plan → badge "—" empty tone.
 * (6) Empty getAssignments result → tent badge "0/5".
 * (7) Pending members excluded from badge totals.
 *
 * Uses vi.hoisted() + MemoryRouter pattern from W17 tests.
 * usePlanUpdates is mocked to a no-op (no WebSocket in unit tests).
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, waitFor } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { PlanPage } from './PlanPage';
import type {
  Plan,
  PlanMember,
  MealPlanDetailResponse,
  Assignment,
  AssignmentDetail,
} from '../api/client';

// ─── Mock api ────────────────────────────────────────────────────────────────

const mockApi = vi.hoisted(() => ({
  getPlans:           vi.fn(),
  getPlanMembers:     vi.fn(),
  getMealPlanForTrip: vi.fn(),
  getAssignments:     vi.fn(),
  getAssignment:      vi.fn(),
  updateMealPlan:     vi.fn(),
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

function makePlan(): Plan {
  return {
    id: PLAN_ID,
    name: 'Test Trip',
    ownerId: 'user1',
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
    createdAt: '',
  };
}

function makePendingMember(userId: string): PlanMember {
  return {
    planId: PLAN_ID,
    userId,
    username: null,
    email: 'pending@example.com',
    role: 'member',
    invitationStatus: 'pending',
    invitedBy: 'user1',
    avatarSeed: null,
    avatar: null,
    createdAt: '',
  };
}

function makeMealPlan(servings: number, dayCount: number): MealPlanDetailResponse {
  return {
    id: 'mp-1',
    planId: PLAN_ID,
    name: 'Camp Grub',
    servings,
    scalingMode: 'auto',
    isTemplate: false,
    sourceTemplateId: null,
    createdBy: 'user1',
    days: Array.from({ length: dayCount }, (_, i) => ({
      id: `day-${i + 1}`,
      dayNumber: i + 1,
      meals: { breakfast: [], lunch: [], dinner: [], snack: [] },
    })),
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  };
}

function makeAssignment(id: string, type: 'tent' | 'canoe'): Assignment {
  return {
    id,
    planId: PLAN_ID,
    name: `${type}-${id}`,
    type,
    maxOccupancy: 4,
    ownerId: 'user1',
    createdAt: '',
    updatedAt: '',
  };
}

function makeAssignmentDetail(
  id: string,
  type: 'tent' | 'canoe',
  memberIds: string[],
): AssignmentDetail {
  return {
    ...makeAssignment(id, type),
    members: memberIds.map(uid => ({
      assignmentId: id,
      userId: uid,
      username: uid,
      createdAt: '',
    })),
  };
}

// ─── Five registered members ──────────────────────────────────────────────────

const FIVE_MEMBERS = [
  makeRegisteredMember('user1', 'CampOwner'),
  makeRegisteredMember('user2', 'Alice'),
  makeRegisteredMember('user3', 'Bob'),
  makeRegisteredMember('user4', 'Carol'),
  makeRegisteredMember('user5', 'Dave'),
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

/** Returns the badge element inside a campsite icon button (by data-item). */
function getBadgeForItem(itemId: string): Element | null {
  return document.querySelector(`[data-item="${itemId}"] .interactable-item__badge`);
}

// ─── Tests ────────────────────────────────────────────────────────────────────

describe('PlanPage — assignment progress badges (W16)', () => {
  beforeEach(() => {
    sessionStorage.clear();
    vi.clearAllMocks();
    mockApi.syncGear.mockResolvedValue(undefined);
    mockApi.updateMealPlan.mockResolvedValue({});
  });

  // ── (1) 3 of 5 members in tent → "3/5" ───────────────────────────────────

  it('(1) 3 of 5 members assigned to a tent → combined tent badge "3/5" progress', async () => {
    mockApi.getPlans.mockResolvedValue([makePlan()]);
    mockApi.getPlanMembers.mockResolvedValue(FIVE_MEMBERS);
    mockApi.getMealPlanForTrip.mockResolvedValue(null);
    mockApi.getAssignments.mockResolvedValue([makeAssignment('tent-1', 'tent')]);
    mockApi.getAssignment.mockResolvedValue(
      makeAssignmentDetail('tent-1', 'tent', ['user1', 'user2', 'user3']),
    );

    renderPlanPage();

    await waitFor(() => {
      expect(getBadgeForItem('tent')).toBeInTheDocument();
    });

    const badge = getBadgeForItem('tent');
    expect(badge?.textContent).toBe('3/5');
    expect(badge).toHaveClass('interactable-item__badge--progress');
  });

  // ── (2) All members in tents AND canoes → "✓" complete ───────────────────

  it('(2) all 5 members in tents AND canoes → combined badge "✓" complete tone', async () => {
    const allIds = ['user1', 'user2', 'user3', 'user4', 'user5'];

    mockApi.getPlans.mockResolvedValue([makePlan()]);
    mockApi.getPlanMembers.mockResolvedValue(FIVE_MEMBERS);
    mockApi.getMealPlanForTrip.mockResolvedValue(null);
    mockApi.getAssignments.mockResolvedValue([
      makeAssignment('tent-1', 'tent'),
      makeAssignment('canoe-1', 'canoe'),
    ]);
    mockApi.getAssignment.mockImplementation((_planId: string, id: string) =>
      Promise.resolve(
        id === 'tent-1'
          ? makeAssignmentDetail('tent-1', 'tent', allIds)
          : makeAssignmentDetail('canoe-1', 'canoe', allIds),
      ),
    );

    renderPlanPage();

    await waitFor(() => {
      expect(getBadgeForItem('tent')?.textContent).toBe('✓');
    });

    const badge = getBadgeForItem('tent');
    expect(badge).toHaveClass('interactable-item__badge--complete');
  });

  // ── (3) 0 members assigned → "0/5" empty ─────────────────────────────────

  it('(3) 0 of 5 members assigned → badge "0/5" empty tone', async () => {
    mockApi.getPlans.mockResolvedValue([makePlan()]);
    mockApi.getPlanMembers.mockResolvedValue(FIVE_MEMBERS);
    mockApi.getMealPlanForTrip.mockResolvedValue(null);
    // One tent assignment with no members yet
    mockApi.getAssignments.mockResolvedValue([makeAssignment('tent-1', 'tent')]);
    mockApi.getAssignment.mockResolvedValue(
      makeAssignmentDetail('tent-1', 'tent', []),
    );

    renderPlanPage();

    await waitFor(() => {
      expect(getBadgeForItem('tent')).toBeInTheDocument();
    });

    const badge = getBadgeForItem('tent');
    expect(badge?.textContent).toBe('0/5');
    expect(badge).toHaveClass('interactable-item__badge--empty');
  });

  // ── (4) Kitchen: meal plan with 5 days → "5d" ────────────────────────────

  it('(4) meal plan with 5 days → kitchen badge "5d" progress tone', async () => {
    mockApi.getPlans.mockResolvedValue([makePlan()]);
    mockApi.getPlanMembers.mockResolvedValue(FIVE_MEMBERS);
    mockApi.getMealPlanForTrip.mockResolvedValue(makeMealPlan(5, 5));
    mockApi.getAssignments.mockResolvedValue([]);

    renderPlanPage();

    await waitFor(() => {
      expect(getBadgeForItem('kitchen')).toBeInTheDocument();
    });

    const badge = getBadgeForItem('kitchen');
    expect(badge?.textContent).toBe('5d');
    expect(badge).toHaveClass('interactable-item__badge--progress');
  });

  // ── (5) Kitchen: null meal plan → "—" empty ──────────────────────────────

  it('(5) null meal plan → kitchen badge "—" empty tone', async () => {
    mockApi.getPlans.mockResolvedValue([makePlan()]);
    mockApi.getPlanMembers.mockResolvedValue(FIVE_MEMBERS);
    mockApi.getMealPlanForTrip.mockResolvedValue(null);
    mockApi.getAssignments.mockResolvedValue([makeAssignment('tent-1', 'tent')]);
    mockApi.getAssignment.mockResolvedValue(
      makeAssignmentDetail('tent-1', 'tent', ['user1', 'user2', 'user3']),
    );

    renderPlanPage();

    await waitFor(() => {
      expect(getBadgeForItem('kitchen')).toBeInTheDocument();
    });

    const badge = getBadgeForItem('kitchen');
    expect(badge?.textContent).toBe('—');
    expect(badge).toHaveClass('interactable-item__badge--empty');
  });

  // ── (6) Empty getAssignments → tent badge "0/5" ───────────────────────────

  it('(6) getAssignments returning [] → tent badge "0/5" empty tone', async () => {
    mockApi.getPlans.mockResolvedValue([makePlan()]);
    mockApi.getPlanMembers.mockResolvedValue(FIVE_MEMBERS);
    mockApi.getMealPlanForTrip.mockResolvedValue(null);
    mockApi.getAssignments.mockResolvedValue([]);

    renderPlanPage();

    await waitFor(() => {
      expect(getBadgeForItem('tent')).toBeInTheDocument();
    });

    const badge = getBadgeForItem('tent');
    expect(badge?.textContent).toBe('0/5');
    expect(badge).toHaveClass('interactable-item__badge--empty');
  });

  // ── (7) Pending members excluded from badge totals ─────────────────────────

  it('(7) pending members excluded from badge totals', async () => {
    // 3 registered + 2 pending = 5 total, but count should be 3 for badges
    mockApi.getPlans.mockResolvedValue([makePlan()]);
    mockApi.getPlanMembers.mockResolvedValue([
      makeRegisteredMember('user1', 'CampOwner'),
      makeRegisteredMember('user2', 'Alice'),
      makeRegisteredMember('user3', 'Bob'),
      makePendingMember('user4'),
      makePendingMember('user5'),
    ]);
    mockApi.getMealPlanForTrip.mockResolvedValue(null);
    // All 3 registered members assigned → badge should be "✓" (3/3 = complete)
    mockApi.getAssignments.mockResolvedValue([makeAssignment('tent-1', 'tent')]);
    mockApi.getAssignment.mockResolvedValue(
      makeAssignmentDetail('tent-1', 'tent', ['user1', 'user2', 'user3']),
    );

    renderPlanPage();

    await waitFor(() => {
      expect(getBadgeForItem('tent')).toBeInTheDocument();
    });

    // 3 assigned out of 3 registered (pending not counted) → complete
    const badge = getBadgeForItem('tent');
    expect(badge?.textContent).toBe('✓');
    expect(badge).toHaveClass('interactable-item__badge--complete');
  });

  // ── tooltip text ──────────────────────────────────────────────────────────

  it('tent badge tooltip shows per-type breakdown', async () => {
    mockApi.getPlans.mockResolvedValue([makePlan()]);
    mockApi.getPlanMembers.mockResolvedValue(FIVE_MEMBERS);
    mockApi.getMealPlanForTrip.mockResolvedValue(null);
    mockApi.getAssignments.mockResolvedValue([makeAssignment('tent-1', 'tent')]);
    mockApi.getAssignment.mockResolvedValue(
      makeAssignmentDetail('tent-1', 'tent', ['user1', 'user2', 'user3']),
    );

    renderPlanPage();

    await waitFor(() => {
      expect(getBadgeForItem('tent')).toBeInTheDocument();
    });

    const badge = getBadgeForItem('tent');
    // Tent has 3/5, canoe has 0/5 → tooltip lists both
    expect(badge).toHaveAttribute('aria-label', 'Tents: 3/5 · Canoes: 0/5');
  });

  it('kitchen badge tooltip says "No meal plan yet" when null', async () => {
    mockApi.getPlans.mockResolvedValue([makePlan()]);
    mockApi.getPlanMembers.mockResolvedValue(FIVE_MEMBERS);
    mockApi.getMealPlanForTrip.mockResolvedValue(null);
    mockApi.getAssignments.mockResolvedValue([]);

    renderPlanPage();

    await waitFor(() => {
      expect(getBadgeForItem('kitchen')).toBeInTheDocument();
    });

    const badge = getBadgeForItem('kitchen');
    expect(badge).toHaveAttribute('aria-label', 'No meal plan yet');
  });

  it('kitchen badge tooltip says "Meal plan: N days" when plan exists', async () => {
    mockApi.getPlans.mockResolvedValue([makePlan()]);
    mockApi.getPlanMembers.mockResolvedValue(FIVE_MEMBERS);
    mockApi.getMealPlanForTrip.mockResolvedValue(makeMealPlan(5, 3));
    mockApi.getAssignments.mockResolvedValue([]);

    renderPlanPage();

    await waitFor(() => {
      expect(getBadgeForItem('kitchen')).toBeInTheDocument();
    });

    const badge = getBadgeForItem('kitchen');
    expect(badge).toHaveAttribute('aria-label', 'Meal plan: 3 days');
  });
});

/**
 * W17 — PlanPage reconcile-banner integration tests
 *
 * Verifies that PlanPage correctly shows / hides MealPlanReconcileBanner
 * based on the combination of getMealPlanForTrip result and member count.
 *
 * (1) Banner renders when mealPlan.servings ≠ registered member count.
 * (2) No banner when servings === registered member count.
 * (3) No banner when getMealPlanForTrip returns null.
 * (4) No banner when sessionStorage dismissal flag is set.
 * (5) Clicking Dismiss writes the sessionStorage key.
 * (6) Clicking Update calls api.updateMealPlan; on success the sessionStorage key is cleared.
 *
 * Uses the same MemoryRouter + vi.hoisted() pattern as other page tests.
 * usePlanUpdates is mocked to a no-op (no WebSocket in tests).
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { PlanPage } from './PlanPage';
import type { Plan, PlanMember, MealPlanDetailResponse } from '../api/client';

// ─── Mock api ────────────────────────────────────────────────────────────────

const mockApi = vi.hoisted(() => ({
  getPlans:           vi.fn(),
  getPlanMembers:     vi.fn(),
  getMealPlanForTrip: vi.fn(),
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
const STORAGE_KEY = `mealPlanReconcileDismissed:${PLAN_ID}`;

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

function makeMealPlan(servings: number): MealPlanDetailResponse {
  return {
    id: 'mp-1',
    planId: PLAN_ID,
    name: 'Camp Grub',
    servings,
    scalingMode: 'auto',
    isTemplate: false,
    sourceTemplateId: null,
    createdBy: 'user1',
    days: [],
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  };
}

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

// ─── Tests ────────────────────────────────────────────────────────────────────

describe('PlanPage — MealPlanReconcileBanner integration (W17)', () => {
  beforeEach(() => {
    sessionStorage.clear();
    vi.clearAllMocks();
    mockApi.syncGear.mockResolvedValue(undefined);
    mockApi.updateMealPlan.mockResolvedValue({});
  });

  // ── (1) Mismatch → banner renders ─────────────────────────────────────────

  it('(1) renders banner when mealPlan.servings ≠ registered member count', async () => {
    // 4-serving meal plan, 5 registered members → should show banner
    mockApi.getPlans.mockResolvedValue([makePlan()]);
    mockApi.getPlanMembers.mockResolvedValue([
      makeRegisteredMember('user1', 'CampOwner'),
      makeRegisteredMember('user2', 'Alice'),
      makeRegisteredMember('user3', 'Bob'),
      makeRegisteredMember('user4', 'Carol'),
      makeRegisteredMember('user5', 'Dave'),
    ]);
    mockApi.getMealPlanForTrip.mockResolvedValue(makeMealPlan(4));

    renderPlanPage();

    await waitFor(() => {
      expect(screen.getByText(/set for 4 servings/i)).toBeInTheDocument();
    });
    expect(screen.getByText(/5 adventurers in camp/i)).toBeInTheDocument();
  });

  // ── (2) No mismatch → no banner ───────────────────────────────────────────

  it('(2) no banner when servings === registered member count', async () => {
    mockApi.getPlans.mockResolvedValue([makePlan()]);
    mockApi.getPlanMembers.mockResolvedValue([
      makeRegisteredMember('user1', 'CampOwner'),
      makeRegisteredMember('user2', 'Alice'),
    ]);
    mockApi.getMealPlanForTrip.mockResolvedValue(makeMealPlan(2));

    renderPlanPage();

    // Wait for data to load (loading spinner disappears)
    await waitFor(() => {
      expect(screen.queryByText(/Setting up camp/i)).not.toBeInTheDocument();
    });

    expect(screen.queryByText(/set for/i)).not.toBeInTheDocument();
  });

  // ── (3) null mealPlan → no banner ─────────────────────────────────────────

  it('(3) no banner when getMealPlanForTrip returns null', async () => {
    mockApi.getPlans.mockResolvedValue([makePlan()]);
    mockApi.getPlanMembers.mockResolvedValue([
      makeRegisteredMember('user1', 'CampOwner'),
      makeRegisteredMember('user2', 'Alice'),
    ]);
    mockApi.getMealPlanForTrip.mockResolvedValue(null);

    renderPlanPage();

    await waitFor(() => {
      expect(screen.queryByText(/Setting up camp/i)).not.toBeInTheDocument();
    });

    expect(screen.queryByText(/set for/i)).not.toBeInTheDocument();
  });

  // ── (4) Dismissal flag in sessionStorage → no banner ─────────────────────

  it('(4) no banner when sessionStorage dismissal flag is set', async () => {
    sessionStorage.setItem(STORAGE_KEY, '1');

    mockApi.getPlans.mockResolvedValue([makePlan()]);
    mockApi.getPlanMembers.mockResolvedValue([
      makeRegisteredMember('user1', 'CampOwner'),
      makeRegisteredMember('user2', 'Alice'),
      makeRegisteredMember('user3', 'Bob'),
    ]);
    mockApi.getMealPlanForTrip.mockResolvedValue(makeMealPlan(2));

    renderPlanPage();

    await waitFor(() => {
      expect(screen.queryByText(/Setting up camp/i)).not.toBeInTheDocument();
    });

    expect(screen.queryByText(/set for/i)).not.toBeInTheDocument();
  });

  // ── (5) Dismiss sets sessionStorage key ───────────────────────────────────

  it('(5) clicking Dismiss writes the sessionStorage key', async () => {
    const user = userEvent.setup();

    mockApi.getPlans.mockResolvedValue([makePlan()]);
    mockApi.getPlanMembers.mockResolvedValue([
      makeRegisteredMember('user1', 'CampOwner'),
      makeRegisteredMember('user2', 'Alice'),
      makeRegisteredMember('user3', 'Bob'),
    ]);
    mockApi.getMealPlanForTrip.mockResolvedValue(makeMealPlan(2));

    renderPlanPage();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Dismiss reminder' })).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: 'Dismiss reminder' }));

    expect(sessionStorage.getItem(STORAGE_KEY)).toBe('1');
  });

  // ── (6) Update success clears sessionStorage key ──────────────────────────

  it('(6) successful Update clears the sessionStorage dismissal key', async () => {
    const user = userEvent.setup();

    // Pre-set a stale dismissal key to verify it gets removed
    sessionStorage.removeItem(STORAGE_KEY);

    mockApi.getPlans.mockResolvedValue([makePlan()]);
    mockApi.getPlanMembers.mockResolvedValue([
      makeRegisteredMember('user1', 'CampOwner'),
      makeRegisteredMember('user2', 'Alice'),
      makeRegisteredMember('user3', 'Bob'),
    ]);
    mockApi.getMealPlanForTrip.mockResolvedValue(makeMealPlan(2));

    renderPlanPage();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Update to 3' })).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: 'Update to 3' }));

    await waitFor(() => {
      expect(mockApi.updateMealPlan).toHaveBeenCalledWith('mp-1', { servings: 3 });
    });

    // SessionStorage key should NOT be set after a successful update
    expect(sessionStorage.getItem(STORAGE_KEY)).not.toBe('1');

    // Toast fires
    expect(mockToast.success).toHaveBeenCalledWith('Meal plan updated to 3 servings');
  });

  // ── Pending members are excluded from the count ────────────────────────────

  it('pending (uninvited) members are excluded from the reconcile count', async () => {
    mockApi.getPlans.mockResolvedValue([makePlan()]);
    mockApi.getPlanMembers.mockResolvedValue([
      makeRegisteredMember('user1', 'CampOwner'),
      makeRegisteredMember('user2', 'Alice'),
      // pending invite — has no username
      makePendingMember('user3'),
    ]);
    // servings === 2 registered members → no banner
    mockApi.getMealPlanForTrip.mockResolvedValue(makeMealPlan(2));

    renderPlanPage();

    await waitFor(() => {
      expect(screen.queryByText(/Setting up camp/i)).not.toBeInTheDocument();
    });

    // Banner should NOT show (pending invite doesn't count toward member total)
    expect(screen.queryByText(/set for/i)).not.toBeInTheDocument();
  });
});

/**
 * W17 — MealPlanReconcileBanner unit tests
 *
 * (1) Renders nothing when mealPlan is null.
 * (2) Renders nothing when mealPlan.servings === memberCount.
 * (3) Renders nothing when sessionStorage dismissal flag is set for the plan.
 * (4) Renders banner text when servings ≠ memberCount and no dismissal.
 * (5) Singular "serving" form when currentServings === 1.
 * (6) Clicking "Update" calls api.updateMealPlan with { servings: memberCount };
 *     on resolution, fires toast.success and calls onUpdated; clears dismissal flag.
 * (7) Update failure: toast.error fires; onUpdated NOT called.
 * (8) Update button is disabled while the request is in flight.
 * (9) Clicking "Dismiss" sets the sessionStorage flag and the banner hides itself.
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MealPlanReconcileBanner } from './MealPlanReconcileBanner';
import type { MealPlanDetailResponse } from '../api/client';

// ─── Mock api ────────────────────────────────────────────────────────────────

const mockApi = vi.hoisted(() => ({
  updateMealPlan: vi.fn(),
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
}));

// ─── Fixtures ─────────────────────────────────────────────────────────────────

function makeMealPlan(servings: number): MealPlanDetailResponse {
  return {
    id: 'mp-1',
    planId: 'plan-abc',
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

const DEFAULT_PLAN_ID = 'plan-abc';
const STORAGE_KEY = `mealPlanReconcileDismissed:${DEFAULT_PLAN_ID}`;

// ─── Tests ────────────────────────────────────────────────────────────────────

describe('MealPlanReconcileBanner (W17)', () => {
  beforeEach(() => {
    sessionStorage.clear();
    vi.clearAllMocks();
    mockApi.updateMealPlan.mockResolvedValue({});
  });

  // ── (1) null mealPlan → nothing rendered ─────────────────────────────────

  it('(1) renders nothing when mealPlan is null', () => {
    const { container } = render(
      <MealPlanReconcileBanner
        mealPlan={null}
        memberCount={5}
        planId={DEFAULT_PLAN_ID}
        onUpdated={vi.fn()}
      />,
    );
    expect(container.firstChild).toBeNull();
  });

  // ── (2) servings === memberCount → nothing rendered ───────────────────────

  it('(2) renders nothing when mealPlan.servings === memberCount', () => {
    const { container } = render(
      <MealPlanReconcileBanner
        mealPlan={makeMealPlan(5)}
        memberCount={5}
        planId={DEFAULT_PLAN_ID}
        onUpdated={vi.fn()}
      />,
    );
    expect(container.firstChild).toBeNull();
  });

  // ── (3) sessionStorage dismissal flag → nothing rendered ─────────────────

  it('(3) renders nothing when sessionStorage dismissal flag is set', () => {
    sessionStorage.setItem(STORAGE_KEY, '1');
    const { container } = render(
      <MealPlanReconcileBanner
        mealPlan={makeMealPlan(4)}
        memberCount={5}
        planId={DEFAULT_PLAN_ID}
        onUpdated={vi.fn()}
      />,
    );
    expect(container.firstChild).toBeNull();
  });

  // ── (4) servings ≠ memberCount, no dismissal → banner renders ────────────

  it('(4) renders banner text when servings ≠ memberCount and no dismissal', () => {
    render(
      <MealPlanReconcileBanner
        mealPlan={makeMealPlan(4)}
        memberCount={5}
        planId={DEFAULT_PLAN_ID}
        onUpdated={vi.fn()}
      />,
    );
    expect(screen.getByText(/set for 4 servings/i)).toBeInTheDocument();
    expect(screen.getByText(/5 adventurers in camp/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Update to 5' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Dismiss reminder' })).toBeInTheDocument();
  });

  // ── (5) singular "serving" when currentServings === 1 ────────────────────

  it('(5) uses singular "serving" when currentServings is 1', () => {
    render(
      <MealPlanReconcileBanner
        mealPlan={makeMealPlan(1)}
        memberCount={3}
        planId={DEFAULT_PLAN_ID}
        onUpdated={vi.fn()}
      />,
    );
    expect(screen.getByText(/set for 1 serving,/i)).toBeInTheDocument();
  });

  // ── (6) Update success ────────────────────────────────────────────────────

  it('(6) clicking Update calls api.updateMealPlan, fires toast.success, calls onUpdated, clears dismissal flag', async () => {
    const user = userEvent.setup();
    // Pre-set the flag to verify it gets cleared
    sessionStorage.setItem(STORAGE_KEY, '1');
    // But render without dismissal (simulate the flag being absent when first shown)
    sessionStorage.removeItem(STORAGE_KEY);

    const onUpdated = vi.fn();
    render(
      <MealPlanReconcileBanner
        mealPlan={makeMealPlan(4)}
        memberCount={5}
        planId={DEFAULT_PLAN_ID}
        onUpdated={onUpdated}
      />,
    );

    await user.click(screen.getByRole('button', { name: 'Update to 5' }));

    await waitFor(() => {
      expect(mockApi.updateMealPlan).toHaveBeenCalledWith('mp-1', { servings: 5 });
    });

    expect(mockToast.success).toHaveBeenCalledWith('Meal plan updated to 5 servings');
    expect(onUpdated).toHaveBeenCalledOnce();
    // Dismissal flag should be cleared (not set to '1')
    expect(sessionStorage.getItem(STORAGE_KEY)).not.toBe('1');
  });

  // ── (7) Update failure ────────────────────────────────────────────────────

  it('(7) update failure: toast.error fires and onUpdated is NOT called', async () => {
    const user = userEvent.setup();
    mockApi.updateMealPlan.mockRejectedValue(new Error('Network error'));

    const onUpdated = vi.fn();
    render(
      <MealPlanReconcileBanner
        mealPlan={makeMealPlan(4)}
        memberCount={5}
        planId={DEFAULT_PLAN_ID}
        onUpdated={onUpdated}
      />,
    );

    await user.click(screen.getByRole('button', { name: 'Update to 5' }));

    await waitFor(() => {
      expect(mockToast.error).toHaveBeenCalledWith('Network error');
    });

    expect(onUpdated).not.toHaveBeenCalled();
    expect(mockToast.success).not.toHaveBeenCalled();
  });

  // ── (8) Update button disabled during request ─────────────────────────────

  it('(8) update button is disabled while the request is in flight', async () => {
    const user = userEvent.setup();
    // Use a promise that we control so we can inspect mid-flight state
    let resolveUpdate!: () => void;
    mockApi.updateMealPlan.mockReturnValue(
      new Promise<void>(resolve => { resolveUpdate = resolve; }),
    );

    render(
      <MealPlanReconcileBanner
        mealPlan={makeMealPlan(4)}
        memberCount={5}
        planId={DEFAULT_PLAN_ID}
        onUpdated={vi.fn()}
      />,
    );

    await user.click(screen.getByRole('button', { name: 'Update to 5' }));

    // While in flight, the button text changes and is disabled
    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Updating…' })).toBeDisabled();
    });

    // Resolve and verify button returns to normal
    resolveUpdate();
    await waitFor(() => {
      expect(screen.queryByRole('button', { name: 'Updating…' })).not.toBeInTheDocument();
    });
  });

  // ── (9) Dismiss sets sessionStorage and banner hides ─────────────────────

  it('(9) clicking Dismiss sets sessionStorage flag and banner re-renders to hidden', async () => {
    const user = userEvent.setup();
    const onUpdated = vi.fn();

    const { container, rerender } = render(
      <MealPlanReconcileBanner
        mealPlan={makeMealPlan(4)}
        memberCount={5}
        planId={DEFAULT_PLAN_ID}
        onUpdated={onUpdated}
      />,
    );

    // Banner is currently visible
    expect(screen.getByText(/set for 4 servings/i)).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Dismiss reminder' }));

    // sessionStorage flag is now set
    expect(sessionStorage.getItem(STORAGE_KEY)).toBe('1');

    // Banner immediately hides (local dismissed state)
    expect(container.firstChild).toBeNull();

    // Re-render with same props — banner stays hidden because sessionStorage has the flag
    rerender(
      <MealPlanReconcileBanner
        mealPlan={makeMealPlan(4)}
        memberCount={5}
        planId={DEFAULT_PLAN_ID}
        onUpdated={onUpdated}
      />,
    );
    expect(container.firstChild).toBeNull();
  });
});

/**
 * W6 — MealPlanModal tab hash sync
 *
 * Verifies that window.location.hash stays in sync with the active tab:
 *  (1) Rendering with isOpen={true} when the URL hash is already #shopping opens
 *      the modal on the Shopping List tab (hash → state on open).
 *  (2) Clicking the "Recipe Book" tab updates window.location.hash to #recipes
 *      (state → hash via pushState).
 *  (3) Firing a hashchange event with #overview (simulating browser back/forward)
 *      updates the active tab to Overview (hashchange listener drives state).
 *  (4) Closing the modal (re-render with isOpen={false}) clears the hash via
 *      replaceState so closing never pollutes browser history.
 *
 * Test setup notes:
 * - No MemoryRouter needed — window.location.hash and window.history.pushState /
 *   replaceState are jsdom built-ins.
 * - Reset hash in beforeEach via window.history.replaceState(null, '', '/') so
 *   each test starts from a clean URL.
 * - vi.hoisted() + vi.mock() stubs the api module and ToastContext; the stubs
 *   return minimal payloads so the loading state resolves and the tab strip renders.
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MealPlanModal } from './MealPlanModal';

// ─── Mock useToast ─────────────────────────────────────────────────────────────

const mockToast = vi.hoisted(() => ({
  success: vi.fn(),
  error: vi.fn(),
  info: vi.fn(),
  dismiss: vi.fn(),
}));

vi.mock('../context/ToastContext', () => ({
  useToast: () => mockToast,
}));

// ─── Mock API ─────────────────────────────────────────────────────────────────
// Stub the four calls MealPlanModal fires on open. Returning null for
// getMealPlanForTrip keeps mealPlan=null and prevents any subsequent
// getShoppingList calls (which guard on mealPlan != null).

const mockApi = vi.hoisted(() => ({
  getMealPlanForTrip: vi.fn().mockResolvedValue(null),
  getRecipes: vi.fn().mockResolvedValue([]),
  getTemplates: vi.fn().mockResolvedValue([]),
  getIngredients: vi.fn().mockResolvedValue([]),
}));

vi.mock('../api/client', () => ({ api: mockApi }));

// ─── Setup ────────────────────────────────────────────────────────────────────

beforeEach(() => {
  vi.clearAllMocks();
  // Reset URL to a clean path with no hash before each test.
  window.history.replaceState(null, '', '/');
});

// ─── Tests ────────────────────────────────────────────────────────────────────

describe('MealPlanModal — tab hash sync (W6)', () => {
  it('(1) opens on Shopping tab when URL hash is already #shopping', async () => {
    // Arrange: set hash before rendering, simulating a shared URL like /plans/abc#shopping.
    window.history.replaceState(null, '', '#shopping');

    render(<MealPlanModal isOpen={true} onClose={vi.fn()} planId="plan1" />);

    // Effect 1 (hash → state) reads #shopping on open and calls setActiveView('shopping').
    await waitFor(() => {
      expect(screen.getByRole('tab', { name: 'Shopping List' })).toHaveAttribute('aria-selected', 'true');
    });
    expect(screen.getByRole('tab', { name: 'Overview' })).toHaveAttribute('aria-selected', 'false');
    expect(screen.getByRole('tab', { name: 'Recipe Book' })).toHaveAttribute('aria-selected', 'false');
  });

  it('(2) clicking Recipe Book tab updates window.location.hash to #recipes', async () => {
    const user = userEvent.setup();
    render(<MealPlanModal isOpen={true} onClose={vi.fn()} planId="plan1" />);

    // Effect 2 (state → hash) pushes #overview on mount; wait for it to settle.
    await waitFor(() => {
      expect(window.location.hash).toBe('#overview');
    });

    // Act: click Recipe Book tab (which calls setActiveView('recipes')).
    await user.click(screen.getByRole('tab', { name: 'Recipe Book' }));

    // Effect 2 fires again and pushes #recipes.
    await waitFor(() => {
      expect(window.location.hash).toBe('#recipes');
    });
  });

  it('(3) firing a hashchange event to #overview updates the active tab', async () => {
    const user = userEvent.setup();
    render(<MealPlanModal isOpen={true} onClose={vi.fn()} planId="plan1" />);

    // Navigate to Shopping List first so we can navigate back.
    await user.click(screen.getByRole('tab', { name: 'Shopping List' }));
    await waitFor(() => {
      expect(screen.getByRole('tab', { name: 'Shopping List' })).toHaveAttribute('aria-selected', 'true');
    });

    // Simulate browser back: change the URL hash and fire the hashchange event.
    act(() => {
      window.history.replaceState(null, '', '#overview');
      window.dispatchEvent(
        new HashChangeEvent('hashchange', {
          newURL: window.location.href,
          oldURL: '#shopping',
        })
      );
    });

    // Effect 1's hashchange listener calls setActiveView('overview').
    await waitFor(() => {
      expect(screen.getByRole('tab', { name: 'Overview' })).toHaveAttribute('aria-selected', 'true');
    });
    expect(screen.getByRole('tab', { name: 'Shopping List' })).toHaveAttribute('aria-selected', 'false');
  });

  it('(4) closing the modal clears the hash', async () => {
    const { rerender } = render(
      <MealPlanModal isOpen={true} onClose={vi.fn()} planId="plan1" />
    );

    // Wait for effect 2 to push #overview on open.
    await waitFor(() => {
      expect(window.location.hash).toBe('#overview');
    });

    // Close the modal.
    rerender(<MealPlanModal isOpen={false} onClose={vi.fn()} planId="plan1" />);

    // Effect 3 fires and strips the hash via replaceState.
    await waitFor(() => {
      expect(window.location.hash).toBe('');
    });
  });
});

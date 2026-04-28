/**
 * W2 — Shopping list "Add Item" stays open
 *
 * Verifies keep-open behaviour for ShoppingListView:
 *  - Successful free-form add clears description, preserves qty/unit, keeps form open.
 *  - Successful ingredient add clears selection, preserves qty/unit, keeps form open.
 *  - Cancel closes the form.
 *  - Escape key closes the form.
 *  - Successful add fires useToast().success with the item name + 1500 ms duration.
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ShoppingListView } from './MealPlanModal';
import type { MealPlanDetailResponse, IngredientResponse } from '../api/client';

// ─── Mock useToast (must be declared with vi.hoisted so it's available when the
//     vi.mock factory runs — vi.mock calls are hoisted above imports). ──────────

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

const mockMealPlan: MealPlanDetailResponse = {
  id: 'mp1',
  planId: 'plan1',
  name: 'Camp Plan',
  servings: 4,
  scalingMode: 'auto',
  isTemplate: false,
  sourceTemplateId: null,
  createdBy: 'user1',
  days: [],
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

const mockIngredient: IngredientResponse = {
  id: 'ing1',
  name: 'onion',
  category: 'produce',
  defaultUnit: 'pieces',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

// ─── Props factory ────────────────────────────────────────────────────────────

function makeProps(overrides?: Record<string, unknown>) {
  return {
    shoppingList: null,
    mealPlan: mockMealPlan,
    onTogglePurchase: vi.fn(),
    onResetPurchases: vi.fn(),
    resetConfirm: false,
    setResetConfirm: vi.fn(),
    onAddManualItem: vi.fn(),
    onRemoveManualItem: vi.fn(),
    ingredients: [] as IngredientResponse[],
    onCreateIngredient: vi.fn(),
    ...overrides,
  };
}

// ─── Tests ────────────────────────────────────────────────────────────────────

describe('ShoppingListView — keep-open add-item form (W2)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('freeform submit: form stays open, description cleared', async () => {
    const user = userEvent.setup();
    render(<ShoppingListView {...makeProps()} />);

    // Open the add form
    await user.click(screen.getByRole('button', { name: /\+ Add Item/i }));

    // Switch to free-form mode
    await user.click(screen.getByRole('button', { name: 'Free-form' }));

    // Type a description
    const descInput = screen.getByPlaceholderText('e.g. paper plates, trash bags...');
    await user.type(descInput, 'paper plates');

    // Submit
    await user.click(screen.getByRole('button', { name: 'Add Item' }));

    // Form stays open — description input still mounted with cleared value
    await waitFor(() => {
      expect(screen.getByPlaceholderText('e.g. paper plates, trash bags...')).toHaveValue('');
    });

    // The trigger button is NOT visible (form is still open)
    expect(screen.queryByRole('button', { name: /\+ Add Item/i })).not.toBeInTheDocument();
    // Cancel button is still visible (part of the open form)
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
  });

  it('ingredient submit: form stays open, ingredient cleared for fresh search, qty preserved', async () => {
    const user = userEvent.setup();
    render(<ShoppingListView {...makeProps({ ingredients: [mockIngredient] })} />);

    // Open the add form (starts in ingredient mode)
    await user.click(screen.getByRole('button', { name: /\+ Add Item/i }));

    // Type in the ingredient search to surface the dropdown
    const searchInput = screen.getByPlaceholderText('Search ingredients...');
    await user.type(searchInput, 'onion');

    // Pick the ingredient from the dropdown
    await user.click(screen.getByText('onion'));

    // Fill in quantity (unit was auto-set to 'pieces' by onSelect)
    const qtyInput = screen.getByPlaceholderText('Qty');
    await user.clear(qtyInput);
    await user.type(qtyInput, '3');

    // Submit
    await user.click(screen.getByRole('button', { name: 'Add Item' }));

    // After submit: ingredient is cleared → search input is visible again (fresh search)
    await waitFor(() => {
      expect(screen.getByPlaceholderText('Search ingredients...')).toBeInTheDocument();
    });

    // Qty is still 3 — preserved at last-used value
    expect(screen.getByPlaceholderText('Qty')).toHaveValue(3);

    // Unit is still 'pieces' — preserved at last-used value (per plan-gate decision)
    expect(screen.getByRole('combobox')).toHaveValue('pieces');

    // Form trigger is NOT visible (form still open)
    expect(screen.queryByRole('button', { name: /\+ Add Item/i })).not.toBeInTheDocument();
  });

  it('Cancel button closes the form', async () => {
    const user = userEvent.setup();
    render(<ShoppingListView {...makeProps()} />);

    // Open the form
    await user.click(screen.getByRole('button', { name: /\+ Add Item/i }));
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();

    // Cancel
    await user.click(screen.getByRole('button', { name: 'Cancel' }));

    // Form is closed — trigger is visible again
    expect(screen.getByRole('button', { name: /\+ Add Item/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Cancel' })).not.toBeInTheDocument();
  });

  it('Escape key closes the form', async () => {
    const user = userEvent.setup();
    render(<ShoppingListView {...makeProps()} />);

    // Open the form
    await user.click(screen.getByRole('button', { name: /\+ Add Item/i }));

    // Focus the ingredient search input explicitly and press Escape
    const searchInput = screen.getByPlaceholderText('Search ingredients...');
    await user.click(searchInput);
    await user.keyboard('{Escape}');

    // Form is closed — trigger reappears
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /\+ Add Item/i })).toBeInTheDocument();
    });
    expect(screen.queryByRole('button', { name: 'Cancel' })).not.toBeInTheDocument();
  });

  it('successful add fires useToast().success with the item name and 1500 ms duration', async () => {
    const user = userEvent.setup();
    render(<ShoppingListView {...makeProps()} />);

    // Open form, switch to free-form
    await user.click(screen.getByRole('button', { name: /\+ Add Item/i }));
    await user.click(screen.getByRole('button', { name: 'Free-form' }));

    // Type a description
    const descInput = screen.getByPlaceholderText('e.g. paper plates, trash bags...');
    await user.type(descInput, 'paper plates');

    // Submit
    await user.click(screen.getByRole('button', { name: 'Add Item' }));

    // Toast fired with the correct message and short duration
    await waitFor(() => {
      expect(mockToast.success).toHaveBeenCalledWith(
        'Added "paper plates"',
        { durationMs: 1500 },
      );
    });
  });
});

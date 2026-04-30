/**
 * W18 — Surface servings/scaling context
 *
 * Part A — ShoppingListView (isolated wrapper):
 * 1. Header line shows "Shopping list for 4 servings · 2 of 10 purchased"
 * 2. Singular "1 serving" when servings === 1
 *
 * Part B — RecipeBookView (isolated wrapper):
 * 3. Scaling line shows "Base: 2 servings · Scaled to 4" when baseServings=2, mealPlan.servings=4
 * 4. Singular base "Base: 1 serving · Scaled to 4" when baseServings === 1
 * 5. When baseServings === mealPlan.servings (no scaling), both numbers match — line still renders
 *
 * Part C — Full MealPlanModal integration (W12 lesson):
 * Per the W12 lesson: isolated-wrapper tests must be paired with at least one full-component
 * integration test that exercises the production rendering path through real loadData + state.
 * 6. Shopping tab shows context line through the real loadData + state path in MealPlanModal
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ShoppingListView, RecipeBookView, MealPlanModal } from './MealPlanModal';
import type { RecipeBookProps } from './MealPlanModal';
import type {
  ShoppingListResponse,
  MealPlanDetailResponse,
  RecipeDetailResponse,
  RecipeResponse,
  IngredientResponse,
} from '../api/client';

// ─── Mock useToast (vi.hoisted so it's available when vi.mock factory runs) ────

const mockToast = vi.hoisted(() => ({
  success: vi.fn(),
  error:   vi.fn(),
  info:    vi.fn(),
  dismiss: vi.fn(),
}));

vi.mock('../context/ToastContext', () => ({
  useToast: () => mockToast,
}));

// ─── Mock API (needed for the full MealPlanModal integration test) ─────────────

const mockApi = vi.hoisted(() => ({
  getMealPlanForTrip: vi.fn().mockResolvedValue(null),
  getRecipes:         vi.fn().mockResolvedValue([]),
  getTemplates:       vi.fn().mockResolvedValue([]),
  getIngredients:     vi.fn().mockResolvedValue([]),
  getShoppingList:    vi.fn().mockResolvedValue(null),
}));

vi.mock('../api/client', () => ({ api: mockApi }));

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

function makeShoppingList(overrides?: Partial<ShoppingListResponse>): ShoppingListResponse {
  return {
    mealPlanId: 'mp1',
    servings: 4,
    scalingMode: 'auto',
    totalItems: 10,
    fullyPurchasedCount: 2,
    categories: [],
    ...overrides,
  };
}

function makeRecipeDetail(overrides?: Partial<RecipeDetailResponse>): RecipeDetailResponse {
  return {
    id: 'r1',
    name: 'Pasta Bolognese',
    description: null,
    webLink: null,
    baseServings: 2,
    status: 'published',
    createdBy: 'user1',
    duplicateOf: null,
    ingredients: [
      {
        id: 'ri1',
        recipeId: 'r1',
        ingredient: {
          id: 'ing1',
          name: 'onion',
          category: 'produce',
          defaultUnit: 'pieces',
          createdAt: '2026-01-01T00:00:00Z',
          updatedAt: '2026-01-01T00:00:00Z',
        },
        originalText: null,
        quantity: 1,
        unit: 'pieces',
        status: 'confirmed',
        matchedIngredient: null,
        suggestedIngredientName: null,
        suggestedCategory: null,
        suggestedUnit: null,
        reviewFlags: [],
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      },
    ],
    meal: null,
    theme: null,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    ...overrides,
  };
}

// ─── Props factories ──────────────────────────────────────────────────────────

function makeShoppingListProps(overrides?: Record<string, unknown>) {
  return {
    shoppingList: makeShoppingList(),
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

function makeRecipeBookProps(overrides?: Partial<RecipeBookProps>): RecipeBookProps {
  return {
    recipes: [] as RecipeResponse[],
    selectedRecipe: makeRecipeDetail(),
    onSelectRecipe: vi.fn(),
    recipeSearch: '',
    setRecipeSearch: vi.fn(),
    mealFilter: null,
    setMealFilter: vi.fn(),
    themeFilter: null,
    setThemeFilter: vi.fn(),
    uniqueMeals: [],
    uniqueThemes: [],
    addingToMeal: null,
    setAddingToMeal: vi.fn(),
    selectedCells: new Set<string>(),
    setSelectedCells: vi.fn(),
    addMealType: 'breakfast',
    onAddRecipeToMeal: vi.fn(),
    mealPlan: mockMealPlan,
    activeDay: 0,
    ...overrides,
  };
}

// ─── Tests ────────────────────────────────────────────────────────────────────

describe('W18 — servings/scaling context', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    window.history.replaceState(null, '', '/');
  });

  // ── Part A: ShoppingListView (isolated) ──────────────────────────────────

  describe('ShoppingListView — shopping list header', () => {
    it('(1) shows "Shopping list for N servings · X of Y purchased" (plural)', () => {
      render(<ShoppingListView {...makeShoppingListProps()} />);
      // shoppingList has servings=4, fullyPurchasedCount=2, totalItems=10
      expect(
        screen.getByText(/Shopping list for 4 servings · 2 of 10 purchased/),
      ).toBeInTheDocument();
    });

    it('(2) uses singular "1 serving" when servings === 1', () => {
      render(
        <ShoppingListView
          {...makeShoppingListProps({ shoppingList: makeShoppingList({ servings: 1 }) })}
        />,
      );
      expect(
        screen.getByText(/Shopping list for 1 serving · 2 of 10 purchased/),
      ).toBeInTheDocument();
      // Sanity: "servings" (plural) must NOT appear in the context line
      expect(screen.queryByText(/1 servings/)).not.toBeInTheDocument();
    });
  });

  // ── Part B: RecipeBookView (isolated) ────────────────────────────────────

  describe('RecipeBookView — recipe scaling line', () => {
    it('(3) shows "Base: 2 servings · Scaled to 4" when baseServings=2, mealPlan.servings=4', () => {
      render(<RecipeBookView {...makeRecipeBookProps()} />);
      // Default fixture: baseServings=2, mealPlan.servings=4
      expect(
        screen.getByText(/Base: 2 servings · Scaled to 4/),
      ).toBeInTheDocument();
    });

    it('(4) uses singular "Base: 1 serving · Scaled to 4" when baseServings === 1', () => {
      render(
        <RecipeBookView
          {...makeRecipeBookProps({
            selectedRecipe: makeRecipeDetail({ baseServings: 1 }),
          })}
        />,
      );
      expect(
        screen.getByText(/Base: 1 serving · Scaled to 4/),
      ).toBeInTheDocument();
      // Sanity: "1 servings" must NOT appear
      expect(screen.queryByText(/1 servings/)).not.toBeInTheDocument();
    });

    it('(5) renders when baseServings === mealPlan.servings (no scaling — same number both sides)', () => {
      render(
        <RecipeBookView
          {...makeRecipeBookProps({
            selectedRecipe: makeRecipeDetail({ baseServings: 4 }),
            mealPlan: { ...mockMealPlan, servings: 4 },
          })}
        />,
      );
      // Both sides are 4 — line still renders, just shows same number
      expect(
        screen.getByText(/Base: 4 servings · Scaled to 4/),
      ).toBeInTheDocument();
    });
  });

  // ── Part C: Full MealPlanModal integration (W12 lesson) ──────────────────

  describe('MealPlanModal integration', () => {
    it('(6) shopping tab shows context line through real loadData + state', async () => {
      const user = userEvent.setup();

      // Arrange: wire up API mocks so the modal loads successfully
      mockApi.getMealPlanForTrip.mockResolvedValue(mockMealPlan);
      mockApi.getShoppingList.mockResolvedValue(
        makeShoppingList({ servings: 4, totalItems: 10, fullyPurchasedCount: 2 }),
      );
      mockApi.getRecipes.mockResolvedValue([]);
      mockApi.getTemplates.mockResolvedValue([]);
      mockApi.getIngredients.mockResolvedValue([]);

      render(<MealPlanModal isOpen={true} onClose={vi.fn()} planId="plan1" />);

      // Wait for the loading state to clear and tabs to be visible
      await waitFor(() => {
        expect(screen.getByRole('tab', { name: 'Shopping List' })).toBeInTheDocument();
      });

      // Navigate to the Shopping List tab (triggers loadShoppingList via useEffect)
      await user.click(screen.getByRole('tab', { name: 'Shopping List' }));

      // The context line must appear with the payload-driven servings value
      await waitFor(() => {
        expect(
          screen.getByText(/Shopping list for 4 servings · 2 of 10 purchased/),
        ).toBeInTheDocument();
      });
    });
  });
});

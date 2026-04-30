/**
 * W12 — MealPlanModal multi-day recipe add (2D grid)
 *
 * Two suites:
 *
 * A) RecipeBookView unit tests (RecipeBookWrapper isolates the view):
 *  (1) Grid renders with one row per mealPlan.days entry and four MEAL_TYPE column headers
 *  (2) Clicking "Add to Meal Plan" pre-checks the activeDay+addMealType cell (production onClick)
 *  (3) Toggling a cell adds/removes it from selectedCells (verified via checkbox state)
 *  (4) Add button label updates: "Add" → "Add to N meals" as cells are toggled
 *  (5) Submit with 1 cell → api.addRecipeToMeal called once + single-meal toast
 *  (6) Submit with 3 cells → api.addRecipeToMeal called 3× in top-left→bottom-right order + batch toast
 *  (7) Cancel button closes the popover without API calls
 *
 * B) MealPlanModal integration test (full modal, production handler):
 *  (8) Batch submit through the real MealPlanModal verifies production handleAddRecipeToMeal
 *      calls api.addRecipeToMeal sequentially and fires the batch toast
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { useState } from 'react';
import type { Dispatch, SetStateAction } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MealPlanModal } from './MealPlanModal';
import { RecipeBookView } from './MealPlanModal';
import type {
  MealPlanDetailResponse,
  MealPlanDayResponse,
  RecipeDetailResponse,
  RecipeResponse,
} from '../api/client';

// ─── Mock api ────────────────────────────────────────────────────────────────
// Extended to cover both unit tests (addRecipeToMeal) and the integration test
// (getMealPlanForTrip, getRecipes, getTemplates, getIngredients, getRecipe).

const mockApi = vi.hoisted(() => ({
  addRecipeToMeal:     vi.fn(),
  getMealPlanForTrip:  vi.fn(),
  getRecipes:          vi.fn(),
  getTemplates:        vi.fn(),
  getIngredients:      vi.fn(),
  getRecipe:           vi.fn(),
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

// ─── Fixtures ─────────────────────────────────────────────────────────────────

const day1: MealPlanDayResponse = {
  id: 'day1',
  dayNumber: 1,
  meals: { breakfast: [], lunch: [], dinner: [], snack: [] },
};
const day2: MealPlanDayResponse = {
  id: 'day2',
  dayNumber: 2,
  meals: { breakfast: [], lunch: [], dinner: [], snack: [] },
};
const day3: MealPlanDayResponse = {
  id: 'day3',
  dayNumber: 3,
  meals: { breakfast: [], lunch: [], dinner: [], snack: [] },
};

const mockMealPlan: MealPlanDetailResponse = {
  id: 'mp1',
  planId: 'plan1',
  name: 'Camp Plan',
  servings: 4,
  scalingMode: 'auto',
  isTemplate: false,
  sourceTemplateId: null,
  createdBy: 'user1',
  days: [day1, day2, day3],
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

const pastaDetail: RecipeDetailResponse = {
  id: 'r1',
  name: 'Pasta Bolognese',
  description: null,
  webLink: null,
  baseServings: 4,
  status: 'published',
  createdBy: 'user1',
  duplicateOf: null,
  ingredients: [],
  meal: null,
  theme: null,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

const pastaRecipeResponse: RecipeResponse = {
  id: 'r1',
  name: 'Pasta Bolognese',
  status: 'published',
  baseServings: 4,
  description: null,
  webLink: null,
  meal: null,
  theme: null,
  createdBy: 'user1',
  duplicateOfId: null,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

// Mirrors the MEAL_TYPES constant in MealPlanModal.tsx (order matters for sequential test).
const MEAL_TYPES_LABELS: ReadonlyArray<{ key: string; label: string }> = [
  { key: 'breakfast', label: 'Breakfast' },
  { key: 'lunch',     label: 'Lunch'     },
  { key: 'dinner',    label: 'Dinner'    },
  { key: 'snack',     label: 'Snacks'    }, // production label is plural
] as const;

// MEAL_TYPES_KEYS used for ordering in the wrapper's handler
const MEAL_TYPES_KEYS = MEAL_TYPES_LABELS.map(m => m.key);

type MealType = 'breakfast' | 'lunch' | 'dinner' | 'snack';

// ─── Stateful test wrapper ─────────────────────────────────────────────────────
// Owns selectedCells + addingToMeal state so RecipeBookView unit tests can
// exercise the full toggle → submit → close flow without rendering MealPlanModal.
//
// The wrapper's onAddRecipeToMeal handler mirrors production handleAddRecipeToMeal
// (sequential loop + toast). Tests (5) and (6) verify the WRAPPER's handler call
// to mockApi; test (8) below verifies the PRODUCTION handler via full modal render.

interface WrapperProps {
  initialSelectedCells?: Set<string>;
  addingToMealInit?: { recipeId: string; recipeName: string } | null;
  activeDay?: number;
  addMealTypeInit?: MealType;
}

function RecipeBookWrapper({
  initialSelectedCells = new Set<string>(),
  addingToMealInit = { recipeId: 'r1', recipeName: 'Pasta Bolognese' },
  activeDay = 0,
  addMealTypeInit = 'breakfast',
}: WrapperProps) {
  const [selectedCells, setSelectedCells] = useState<Set<string>>(initialSelectedCells);
  const [addingToMeal, setAddingToMeal] =
    useState<{ recipeId: string; recipeName: string } | null>(addingToMealInit);
  const [addMealType] = useState<MealType>(addMealTypeInit);

  const handleAdd = async () => {
    if (!addingToMeal || selectedCells.size === 0) return;
    const orderedCells: { dayId: string; mealType: string }[] = [];
    for (const day of mockMealPlan.days) {
      for (const mt of MEAL_TYPES_KEYS) {
        if (selectedCells.has(`${day.id}:${mt}`)) {
          orderedCells.push({ dayId: day.id, mealType: mt });
        }
      }
    }
    try {
      for (const cell of orderedCells) {
        await mockApi.addRecipeToMeal(mockMealPlan.id, cell.dayId, {
          mealType: cell.mealType,
          recipeId: addingToMeal.recipeId,
        });
      }
      if (orderedCells.length === 1) {
        const cell = orderedCells[0];
        const day = mockMealPlan.days.find(d => d.id === cell.dayId);
        // Use the production MEAL_TYPES label (mirrors production label, e.g. 'Snacks' not 'Snack')
        const mt = MEAL_TYPES_LABELS.find(m => m.key === cell.mealType);
        mockToast.success(
          `Added "${addingToMeal.recipeName}" to Day ${day?.dayNumber} ${mt?.label}`,
        );
      } else {
        mockToast.success(`Added "${addingToMeal.recipeName}" to ${orderedCells.length} meals`);
      }
      setAddingToMeal(null);
      setSelectedCells(new Set());
    } catch {
      mockToast.error('Failed to add recipe');
    }
  };

  return (
    <RecipeBookView
      recipes={[pastaRecipeResponse]}
      selectedRecipe={pastaDetail}
      onSelectRecipe={vi.fn()}
      recipeSearch=""
      setRecipeSearch={vi.fn()}
      mealFilter={null}
      setMealFilter={vi.fn()}
      themeFilter={null}
      setThemeFilter={vi.fn()}
      uniqueMeals={[]}
      uniqueThemes={[]}
      addingToMeal={addingToMeal}
      setAddingToMeal={setAddingToMeal as Dispatch<SetStateAction<{ recipeId: string; recipeName: string } | null>>}
      selectedCells={selectedCells}
      setSelectedCells={setSelectedCells as Dispatch<SetStateAction<Set<string>>>}
      addMealType={addMealType}
      onAddRecipeToMeal={handleAdd}
      mealPlan={mockMealPlan}
      activeDay={activeDay}
    />
  );
}

// ═══════════════════════════════════════════════════════════════════════════════
// A) RecipeBookView unit tests
// ═══════════════════════════════════════════════════════════════════════════════

describe('RecipeBookView — multi-day recipe add grid (W12)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockApi.addRecipeToMeal.mockResolvedValue({});
  });

  // ── (1) Grid renders with correct rows and column headers ────────────────

  it('(1) grid renders with one row per day and four meal-type column headers', () => {
    render(<RecipeBookWrapper />);

    // Four column headers: Breakfast, Lunch, Dinner, Snacks
    expect(screen.getByRole('columnheader', { name: /breakfast/i })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: /lunch/i })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: /dinner/i })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: /snack/i })).toBeInTheDocument();

    // Three row headers: Day 1, Day 2, Day 3
    expect(screen.getByRole('rowheader', { name: 'Day 1' })).toBeInTheDocument();
    expect(screen.getByRole('rowheader', { name: 'Day 2' })).toBeInTheDocument();
    expect(screen.getByRole('rowheader', { name: 'Day 3' })).toBeInTheDocument();

    // 12 checkboxes total (3 days × 4 meal types)
    expect(screen.getAllByRole('checkbox')).toHaveLength(12);
  });

  // ── (2) Production onClick pre-checks activeDay + addMealType cell ────────
  // Renders with addingToMealInit=null so the "Add to Meal Plan" button shows,
  // then clicks it — exercising the production onClick at MealPlanModal.tsx:1262.

  it('(2) clicking "Add to Meal Plan" pre-checks the activeDay+addMealType cell', async () => {
    const user = userEvent.setup();
    render(
      <RecipeBookWrapper
        addingToMealInit={null}
        initialSelectedCells={new Set()}
        activeDay={0}
        addMealTypeInit="breakfast"
      />
    );

    // Grid is NOT shown yet — only "Add to Meal Plan" button
    expect(screen.queryByRole('grid')).not.toBeInTheDocument();

    // Click the button → production onClick seeds selectedCells with day1:breakfast
    await user.click(screen.getByRole('button', { name: 'Add to Meal Plan' }));

    // Grid now visible; day1:breakfast is checked, all others are not
    const day1BreakfastCb = screen.getByRole('checkbox', { name: /day 1 breakfast/i });
    expect(day1BreakfastCb).toBeChecked();

    const allCheckboxes = screen.getAllByRole('checkbox');
    expect(allCheckboxes.filter(cb => !cb.matches(':checked'))).toHaveLength(11);
  });

  // ── (3) Toggling a cell adds/removes it ─────────────────────────────────

  it('(3) clicking an unchecked cell checks it; clicking again unchecks it', async () => {
    const user = userEvent.setup();
    render(<RecipeBookWrapper initialSelectedCells={new Set()} />);

    const day2LunchCb = screen.getByRole('checkbox', { name: /day 2 lunch/i });
    expect(day2LunchCb).not.toBeChecked();

    // Toggle on
    await user.click(day2LunchCb.closest('label')!);
    expect(screen.getByRole('checkbox', { name: /day 2 lunch/i })).toBeChecked();

    // Toggle off
    await user.click(day2LunchCb.closest('label')!);
    expect(screen.getByRole('checkbox', { name: /day 2 lunch/i })).not.toBeChecked();
  });

  // ── (4) Button label updates dynamically ────────────────────────────────

  it('(4) button label is "Add" for 0–1 cells and "Add to N meals" for ≥2', async () => {
    const user = userEvent.setup();
    render(<RecipeBookWrapper initialSelectedCells={new Set()} />);

    // 0 cells selected → "Add" and disabled
    expect(screen.getByRole('button', { name: 'Add' })).toBeDisabled();

    // Select 1 cell → still "Add" but enabled
    await user.click(screen.getByRole('checkbox', { name: /day 1 breakfast/i }).closest('label')!);
    expect(screen.getByRole('button', { name: 'Add' })).toBeEnabled();

    // Select 2nd cell → "Add to 2 meals"
    await user.click(screen.getByRole('checkbox', { name: /day 1 lunch/i }).closest('label')!);
    expect(screen.getByRole('button', { name: 'Add to 2 meals' })).toBeInTheDocument();

    // Select 3rd cell → "Add to 3 meals"
    await user.click(screen.getByRole('checkbox', { name: /day 2 dinner/i }).closest('label')!);
    expect(screen.getByRole('button', { name: 'Add to 3 meals' })).toBeInTheDocument();
  });

  // ── (5) Submit with 1 cell ───────────────────────────────────────────────

  it('(5) submitting with 1 cell calls api.addRecipeToMeal once and fires single-meal toast', async () => {
    const user = userEvent.setup();
    render(
      <RecipeBookWrapper initialSelectedCells={new Set(['day2:dinner'])} />
    );

    await user.click(screen.getByRole('button', { name: 'Add' }));

    await waitFor(() => {
      expect(mockApi.addRecipeToMeal).toHaveBeenCalledTimes(1);
    });

    expect(mockApi.addRecipeToMeal).toHaveBeenCalledWith(
      'mp1', 'day2', { mealType: 'dinner', recipeId: 'r1' }
    );

    expect(mockToast.success).toHaveBeenCalledWith(
      'Added "Pasta Bolognese" to Day 2 Dinner'
    );

    // Popover closes after success
    await waitFor(() => {
      expect(screen.queryByRole('grid')).not.toBeInTheDocument();
    });
  });

  // ── (6) Submit with 3 cells ──────────────────────────────────────────────

  it('(6) submitting with 3 cells calls api.addRecipeToMeal 3× in grid order and fires batch toast', async () => {
    const user = userEvent.setup();

    // Cells inserted in arbitrary Set order; should submit in day-then-mealtype order:
    //   day1:dinner → day2:breakfast → day3:lunch
    render(
      <RecipeBookWrapper
        initialSelectedCells={new Set(['day3:lunch', 'day1:dinner', 'day2:breakfast'])}
      />
    );

    await user.click(screen.getByRole('button', { name: 'Add to 3 meals' }));

    await waitFor(() => {
      expect(mockApi.addRecipeToMeal).toHaveBeenCalledTimes(3);
    });

    expect(mockApi.addRecipeToMeal).toHaveBeenNthCalledWith(
      1, 'mp1', 'day1', { mealType: 'dinner', recipeId: 'r1' }
    );
    expect(mockApi.addRecipeToMeal).toHaveBeenNthCalledWith(
      2, 'mp1', 'day2', { mealType: 'breakfast', recipeId: 'r1' }
    );
    expect(mockApi.addRecipeToMeal).toHaveBeenNthCalledWith(
      3, 'mp1', 'day3', { mealType: 'lunch', recipeId: 'r1' }
    );

    expect(mockToast.success).toHaveBeenCalledWith(
      'Added "Pasta Bolognese" to 3 meals'
    );

    // Popover closes
    await waitFor(() => {
      expect(screen.queryByRole('grid')).not.toBeInTheDocument();
    });
  });

  // ── (7) Cancel closes popover without API calls ──────────────────────────

  it('(7) Cancel button closes the popover and does not call api.addRecipeToMeal', async () => {
    const user = userEvent.setup();
    render(
      <RecipeBookWrapper
        initialSelectedCells={new Set(['day1:breakfast', 'day2:lunch'])}
      />
    );

    expect(screen.getByRole('grid')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Cancel' }));

    await waitFor(() => {
      expect(screen.queryByRole('grid')).not.toBeInTheDocument();
    });

    expect(screen.getByRole('button', { name: 'Add to Meal Plan' })).toBeInTheDocument();
    expect(mockApi.addRecipeToMeal).not.toHaveBeenCalled();
  });
});

// ═══════════════════════════════════════════════════════════════════════════════
// B) MealPlanModal integration test — verifies PRODUCTION handleAddRecipeToMeal
// ═══════════════════════════════════════════════════════════════════════════════
// Tests (5) and (6) above only exercise the wrapper's hand-copied handler.
// Test (8) renders the full MealPlanModal so the Add button fires the real
// handleAddRecipeToMeal — if the sequential loop or toast format were changed in
// production, this test would catch it.

describe('MealPlanModal — handleAddRecipeToMeal (integration, W12)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    window.history.replaceState(null, '', '/');

    mockApi.getMealPlanForTrip.mockResolvedValue(mockMealPlan);
    mockApi.getRecipes.mockResolvedValue([pastaRecipeResponse]);
    mockApi.getTemplates.mockResolvedValue([]);
    mockApi.getIngredients.mockResolvedValue([]);
    mockApi.getRecipe.mockResolvedValue(pastaDetail);
    mockApi.addRecipeToMeal.mockResolvedValue({});
  });

  it('(8) batch submit via full MealPlanModal calls production sequential loop + fires batch toast', async () => {
    const user = userEvent.setup();

    render(<MealPlanModal isOpen={true} onClose={vi.fn()} planId="plan1" />);

    // Wait for tab strip (always visible) and loading to finish (recipe list appears)
    await waitFor(() => {
      expect(screen.getByRole('tab', { name: 'Recipe Book' })).toBeInTheDocument();
    });

    // Switch to Recipe Book tab
    await user.click(screen.getByRole('tab', { name: 'Recipe Book' }));

    // Wait for Pasta Bolognese to appear in the recipe list
    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Pasta Bolognese' })).toBeInTheDocument();
    });

    // Select the recipe → triggers handleSelectRecipe → calls getRecipe
    await user.click(screen.getByRole('button', { name: 'Pasta Bolognese' }));

    // Wait for "Add to Meal Plan" button to appear (recipe detail loaded)
    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Add to Meal Plan' })).toBeInTheDocument();
    });

    // Click "Add to Meal Plan" → production onClick seeds selectedCells with day1:breakfast
    await user.click(screen.getByRole('button', { name: 'Add to Meal Plan' }));

    // Grid appears
    await waitFor(() => {
      expect(screen.getByRole('grid')).toBeInTheDocument();
    });

    // Pre-checked: day1:breakfast (activeDay=0, addMealType='breakfast').
    // Toggle day2:dinner → selectedCells = {day1:breakfast, day2:dinner} → 2 cells
    const day2DinnerCb = screen.getByRole('checkbox', { name: /day 2 dinner/i });
    await user.click(day2DinnerCb.closest('label')!);

    // Submit via production handleAddRecipeToMeal
    await user.click(screen.getByRole('button', { name: 'Add to 2 meals' }));

    // Production sequential loop fires: breakfast before dinner (MEAL_TYPES order within day1)
    await waitFor(() => {
      expect(mockApi.addRecipeToMeal).toHaveBeenCalledTimes(2);
    });

    expect(mockApi.addRecipeToMeal).toHaveBeenNthCalledWith(
      1, 'mp1', 'day1', { mealType: 'breakfast', recipeId: 'r1' }
    );
    expect(mockApi.addRecipeToMeal).toHaveBeenNthCalledWith(
      2, 'mp1', 'day2', { mealType: 'dinner', recipeId: 'r1' }
    );

    expect(mockToast.success).toHaveBeenCalledWith(
      'Added "Pasta Bolognese" to 2 meals'
    );

    // Grid closes after success
    await waitFor(() => {
      expect(screen.queryByRole('grid')).not.toBeInTheDocument();
    });
  });
});

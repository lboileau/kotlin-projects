/**
 * W3 — MealPlanModal recipe-add inline keep-open behaviour
 *
 * Verifies:
 *  (1) Pick recipe from inline search → search cleared, addingMealType stays set (form open).
 *  (2) Cancel button closes the inline form.
 *  (3) Escape key closes the inline form.
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { OverviewView } from './MealPlanModal';
import type { OverviewProps } from './MealPlanModal';
import type { MealPlanDetailResponse, MealPlanDayResponse, RecipeResponse } from '../api/client';

// ─── Mock useToast ─────────────────────────────────────────────────────────────

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

const mockDay: MealPlanDayResponse = {
  id: 'day1',
  dayNumber: 1,
  meals: {
    breakfast: [],
    lunch: [],
    dinner: [],
    snack: [],
  },
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
  days: [mockDay],
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

const mockRecipes: RecipeResponse[] = [
  { id: 'r1', name: 'Pasta Bolognese', status: 'published', baseServings: 4, description: null, webLink: null, meal: null, theme: null, createdBy: 'user1', duplicateOfId: null, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
  { id: 'r2', name: 'Pancakes', status: 'published', baseServings: 2, description: null, webLink: null, meal: null, theme: null, createdBy: 'user1', duplicateOfId: null, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
];

// ─── Props factory ────────────────────────────────────────────────────────────

function makeProps(overrides?: Partial<OverviewProps>): OverviewProps {
  return {
    mealPlan: mockMealPlan,
    currentDay: mockDay,
    activeDay: 0,
    setActiveDay: vi.fn(),
    onAddDay: vi.fn(),
    onRemoveDay: vi.fn(),
    onRemoveRecipe: vi.fn(),
    onUpdateName: vi.fn(),
    onUpdateServings: vi.fn(),
    createName: '',
    setCreateName: vi.fn(),
    createServings: 4,
    setCreateServings: vi.fn(),
    creating: false,
    onCreate: vi.fn(),
    error: null,
    templates: [],
    showLoadTemplate: false,
    setShowLoadTemplate: vi.fn(),
    loadingTemplate: false,
    onLoadTemplate: vi.fn(),
    onPreviewTemplate: vi.fn(),
    templatePreview: null,
    loadingPreview: false,
    setReplaceTemplateId: vi.fn(),
    setTemplatePreview: vi.fn(),
    showSaveTemplate: false,
    setShowSaveTemplate: vi.fn(),
    templateName: '',
    setTemplateName: vi.fn(),
    savingTemplate: false,
    onSaveAsTemplate: vi.fn(),
    recipes: mockRecipes,
    onAddRecipeInline: vi.fn(),
    ...overrides,
  };
}

// ─── Tests ────────────────────────────────────────────────────────────────────

describe('OverviewView — recipe-add inline keep-open (W3)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('pick recipe: search cleared, form stays open, toast fired', async () => {
    const user = userEvent.setup();
    const onAddRecipeInline = vi.fn().mockResolvedValue(undefined);
    render(<OverviewView {...makeProps({ onAddRecipeInline })} />);

    // There are 4 meal sections (breakfast/lunch/dinner/snack); open the first (Breakfast)
    const addBtns = screen.getAllByRole('button', { name: /\+ Add Recipe/i });
    expect(addBtns).toHaveLength(4);
    await user.click(addBtns[0]);

    // Search input appears
    const searchInput = screen.getByPlaceholderText('Search recipes...');
    expect(searchInput).toBeInTheDocument();

    // Type to filter
    await user.type(searchInput, 'Pasta');

    // Pick the recipe
    await user.click(screen.getByRole('button', { name: 'Pasta Bolognese' }));

    // onAddRecipeInline was called
    expect(onAddRecipeInline).toHaveBeenCalledWith('day1', 'breakfast', 'r1');

    // Toast fired with recipe name and meal type label
    expect(mockToast.success).toHaveBeenCalledWith(
      'Added "Pasta Bolognese" to Breakfast',
    );

    // Search is cleared — input still visible (form still open)
    await waitFor(() => {
      expect(screen.getByPlaceholderText('Search recipes...')).toHaveValue('');
    });

    // Now only 3 "+ Add Recipe" triggers visible (breakfast is replaced by open form)
    expect(screen.getAllByRole('button', { name: /\+ Add Recipe/i })).toHaveLength(3);

    // Cancel button still visible
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
  });

  it('Cancel button closes the inline form', async () => {
    const user = userEvent.setup();
    render(<OverviewView {...makeProps()} />);

    // Open the inline add for Breakfast (first meal section)
    const addBtns = screen.getAllByRole('button', { name: /\+ Add Recipe/i });
    await user.click(addBtns[0]);
    expect(screen.getByPlaceholderText('Search recipes...')).toBeInTheDocument();

    // Cancel
    await user.click(screen.getByRole('button', { name: 'Cancel' }));

    // Form is closed — all 4 triggers visible again
    expect(screen.getAllByRole('button', { name: /\+ Add Recipe/i })).toHaveLength(4);
    expect(screen.queryByPlaceholderText('Search recipes...')).not.toBeInTheDocument();
  });

  it('Escape key closes the inline form', async () => {
    const user = userEvent.setup();
    render(<OverviewView {...makeProps()} />);

    // Open the inline add for Breakfast
    const addBtns = screen.getAllByRole('button', { name: /\+ Add Recipe/i });
    await user.click(addBtns[0]);

    const searchInput = screen.getByPlaceholderText('Search recipes...');
    await user.click(searchInput);
    await user.keyboard('{Escape}');

    // Form is closed — all 4 triggers visible again
    await waitFor(() => {
      expect(screen.getAllByRole('button', { name: /\+ Add Recipe/i })).toHaveLength(4);
    });
    expect(screen.queryByPlaceholderText('Search recipes...')).not.toBeInTheDocument();
  });
});

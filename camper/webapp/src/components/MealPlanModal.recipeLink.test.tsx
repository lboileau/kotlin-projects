/**
 * W4 — MealPlanModal external recipe link in OverviewView
 *
 * Verifies:
 *  (1) Recipe row with non-null recipeWebLink renders an <a target="_blank">
 *      with the correct href and rel="noopener noreferrer".
 *  (2) Recipe row with null recipeWebLink does not render the anchor.
 *  (3) aria-label includes the hostname extracted from the URL.
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { OverviewView } from './MealPlanModal';
import type { OverviewProps } from './MealPlanModal';
import type {
  MealPlanDetailResponse,
  MealPlanDayResponse,
  MealPlanRecipeDetailResponse,
  RecipeResponse,
} from '../api/client';

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

const recipeWithLink: MealPlanRecipeDetailResponse = {
  id: 'mpr1',
  recipeId: 'r1',
  recipeName: 'Trail Pasta',
  recipeWebLink: 'https://www.allrecipes.com/trail-pasta',
  baseServings: 4,
  scaleFactor: 1,
  isFullyPurchased: false,
  ingredients: [],
};

const recipeWithoutLink: MealPlanRecipeDetailResponse = {
  id: 'mpr2',
  recipeId: 'r2',
  recipeName: 'Camp Oatmeal',
  recipeWebLink: null,
  baseServings: 2,
  scaleFactor: 1,
  isFullyPurchased: false,
  ingredients: [],
};

const mockDay: MealPlanDayResponse = {
  id: 'day1',
  dayNumber: 1,
  meals: {
    breakfast: [recipeWithoutLink],
    lunch: [],
    dinner: [recipeWithLink],
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

const mockRecipes: RecipeResponse[] = [];

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

describe('OverviewView — external recipe link (W4)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders an <a target="_blank"> with the URL and rel="noopener noreferrer" when recipeWebLink is set', () => {
    render(<OverviewView {...makeProps()} />);

    const link = screen.getByRole('link', {
      name: /open original recipe at/i,
    });
    expect(link).toBeInTheDocument();
    expect(link).toHaveAttribute('href', 'https://www.allrecipes.com/trail-pasta');
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
  });

  it('does not render the anchor when recipeWebLink is null', () => {
    render(<OverviewView {...makeProps()} />);

    // Camp Oatmeal (breakfast) has null recipeWebLink — no link should exist for it
    // The only link present is for Trail Pasta (dinner)
    const links = screen.getAllByRole('link');
    expect(links).toHaveLength(1);
    expect(links[0]).toHaveAttribute('href', 'https://www.allrecipes.com/trail-pasta');
  });

  it('aria-label includes the hostname from the URL', () => {
    render(<OverviewView {...makeProps()} />);

    const link = screen.getByRole('link', {
      name: /open original recipe at www\.allrecipes\.com/i,
    });
    expect(link).toBeInTheDocument();
    expect(link).toHaveAttribute(
      'aria-label',
      'Open original recipe at www.allrecipes.com',
    );
  });
});

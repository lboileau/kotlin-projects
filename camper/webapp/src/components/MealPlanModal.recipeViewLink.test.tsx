/**
 * W1 — MealPlanModal in-app "View recipe" link in OverviewView
 *
 * Verifies:
 *  (1) Recipe row renders a "View recipe" anchor with href="/recipes/{recipeId}",
 *      target="_blank", and rel="noopener noreferrer".
 *  (2) The anchor's aria-label contains the recipe name.
 *  (3) The viewlink anchor sits before the existing recipeWebLink external-link
 *      anchor in DOM order.
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
  error: vi.fn(),
  info: vi.fn(),
  dismiss: vi.fn(),
}));

vi.mock('../context/ToastContext', () => ({
  useToast: () => mockToast,
}));

// ─── Fixtures ─────────────────────────────────────────────────────────────────

const recipeWithBothLinks: MealPlanRecipeDetailResponse = {
  id: 'mpr1',
  recipeId: 'r1',
  recipeName: 'Trail Pasta',
  recipeWebLink: 'https://www.allrecipes.com/trail-pasta',
  baseServings: 4,
  scaleFactor: 1,
  isFullyPurchased: false,
  ingredients: [],
};

const mockDay: MealPlanDayResponse = {
  id: 'day1',
  dayNumber: 1,
  meals: {
    breakfast: [],
    lunch: [],
    dinner: [recipeWithBothLinks],
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

describe('OverviewView — in-app "View recipe" link (W1)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('(1) renders a viewlink anchor with correct href, target, and rel', () => {
    render(<OverviewView {...makeProps()} />);

    const viewLink = screen.getByRole('link', {
      name: /view trail pasta in the recipe chest/i,
    });
    expect(viewLink).toBeInTheDocument();
    expect(viewLink).toHaveAttribute('href', '/recipes/r1');
    expect(viewLink).toHaveAttribute('target', '_blank');
    expect(viewLink).toHaveAttribute('rel', 'noopener noreferrer');
  });

  it('(2) aria-label contains the recipe name', () => {
    render(<OverviewView {...makeProps()} />);

    const viewLink = screen.getByRole('link', {
      name: /view trail pasta in the recipe chest/i,
    });
    expect(viewLink).toHaveAttribute(
      'aria-label',
      'View Trail Pasta in the recipe chest'
    );
  });

  it('(3) viewlink sits before the extlink in DOM order', () => {
    render(<OverviewView {...makeProps()} />);

    const links = screen.getAllByRole('link');
    // Two links in this row: viewlink (in-app) first, extlink (external) second
    expect(links.length).toBeGreaterThanOrEqual(2);

    const viewLinkIdx = links.findIndex(l =>
      l.getAttribute('href') === '/recipes/r1'
    );
    const extLinkIdx = links.findIndex(l =>
      l.getAttribute('href') === 'https://www.allrecipes.com/trail-pasta'
    );
    expect(viewLinkIdx).toBeGreaterThanOrEqual(0);
    expect(extLinkIdx).toBeGreaterThanOrEqual(0);
    // viewlink comes before extlink in document order
    expect(viewLinkIdx).toBeLessThan(extLinkIdx);
  });
});

import { describe, it, expect } from 'vitest';
import { buildMealPlanSummary } from './mealPlanSummary';
import type {
  MealPlanDetailResponse,
  MealPlanDayResponse,
  MealPlanRecipeDetailResponse,
} from '../api/client';

function makeRecipe(
  id: string,
  name: string,
  webLink: string | null = null,
): MealPlanRecipeDetailResponse {
  return {
    id: `mpr-${id}`,
    recipeId: id,
    recipeName: name,
    recipeWebLink: webLink,
    baseServings: 4,
    scaleFactor: 1,
    isFullyPurchased: false,
    ingredients: [],
  };
}

function makeDay(
  dayNumber: number,
  meals: Partial<{
    breakfast: MealPlanRecipeDetailResponse[];
    lunch: MealPlanRecipeDetailResponse[];
    dinner: MealPlanRecipeDetailResponse[];
    snack: MealPlanRecipeDetailResponse[];
  }> = {},
): MealPlanDayResponse {
  return {
    id: `day-${dayNumber}`,
    dayNumber,
    meals: {
      breakfast: meals.breakfast ?? [],
      lunch: meals.lunch ?? [],
      dinner: meals.dinner ?? [],
      snack: meals.snack ?? [],
    },
  };
}

function makePlan(...days: MealPlanDayResponse[]): MealPlanDetailResponse {
  return {
    id: 'plan-1',
    planId: null,
    name: 'Test Plan',
    servings: 4,
    scalingMode: 'manual',
    isTemplate: false,
    sourceTemplateId: null,
    createdBy: 'user-1',
    days,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  };
}

describe('buildMealPlanSummary', () => {
  it('returns empty string for meal plan with no days', () => {
    expect(buildMealPlanSummary(makePlan())).toBe('');
  });

  it('returns empty string when days have no recipes', () => {
    expect(buildMealPlanSummary(makePlan(makeDay(1), makeDay(2)))).toBe('');
  });

  it('returns just the recipe name when there is no web link', () => {
    const plan = makePlan(makeDay(1, { breakfast: [makeRecipe('r1', 'Pancakes')] }));
    expect(buildMealPlanSummary(plan)).toBe('Pancakes');
  });

  it('returns name and link separated by newline when recipe has a web link', () => {
    const plan = makePlan(
      makeDay(1, { dinner: [makeRecipe('r1', 'Campfire Chili', 'https://example.com/chili')] }),
    );
    expect(buildMealPlanSummary(plan)).toBe('Campfire Chili\nhttps://example.com/chili');
  });

  it('separates two recipes with a blank line, link-recipe first then no-link', () => {
    const plan = makePlan(
      makeDay(1, {
        dinner: [makeRecipe('r1', 'Campfire Chili', 'https://example.com/chili')],
        snack: [makeRecipe('r2', 'Pancakes')],
      }),
    );
    expect(buildMealPlanSummary(plan)).toBe(
      'Campfire Chili\nhttps://example.com/chili\n\nPancakes',
    );
  });

  it('deduplicates by recipeId — same recipe across meals and days appears once', () => {
    const recipe = makeRecipe('r1', 'Trail Mix', 'https://example.com/trail');
    const plan = makePlan(
      makeDay(1, { breakfast: [recipe], lunch: [recipe] }),
      makeDay(2, { snack: [recipe] }),
    );
    expect(buildMealPlanSummary(plan)).toBe('Trail Mix\nhttps://example.com/trail');
  });

  it('does not deduplicate by name — two recipes with same name but different ids both appear', () => {
    const r1 = makeRecipe('r1', 'Granola');
    const r2 = makeRecipe('r2', 'Granola');
    const plan = makePlan(makeDay(1, { breakfast: [r1], lunch: [r2] }));
    expect(buildMealPlanSummary(plan)).toBe('Granola\n\nGranola');
  });

  it('respects day order ascending and meal type order breakfast→lunch→dinner→snack', () => {
    const plan = makePlan(
      makeDay(2, { snack: [makeRecipe('snack-d2', 'Apple')] }),
      makeDay(1, {
        snack: [makeRecipe('snack-d1', 'Cookie')],
        breakfast: [makeRecipe('bfast', 'Oatmeal')],
        dinner: [makeRecipe('dinner', 'Stew')],
        lunch: [makeRecipe('lunch', 'Sandwich')],
      }),
    );
    // Day 2 comes second in the array but has dayNumber 2 — the function iterates
    // the days array in response order, so day-2 (first in array) is visited first.
    // Then day-1 is visited; within day-1 the order is breakfast→lunch→dinner→snack.
    expect(buildMealPlanSummary(plan)).toBe(
      'Apple\n\nOatmeal\n\nSandwich\n\nStew\n\nCookie',
    );
  });
});

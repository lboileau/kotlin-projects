import { describe, it, expect } from 'vitest';
import type { ShoppingListItemResponse, ShoppingListResponse } from '../api/shopping';
import {
  applyRowPurchases,
  clearAllPurchases,
  buildShoppingRows,
  derivePurchaseStatus,
  formatStillNeededText,
  haveSheetEntries,
  onlyStillToBuy,
  restoreRowEntries,
  rowPurchasesForToggle,
  type ShoppingRow,
} from './shoppingRows';

function makeItem(overrides: Partial<ShoppingListItemResponse>): ShoppingListItemResponse {
  return {
    ingredientId: 'garlic',
    ingredientName: 'Garlic',
    description: null,
    quantityRequired: 3,
    quantityPurchased: 0,
    unit: 'clove',
    status: 'not_purchased',
    usedInRecipes: [],
    usedInRecipeRefs: [],
    source: 'recipe',
    manualItemId: null,
    ...overrides,
  };
}

function makeList(items: ShoppingListItemResponse[]): ShoppingListResponse {
  return {
    mealPlanId: 'plan',
    mealPlanName: 'Plan',
    servings: 4,
    scalingMode: 'fractional',
    categories: [{ category: 'produce', items }],
    totalItems: items.length,
    fullyPurchasedCount: items.filter((item) => item.status === 'done').length,
  };
}

function onlyRow(list: ShoppingListResponse, key: string): ShoppingRow {
  const row = buildShoppingRows(list)
    .flatMap((group) => group.rows)
    .find((candidate) => candidate.key === key);
  if (!row) throw new Error(`no row ${key}`);
  return row;
}

describe('derivePurchaseStatus', () => {
  it('matches the server for each case', () => {
    expect(derivePurchaseStatus(3, 0)).toBe('not_purchased');
    expect(derivePurchaseStatus(3, 2)).toBe('more_needed');
    expect(derivePurchaseStatus(3, 3)).toBe('done');
    expect(derivePurchaseStatus(3, 5)).toBe('done');
    expect(derivePurchaseStatus(0, 2)).toBe('no_longer_needed');
    expect(derivePurchaseStatus(0, 0)).toBe('not_purchased');
  });
});

describe('clearAllPurchases', () => {
  it('unticks every row, zeroes the count and keeps the list otherwise the same', () => {
    const list = makeList([
      makeItem({ quantityPurchased: 3, status: 'done' }),
      makeItem({ ingredientId: 'onion', ingredientName: 'Onion', unit: 'whole', quantityPurchased: 1, status: 'more_needed' }),
    ]);
    const before = { ...list, fullyPurchasedCount: 1 };

    const next = clearAllPurchases(before);

    expect(next.fullyPurchasedCount).toBe(0);
    expect(next.totalItems).toBe(2);
    for (const category of next.categories) {
      for (const item of category.items) {
        expect(item.quantityPurchased).toBe(0);
        expect(item.status).toBe('not_purchased');
      }
    }
    expect(onlyRow(next, 'ingredient-onion').entries[0].quantityRequired).toBe(
      onlyRow(before, 'ingredient-onion').entries[0].quantityRequired,
    );
  });

  it('drops an entry that was only listed because it had been bought (nothing required any more)', () => {
    const list = makeList([
      makeItem({}),
      makeItem({ ingredientId: 'onion', ingredientName: 'Onion', unit: 'whole', quantityRequired: 0, quantityPurchased: 2, status: 'no_longer_needed' }),
    ]);

    const next = clearAllPurchases({ ...list, totalItems: 2 });

    expect(next.totalItems).toBe(1);
    expect(next.categories.flatMap((c) => c.items).map((i) => i.ingredientId)).toEqual(['garlic']);
  });
});

describe('applyRowPurchases', () => {
  it('sets a part amount: the row becomes more_needed and reads "1 clove more · have 2 clove"', () => {
    const list = makeList([makeItem({}), makeItem({ ingredientId: 'onion', ingredientName: 'Onion', unit: 'whole' })]);
    const row = onlyRow(list, 'ingredient-garlic');

    const next = applyRowPurchases(list, [{ entry: row.entries[0], quantityPurchased: 2 }]);
    const nextRow = onlyRow(next, 'ingredient-garlic');

    expect(nextRow.overallStatus).toBe('more_needed');
    expect(formatStillNeededText(nextRow)).toBe('1 clove more · have 2 clove');
    expect(next.fullyPurchasedCount).toBe(0);
    // The other row is untouched.
    expect(onlyRow(next, 'ingredient-onion').entries[0].quantityPurchased).toBe(0);
  });

  it('counts a row as purchased once the amount covers what is needed, and uncounts it again', () => {
    const list = makeList([makeItem({})]);
    const row = onlyRow(list, 'ingredient-garlic');

    const covered = applyRowPurchases(list, [{ entry: row.entries[0], quantityPurchased: 4 }]);
    expect(onlyRow(covered, 'ingredient-garlic').overallStatus).toBe('done');
    expect(covered.fullyPurchasedCount).toBe(1);

    const back = applyRowPurchases(covered, [{ entry: row.entries[0], quantityPurchased: 1 }]);
    expect(back.fullyPurchasedCount).toBe(0);
  });

  it('only touches the unit entry it was given on a row spanning two units', () => {
    const list = makeList([
      makeItem({ ingredientId: 'flour', ingredientName: 'Flour', unit: 'cup', quantityRequired: 2 }),
      makeItem({ ingredientId: 'flour', ingredientName: 'Flour', unit: 'g', quantityRequired: 300 }),
    ]);
    const row = onlyRow(list, 'ingredient-flour');
    const grams = row.entries.find((entry) => entry.unit === 'g')!;

    const next = applyRowPurchases(list, [{ entry: grams, quantityPurchased: 100 }]);
    const nextRow = onlyRow(next, 'ingredient-flour');

    expect(nextRow.entries.find((entry) => entry.unit === 'cup')!.quantityPurchased).toBe(0);
    expect(nextRow.entries.find((entry) => entry.unit === 'g')!.quantityPurchased).toBe(100);
    expect(nextRow.overallStatus).toBe('more_needed');
  });

  it('behaves as a check-off through rowPurchasesForToggle', () => {
    const list = makeList([makeItem({ quantityPurchased: 2, status: 'more_needed' })]);
    const row = onlyRow(list, 'ingredient-garlic');

    const checked = applyRowPurchases(list, rowPurchasesForToggle(row, true));
    expect(onlyRow(checked, 'ingredient-garlic').overallStatus).toBe('done');
    expect(checked.fullyPurchasedCount).toBe(1);

    const unchecked = applyRowPurchases(checked, rowPurchasesForToggle(row, false));
    expect(onlyRow(unchecked, 'ingredient-garlic').overallStatus).toBe('not_purchased');
    expect(unchecked.fullyPurchasedCount).toBe(0);
  });

  it('is undone by restoreRowEntries with the row as it was', () => {
    const list = makeList([makeItem({ quantityPurchased: 3, status: 'done' })]);
    const row = onlyRow(list, 'ingredient-garlic');

    const next = applyRowPurchases(list, [{ entry: row.entries[0], quantityPurchased: 1 }]);
    expect(restoreRowEntries(next, row.entries)).toEqual(list);
  });
});

describe('haveSheetEntries', () => {
  it('leaves out an entry that is neither needed nor bought', () => {
    const row: ShoppingRow = {
      ...onlyRow(makeList([makeItem({})]), 'ingredient-garlic'),
      entries: [
        { ingredientId: 'garlic', manualItemId: null, unit: 'clove', quantityRequired: 3, quantityPurchased: 0, status: 'not_purchased' },
        { ingredientId: 'garlic', manualItemId: null, unit: 'g', quantityRequired: 0, quantityPurchased: 0, status: 'not_purchased' },
        { ingredientId: 'garlic', manualItemId: null, unit: 'tsp', quantityRequired: 0, quantityPurchased: 1, status: 'no_longer_needed' },
      ],
    };
    expect(haveSheetEntries(row).map((entry) => entry.unit)).toEqual(['clove', 'tsp']);
  });
});

describe('onlyStillToBuy', () => {
  const list: ShoppingListResponse = {
    ...makeList([]),
    categories: [
      {
        category: 'produce',
        items: [
          makeItem({}),
          makeItem({ ingredientId: 'onion', ingredientName: 'Onion', quantityPurchased: 1, status: 'more_needed' }),
          makeItem({ ingredientId: 'lime', ingredientName: 'Lime', quantityPurchased: 3, status: 'done' }),
        ],
      },
      {
        category: 'dairy',
        items: [
          makeItem({ ingredientId: 'milk', ingredientName: 'Milk', quantityPurchased: 3, status: 'done' }),
          makeItem({ ingredientId: 'ghee', ingredientName: 'Ghee', quantityRequired: 0, quantityPurchased: 1, status: 'no_longer_needed' }),
        ],
      },
    ],
  };
  it('keeps unbought and part-bought rows, and drops categories with nothing left', () => {
    const names = onlyStillToBuy(buildShoppingRows(list)).map((group) => [
      group.category,
      group.rows.map((row) => row.ingredientName),
    ]);
    expect(names).toEqual([['produce', ['Garlic', 'Onion']]]);
  });
});

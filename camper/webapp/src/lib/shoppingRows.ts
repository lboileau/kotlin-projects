import type {
  ManualShoppingItemResponse,
  ShoppingItemSource,
  ShoppingItemStatus,
  ShoppingListCategoryResponse,
  ShoppingListItemResponse,
  ShoppingListResponse,
  ShoppingRecipeRef,
} from '../api/shopping';
import { formatShoppingQuantity } from './formatQuantity';

// Store-walk order; anything not listed sorts last, alphabetically among itself.
const CATEGORY_ORDER = [
  'produce',
  'bakery',
  'meat',
  'seafood',
  'dairy',
  'frozen',
  'pantry',
  'spice',
  'condiment',
  'other',
  'misc',
];

export interface ShoppingRowEntry {
  ingredientId: string | null;
  manualItemId: string | null;
  unit: string | null;
  quantityRequired: number;
  quantityPurchased: number;
  status: ShoppingItemStatus;
}

export interface ShoppingRow {
  /** Stable key across renders: `manual-{id}` or `ingredient-{id}`. */
  key: string;
  ingredientId: string | null;
  ingredientName: string | null;
  description: string | null;
  entries: ShoppingRowEntry[];
  overallStatus: ShoppingItemStatus;
  recipeRefs: ShoppingRecipeRef[];
  source: ShoppingItemSource;
  manualItemId: string | null;
}

export interface ShoppingCategoryGroup {
  category: string;
  rows: ShoppingRow[];
}

function toEntry(item: ShoppingListItemResponse): ShoppingRowEntry {
  return {
    ingredientId: item.ingredientId,
    manualItemId: item.manualItemId,
    unit: item.unit,
    quantityRequired: item.quantityRequired,
    quantityPurchased: item.quantityPurchased,
    status: item.status,
  };
}

function deriveOverallStatus(entries: ShoppingRowEntry[]): ShoppingItemStatus {
  if (entries.every((entry) => entry.status === 'done')) return 'done';
  if (entries.some((entry) => entry.status === 'done' || entry.status === 'more_needed')) return 'more_needed';
  if (entries.every((entry) => entry.status === 'no_longer_needed')) return 'no_longer_needed';
  return 'not_purchased';
}

/**
 * Merges items sharing an ingredientId (one row can span several unit
 * families) into a single display row; manual items are never merged.
 * Rows where quantityRequired and quantityPurchased are both 0 (orphaned
 * purchases the plan no longer needs at all) are dropped. Adapted from
 * the old app's `mergeItemsByIngredient` in MealPlanModal.tsx, updated
 * to use `usedInRecipeRefs` (deduped by id) instead of name strings.
 */
export function mergeShoppingItems(items: ShoppingListItemResponse[]): ShoppingRow[] {
  const grouped = new Map<string, ShoppingRow>();

  for (const item of items) {
    if (item.quantityRequired === 0 && item.quantityPurchased === 0) continue;

    const key = item.manualItemId ? `manual-${item.manualItemId}` : `ingredient-${item.ingredientId}`;
    const existing = grouped.get(key);

    if (existing && !item.manualItemId) {
      existing.entries.push(toEntry(item));
      for (const ref of item.usedInRecipeRefs) {
        if (!existing.recipeRefs.some((existingRef) => existingRef.id === ref.id)) {
          existing.recipeRefs.push(ref);
        }
      }
    } else {
      grouped.set(key, {
        key,
        ingredientId: item.ingredientId,
        ingredientName: item.ingredientName,
        description: item.description,
        entries: [toEntry(item)],
        overallStatus: item.status,
        recipeRefs: [...item.usedInRecipeRefs],
        source: item.source,
        manualItemId: item.manualItemId,
      });
    }
  }

  for (const row of grouped.values()) {
    row.overallStatus = deriveOverallStatus(row.entries);
  }

  return [...grouped.values()];
}

/**
 * "2 cups + 1 tbsp" style quantity text across all of a row's entries.
 * Goes through `lib/formatQuantity` (recipe-fraction glyphs, e.g. "1½")
 * so a quantity reads the same on the shopping list as it does on the
 * recipe page — these are server-scaled and can land on an awkward
 * decimal (0.3333, 2.6667) that a plain `toFixed(2)` would show as
 * "0.33"/"2.67" instead of "⅓"/"2⅔".
 */
export function formatQuantityText(row: ShoppingRow): string {
  return row.entries
    .filter((entry) => entry.quantityRequired > 0)
    .map((entry) => `${formatShoppingQuantity(entry.quantityRequired)}${entry.unit ? ` ${entry.unit}` : ''}`)
    .join(' + ');
}

/**
 * For a row that is partly in the basket: what is still to buy and what is
 * already bought, e.g. "1 clove more · have 2 clove", or "300 g more · have
 * 2 cup" when one ingredient is needed in two unit families and only one of
 * them has been bought. That happens when a recipe is added or the servings
 * go up after shopping started. Empty when nothing of the row has been
 * bought, or nothing is left to buy.
 */
export function formatStillNeededText(row: ShoppingRow): string {
  const withUnit = (quantity: number, unit: string | null) => `${formatShoppingQuantity(quantity)}${unit ? ` ${unit}` : ''}`;
  const needed = row.entries
    .filter((entry) => entry.quantityRequired > entry.quantityPurchased)
    .map((entry) => withUnit(entry.quantityRequired - entry.quantityPurchased, entry.unit));
  const have = row.entries
    .filter((entry) => entry.quantityPurchased > 0)
    .map((entry) => withUnit(entry.quantityPurchased, entry.unit));
  if (needed.length === 0 || have.length === 0) return '';
  return `${needed.join(' + ')} more · have ${have.join(' + ')}`;
}

function rowDisplayName(row: ShoppingRow): string {
  return row.ingredientName ?? row.description ?? '';
}

/**
 * Ingredient rows by name; then the items the user typed in themselves, in
 * the order they were added (the server returns them by creation time, and a
 * quick-add's optimistic row is appended) — so a new item always lands at
 * the very end, where the quick-add field is, instead of being filed
 * alphabetically somewhere in the middle. `sort` is stable, so returning 0
 * for two manual rows keeps their incoming order.
 */
function sortRows(rows: ShoppingRow[]): ShoppingRow[] {
  return rows.sort((a, b) => {
    const aManual = a.source === 'manual';
    const bManual = b.source === 'manual';
    if (aManual || bManual) return aManual === bManual ? 0 : aManual ? 1 : -1;
    return rowDisplayName(a).localeCompare(rowDisplayName(b));
  });
}

/**
 * Categories in store-walk order, each with its rows merged and ordered by
 * `sortRows` — a row's position never changes when it's checked off (or
 * marked no longer needed), by design: nothing should jump around the
 * list mid-shop just because you tapped it.
 */
export function buildShoppingRows(list: ShoppingListResponse): ShoppingCategoryGroup[] {
  return list.categories
    .map((category) => ({
      category: category.category,
      rows: sortRows(mergeShoppingItems(category.items)),
    }))
    .filter((group) => group.rows.length > 0)
    .sort((a, b) => categoryRank(a.category) - categoryRank(b.category));
}

/** Still something to buy: never bought, or bought and then needed more of. */
export function isStillToBuy(row: ShoppingRow): boolean {
  return row.overallStatus === 'not_purchased' || row.overallStatus === 'more_needed';
}

/**
 * The "hide bought" view: only rows with something left to buy, and only the
 * categories that still have any. `linger` keeps named rows (by key) a
 * moment longer — a row just ticked off pulses before it goes.
 */
export function onlyStillToBuy(groups: ShoppingCategoryGroup[], linger: ReadonlySet<string> = new Set()): ShoppingCategoryGroup[] {
  return groups
    .map((group) => ({ ...group, rows: group.rows.filter((row) => isStillToBuy(row) || linger.has(row.key)) }))
    .filter((group) => group.rows.length > 0);
}

function categoryRank(category: string): number {
  const index = CATEGORY_ORDER.indexOf(category.toLowerCase());
  return index === -1 ? CATEGORY_ORDER.length : index;
}

// ── Pure cache-editing helpers for optimistic updates ──────────────────
// All take and return a whole ShoppingListResponse so mutations can do a
// single queryClient.setQueryData call.

function matchesEntry(item: ShoppingListItemResponse, entry: ShoppingRowEntry): boolean {
  return entry.manualItemId
    ? entry.manualItemId === item.manualItemId
    : entry.ingredientId === item.ingredientId && entry.unit === item.unit;
}

/** Mirrors the server's `PurchaseStatus.derive` (meal-plan-calculator), for optimistic writes. */
export function derivePurchaseStatus(quantityRequired: number, quantityPurchased: number): ShoppingItemStatus {
  if (quantityRequired === 0 && quantityPurchased > 0) return 'no_longer_needed';
  if (quantityPurchased > 0 && quantityPurchased >= quantityRequired) return 'done';
  if (quantityPurchased > 0) return 'more_needed';
  return 'not_purchased';
}

/** How much of one entry of a row is bought (or already at home) — what a PATCH sends, one per entry. */
export interface RowPurchase {
  entry: ShoppingRowEntry;
  quantityPurchased: number;
}

/** Check-off: every entry of the row fully purchased, or none of it. */
export function rowPurchasesForToggle(row: ShoppingRow, checked: boolean): RowPurchase[] {
  return row.entries.map((entry) => ({ entry, quantityPurchased: checked ? entry.quantityRequired : 0 }));
}

/**
 * The whole list unbought — what the server's reset produces, for the
 * optimistic write. Every entry goes back to nothing purchased and
 * `not_purchased`; an entry that was only on the list because it had been
 * bought (`no_longer_needed`: nothing required any more) drops off it,
 * since with no purchase left the server won't list it either.
 */
export function clearAllPurchases(list: ShoppingListResponse): ShoppingListResponse {
  const categories = list.categories
    .map((category) => ({
      ...category,
      items: category.items
        .filter((item) => item.quantityRequired > 0)
        .map((item) => ({ ...item, quantityPurchased: 0, status: 'not_purchased' as const })),
    }))
    .filter((category) => category.items.length > 0);
  const totalItems = categories.reduce((count, category) => count + category.items.length, 0);
  return { ...list, categories, totalItems, fullyPurchasedCount: 0 };
}

/**
 * Sets the purchased quantity of a row's entries — a check-off
 * (`rowPurchasesForToggle`) or an amount typed into the "have" sheet —
 * re-deriving each status and adjusting `fullyPurchasedCount`.
 */
export function applyRowPurchases(list: ShoppingListResponse, purchases: RowPurchase[]): ShoppingListResponse {
  let purchasedDelta = 0;

  const categories = list.categories.map((category) => ({
    ...category,
    items: category.items.map((item) => {
      const purchase = purchases.find((candidate) => matchesEntry(item, candidate.entry));
      if (!purchase) return item;

      const wasDone = item.status === 'done';
      const status = derivePurchaseStatus(item.quantityRequired, purchase.quantityPurchased);
      if (wasDone !== (status === 'done')) purchasedDelta += status === 'done' ? 1 : -1;

      return { ...item, quantityPurchased: purchase.quantityPurchased, status };
    }),
  }));

  return { ...list, categories, fullyPurchasedCount: list.fullyPurchasedCount + purchasedDelta };
}

/** The entries of a row that the "have" sheet asks about: anything needed or already bought. */
export function haveSheetEntries(row: ShoppingRow): ShoppingRowEntry[] {
  return row.entries.filter((entry) => entry.quantityRequired > 0 || entry.quantityPurchased > 0);
}

function findOrCreateCategory(
  categories: ShoppingListCategoryResponse[],
  category: string,
): ShoppingListCategoryResponse[] {
  if (categories.some((existing) => existing.category === category)) return categories;
  return [...categories, { category, items: [] }];
}

/** Optimistic insert for quick-add, under Misc, before the server has assigned a real id. */
export function insertTempManualItem(list: ShoppingListResponse, description: string, tempId: string): ShoppingListResponse {
  const tempItem: ShoppingListItemResponse = {
    ingredientId: null,
    ingredientName: null,
    description,
    quantityRequired: 1,
    quantityPurchased: 0,
    unit: null,
    status: 'not_purchased',
    usedInRecipes: [],
    usedInRecipeRefs: [],
    source: 'manual',
    manualItemId: tempId,
  };

  const categories = findOrCreateCategory(list.categories, 'misc').map((category) =>
    category.category === 'misc' ? { ...category, items: [...category.items, tempItem] } : category,
  );

  return { ...list, categories, totalItems: list.totalItems + 1 };
}

/** Swaps a temp manual-item row for the server's real one, in place — no flicker, no reorder. */
export function replaceTempManualItem(
  list: ShoppingListResponse,
  tempId: string,
  created: ManualShoppingItemResponse,
): ShoppingListResponse {
  const realItem: ShoppingListItemResponse = {
    ingredientId: null,
    ingredientName: null,
    description: created.description,
    quantityRequired: created.quantity,
    quantityPurchased: created.quantityPurchased,
    unit: null,
    status: created.status,
    usedInRecipes: [],
    usedInRecipeRefs: [],
    source: 'manual',
    manualItemId: created.id,
  };

  const categories = list.categories.map((category) => ({
    ...category,
    items: category.items.map((item) => (item.manualItemId === tempId ? realItem : item)),
  }));

  return { ...list, categories };
}

/** Removes a manual item (temp or real) by id, adjusting totals. */
export function removeManualItemFromList(list: ShoppingListResponse, manualItemId: string): ShoppingListResponse {
  let removed = false;
  let removedWasDone = false;

  const categories = list.categories.map((category) => ({
    ...category,
    items: category.items.filter((item) => {
      if (item.manualItemId !== manualItemId) return true;
      removed = true;
      removedWasDone = item.status === 'done';
      return false;
    }),
  }));

  if (!removed) return list;

  return {
    ...list,
    categories,
    totalItems: list.totalItems - 1,
    fullyPurchasedCount: removedWasDone ? list.fullyPurchasedCount - 1 : list.fullyPurchasedCount,
  };
}

/**
 * Reverts just the entries belonging to one row back to a previous
 * quantityPurchased/status, applied on top of whatever the CURRENT list
 * is (not a stale whole-list snapshot) — so rolling back a failed toggle
 * can't undo a *different* row's change that succeeded in the meantime.
 * `fullyPurchasedCount` is adjusted by the delta this produces, never
 * overwritten outright.
 */
export function restoreRowEntries(list: ShoppingListResponse, previousEntries: ShoppingRowEntry[]): ShoppingListResponse {
  let purchasedDelta = 0;

  const categories = list.categories.map((category) => ({
    ...category,
    items: category.items.map((item) => {
      const previousEntry = previousEntries.find((entry) => matchesEntry(item, entry));
      if (!previousEntry) return item;

      const wasDone = item.status === 'done';
      const willBeDone = previousEntry.status === 'done';
      if (wasDone !== willBeDone) purchasedDelta += willBeDone ? 1 : -1;

      return { ...item, quantityPurchased: previousEntry.quantityPurchased, status: previousEntry.status };
    }),
  }));

  return { ...list, categories, fullyPurchasedCount: list.fullyPurchasedCount + purchasedDelta };
}

export interface RemovedManualItem {
  category: string;
  item: ShoppingListItemResponse;
}

/** Captures a manual item together with its category, before removing it — for a precise splice-back on rollback. */
export function captureManualItem(list: ShoppingListResponse, manualItemId: string): RemovedManualItem | null {
  for (const category of list.categories) {
    const item = category.items.find((existing) => existing.manualItemId === manualItemId);
    if (item) return { category: category.category, item };
  }
  return null;
}

/** Splices a previously removed manual item back into the CURRENT list. A no-op if it's already there (e.g. a refetch beat the rollback). */
export function reinsertManualItem(list: ShoppingListResponse, removed: RemovedManualItem): ShoppingListResponse {
  const alreadyPresent = list.categories.some((category) =>
    category.items.some((item) => item.manualItemId === removed.item.manualItemId),
  );
  if (alreadyPresent) return list;

  const categories = findOrCreateCategory(list.categories, removed.category).map((category) =>
    category.category === removed.category ? { ...category, items: [...category.items, removed.item] } : category,
  );

  return {
    ...list,
    categories,
    totalItems: list.totalItems + 1,
    fullyPurchasedCount: removed.item.status === 'done' ? list.fullyPurchasedCount + 1 : list.fullyPurchasedCount,
  };
}

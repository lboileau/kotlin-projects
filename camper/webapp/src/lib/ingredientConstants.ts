// Hardcoded from the DB CHECK constraints in
// `databases/camper-db/migrations/V013__create_ingredients.sql` — no
// endpoint exposes these lists. `meal` and `theme` are the free-text
// options the previous app offered; both columns are optional and
// capped at 20 characters server-side, which every option below
// respects.

export const CATEGORIES = [
  'produce',
  'dairy',
  'meat',
  'seafood',
  'pantry',
  'spice',
  'condiment',
  'frozen',
  'bakery',
  'other',
] as const;
export type Category = (typeof CATEGORIES)[number];

export const UNITS = [
  'g',
  'kg',
  'ml',
  'l',
  'tsp',
  'tbsp',
  'cup',
  'oz',
  'lb',
  'pieces',
  'whole',
  'bunch',
  'can',
  'clove',
  'pinch',
  'slice',
  'sprig',
] as const;
export type Unit = (typeof UNITS)[number];

export const MEALS = ['breakfast', 'lunch', 'dinner', 'snack', 'dessert', 'appetizer', 'side', 'drink'] as const;
export type Meal = (typeof MEALS)[number];

export const THEMES = [
  'chicken',
  'beef',
  'pork',
  'fish',
  'seafood',
  'vegetarian',
  'vegan',
  'pasta',
  'soup',
  'salad',
  'other',
] as const;
export type Theme = (typeof THEMES)[number];

export const DEFAULT_CATEGORY: Category = 'other';
export const DEFAULT_UNIT: Unit = 'pieces';

/** "produce" -> "Produce". Used for select options and badges. */
export function capitalize(value: string): string {
  return value.length === 0 ? value : value.charAt(0).toUpperCase() + value.slice(1);
}

// A scraped/LLM-suggested category or unit is free text — "vegetable",
// "Tbsp.", "tablespoons" — and the DB CHECK constraint on `ingredients`
// rejects anything outside the enums above. These map common synonyms
// (including plurals and a trailing period) onto a valid value, falling
// back to `other`/`pieces` rather than ever sending something the
// backend will reject.

const CATEGORY_SYNONYMS: Record<string, Category> = {
  vegetable: 'produce',
  vegetables: 'produce',
  veg: 'produce',
  fruit: 'produce',
  fruits: 'produce',
  spices: 'spice',
  herb: 'spice',
  herbs: 'spice',
  seasoning: 'spice',
  seasonings: 'spice',
  sauce: 'condiment',
  sauces: 'condiment',
  baking: 'pantry',
  grain: 'pantry',
  grains: 'pantry',
  'dry goods': 'pantry',
  fish: 'seafood',
  poultry: 'meat',
};

const UNIT_SYNONYMS: Record<string, Unit> = {
  tablespoon: 'tbsp',
  tablespoons: 'tbsp',
  teaspoon: 'tsp',
  teaspoons: 'tsp',
  cups: 'cup',
  gram: 'g',
  grams: 'g',
  kilogram: 'kg',
  kilograms: 'kg',
  milliliter: 'ml',
  milliliters: 'ml',
  millilitre: 'ml',
  millilitres: 'ml',
  liter: 'l',
  liters: 'l',
  litre: 'l',
  litres: 'l',
  ounce: 'oz',
  ounces: 'oz',
  pound: 'lb',
  pounds: 'lb',
  lbs: 'lb',
  piece: 'pieces',
  cloves: 'clove',
  cans: 'can',
  slices: 'slice',
  sprigs: 'sprig',
  pinches: 'pinch',
  bunches: 'bunch',
};

function normalizeToken(raw: string): string {
  return raw.trim().toLowerCase().replace(/\.$/, '');
}

export function normalizeCategory(raw: string | null | undefined): Category {
  if (!raw) return DEFAULT_CATEGORY;
  const token = normalizeToken(raw);
  if ((CATEGORIES as readonly string[]).includes(token)) return token as Category;
  if (CATEGORY_SYNONYMS[token]) return CATEGORY_SYNONYMS[token];
  if (token.endsWith('s')) {
    const singular = token.slice(0, -1);
    if ((CATEGORIES as readonly string[]).includes(singular)) return singular as Category;
    if (CATEGORY_SYNONYMS[singular]) return CATEGORY_SYNONYMS[singular];
  }
  return DEFAULT_CATEGORY;
}

export function normalizeUnit(raw: string | null | undefined): Unit {
  if (!raw) return DEFAULT_UNIT;
  const token = normalizeToken(raw);
  if ((UNITS as readonly string[]).includes(token)) return token as Unit;
  if (UNIT_SYNONYMS[token]) return UNIT_SYNONYMS[token];
  if (token.endsWith('s')) {
    const singular = token.slice(0, -1);
    if ((UNITS as readonly string[]).includes(singular)) return singular as Unit;
    if (UNIT_SYNONYMS[singular]) return UNIT_SYNONYMS[singular];
  }
  return DEFAULT_UNIT;
}

// A recipe-ingredient quantity display helper: no trailing zeros, and
// the handful of fractions a recipe is likely to use render as glyphs
// instead of long decimals (0.333333... -> ⅓).

// The kitchen fractions, in order, with the glyph each is written as.
// `parseQuantity` reads every one of these back.
const FRACTIONS: Array<[number, string]> = [
  [1 / 8, '⅛'],
  [1 / 6, '⅙'],
  [1 / 4, '¼'],
  [1 / 3, '⅓'],
  [3 / 8, '⅜'],
  [1 / 2, '½'],
  [5 / 8, '⅝'],
  [2 / 3, '⅔'],
  [3 / 4, '¾'],
  [5 / 6, '⅚'],
  [7 / 8, '⅞'],
];

const GLYPHS = FRACTIONS.map(([, glyph]) => glyph).join('');

const FRACTION_EPSILON = 0.02;

export function formatQuantity(quantity: number): string {
  // Defensive only — quantities are validated on the way in (see `parseQuantity`),
  // so this should never see a non-positive value in practice.
  if (!Number.isFinite(quantity) || quantity <= 0) return String(quantity);

  const whole = Math.floor(quantity);
  const fraction = quantity - whole;

  for (const [value, glyph] of FRACTIONS) {
    if (Math.abs(fraction - value) < FRACTION_EPSILON) {
      return whole > 0 ? `${whole}${glyph}` : glyph;
    }
  }

  const rounded = Math.round(quantity * 100) / 100;
  return String(rounded);
}

// The shopping list rounds up to these: what you can actually buy or measure.
const QUARTERS: Array<[number, string]> = [
  [1 / 4, '¼'],
  [1 / 2, '½'],
  [3 / 4, '¾'],
];

/**
 * A quantity written the way the shopping list reads: rounded UP to the
 * next quarter — "1.17 cup" is "1¼ cup", "0.17 cup" is "¼ cup", "10.92 tbsp"
 * is "11 tbsp". The list's amounts come from scaling recipes, so decimals
 * like these come up constantly and mean nothing at the shelf; rounding up
 * means the list never asks for less than the recipes need. An amount that
 * is already a quarter (within the same tolerance `formatQuantity` uses for
 * a fraction, so float noise like 2.001 stays "2") is kept.
 *
 * Recipe lines keep `formatQuantity`: those amounts were typed, and are
 * shown as typed.
 */
export function formatShoppingQuantity(quantity: number): string {
  if (!Number.isFinite(quantity) || quantity <= 0) return formatQuantity(quantity);

  const whole = Math.floor(quantity);
  const fraction = quantity - whole;

  if (fraction < FRACTION_EPSILON) return String(whole);
  for (const [value, glyph] of QUARTERS) {
    if (fraction <= value + FRACTION_EPSILON) return whole > 0 ? `${whole}${glyph}` : glyph;
  }
  return String(whole + 1);
}

// A number as `formatQuantity` writes it (digits, a decimal, a fraction
// glyph, or a whole with a glyph: "2", "0.17", "⅓", "1⅓").
const QUANTITY_RUN = new RegExp(`\\d+(?:\\.\\d+)?[${GLYPHS}]?|[${GLYPHS}]`, 'g');

export interface QuantityRun {
  text: string;
  /** True for the number itself; false for the unit and words around it. */
  isNumber: boolean;
  /** A number that is only a fraction glyph ("⅓"): one small character, which a row can enlarge. */
  isLoneFraction?: boolean;
}

const LONE_FRACTION = new RegExp(`^[${GLYPHS}]$`);

/**
 * Splits a quantity line ("1⅓ bunch", "1.17 cup + 5⅓ whole", "have 2 clove")
 * into runs, marking the numbers, so a row can set them in a heavier weight
 * than the unit. Words with digits in them are not a concern here: the
 * lines are built from `formatQuantity` and unit names.
 */
export function splitQuantityRuns(line: string): QuantityRun[] {
  const runs: QuantityRun[] = [];
  let last = 0;
  for (const match of line.matchAll(QUANTITY_RUN)) {
    const start = match.index ?? 0;
    if (start > last) runs.push({ text: line.slice(last, start), isNumber: false });
    runs.push({ text: match[0], isNumber: true, ...(LONE_FRACTION.test(match[0]) ? { isLoneFraction: true } : {}) });
    last = start + match[0].length;
  }
  if (last < line.length) runs.push({ text: line.slice(last), isNumber: false });
  return runs;
}

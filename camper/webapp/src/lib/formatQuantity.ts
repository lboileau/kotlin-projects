// A recipe-ingredient quantity display helper: no trailing zeros, and
// the handful of fractions a recipe is likely to use render as glyphs
// instead of long decimals (0.333333... -> ⅓).

const FRACTIONS: Array<[number, string]> = [
  [1 / 4, '¼'],
  [1 / 3, '⅓'],
  [1 / 2, '½'],
  [2 / 3, '⅔'],
  [3 / 4, '¾'],
];

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

// A number as `formatQuantity` writes it (digits, a decimal, a fraction
// glyph, or a whole with a glyph: "2", "0.17", "⅓", "1⅓").
const QUANTITY_RUN = /\d+(?:\.\d+)?[¼⅓½⅔¾]?|[¼⅓½⅔¾]/g;

export interface QuantityRun {
  text: string;
  /** True for the number itself; false for the unit and words around it. */
  isNumber: boolean;
}

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
    runs.push({ text: match[0], isNumber: true });
    last = start + match[0].length;
  }
  if (last < line.length) runs.push({ text: line.slice(last), isNumber: false });
  return runs;
}

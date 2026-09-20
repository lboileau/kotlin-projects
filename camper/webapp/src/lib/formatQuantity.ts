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

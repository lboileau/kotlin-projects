// A free-typed recipe-ingredient quantity is riskier to parse than
// `parseFloat` alone handles: `parseFloat("1/2")` silently returns `1`
// and `parseFloat("1,5")` silently returns `1` — both save the wrong
// number with no error. This accepts everything a person is likely to
// type — plain decimals with `.` or `,`, simple fractions ("1/2"), mixed
// numbers ("1 1/2"), and the unicode glyphs `formatQuantity` itself
// renders (½ ⅓ ¼ ¾ …, including a leading whole number like "1½") — and
// rejects everything else, returning `null` rather than guessing.

const FRACTION_GLYPHS: Record<string, number> = {
  '¼': 1 / 4,
  '½': 1 / 2,
  '¾': 3 / 4,
  '⅓': 1 / 3,
  '⅔': 2 / 3,
  '⅕': 1 / 5,
  '⅖': 2 / 5,
  '⅗': 3 / 5,
  '⅘': 4 / 5,
  '⅙': 1 / 6,
  '⅚': 5 / 6,
  '⅛': 1 / 8,
  '⅜': 3 / 8,
  '⅝': 5 / 8,
  '⅞': 7 / 8,
} as const;

const GLYPH_PATTERN = Object.keys(FRACTION_GLYPHS).join('');
const GLYPH_RE = new RegExp(`^(\\d+)?\\s*([${GLYPH_PATTERN}])$`);
const FRACTION_RE = /^(?:(\d+)\s+)?(\d+)\/(\d+)$/;
// ".5" counts: the number pad makes it the natural way to type a half.
const DECIMAL_RE = /^(\d+([.,]\d+)?|[.,]\d+)$/;

// Quantity fields show the number pad, which has no slash, so a third can
// only be typed as a decimal. "0.33" / "0.333" / "0.66" / "0.67" are read as the
// third they stand for: 0.33 cup scaled by three would otherwise come out
// as 0.99 instead of 1.
const THIRD_TOLERANCE = 0.01;

function snapThirds(value: number): number {
  const whole = Math.floor(value);
  const fraction = value - whole;
  for (const third of [1 / 3, 2 / 3]) {
    if (Math.abs(fraction - third) < THIRD_TOLERANCE) return whole + third;
  }
  return value;
}

function finalize(value: number): number | null {
  if (!Number.isFinite(value) || value <= 0) return null;
  return Math.round(value * 1000) / 1000;
}

export function parseQuantity(raw: string): number | null {
  const trimmed = raw.trim();
  if (!trimmed) return null;

  const glyphMatch = trimmed.match(GLYPH_RE);
  if (glyphMatch) {
    const whole = glyphMatch[1] ? parseInt(glyphMatch[1], 10) : 0;
    return finalize(whole + FRACTION_GLYPHS[glyphMatch[2]]);
  }

  const fractionMatch = trimmed.match(FRACTION_RE);
  if (fractionMatch) {
    const whole = fractionMatch[1] ? parseInt(fractionMatch[1], 10) : 0;
    const numerator = parseInt(fractionMatch[2], 10);
    const denominator = parseInt(fractionMatch[3], 10);
    if (denominator === 0) return null;
    return finalize(whole + numerator / denominator);
  }

  if (DECIMAL_RE.test(trimmed)) {
    return finalize(snapThirds(parseFloat(trimmed.replace(',', '.'))));
  }

  return null;
}

/**
 * For a "how much do I have" field, where none is a valid answer: blank
 * and any spelling of zero are 0, everything else as `parseQuantity`.
 */
export function parseQuantityOrZero(raw: string): number | null {
  const trimmed = raw.trim();
  if (trimmed === '') return 0;
  if (DECIMAL_RE.test(trimmed) && parseFloat(trimmed.replace(',', '.')) === 0) return 0;
  return parseQuantity(trimmed);
}

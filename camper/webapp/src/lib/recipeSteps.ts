/**
 * Pure helpers for a recipe's instructions (the ordered list of steps) and
 * for the recipe page's tabs.
 */

export type RecipeTab = 'ingredients' | 'instructions' | 'photos';

export const RECIPE_TABS: RecipeTab[] = ['ingredients', 'instructions', 'photos'];

/** The `?tab=` value, with anything unrecognised reading as the first tab. */
export function parseRecipeTab(value: string | null | undefined): RecipeTab {
  return value === 'instructions' || value === 'photos' ? value : 'ingredients';
}

/**
 * Leading numbering people type or paste into a step — "1.", "1)", "Step 3:",
 * "STEP TWO -" — which the list already shows. Kept as one pattern so the
 * editor and the save agree on what a step is.
 */
const LEADING_LABEL = /^\s*(?:step\s+(?:\d+|one|two|three|four|five|six|seven|eight|nine|ten)\s*[:.)\-–—]?|\d+\s*[.):\-–—])\s*/i;

/** One step, trimmed, with a leading "1." / "Step 1:" label removed. */
export function normaliseStep(text: string): string {
  return text.replace(LEADING_LABEL, '').trim();
}

/** The list as it is saved: each step normalised, blanks dropped, order kept. */
export function normaliseSteps(steps: readonly string[]): string[] {
  return steps.map(normaliseStep).filter((step) => step.length > 0);
}

/** Whether the edit form's steps differ from what the recipe has, after normalising both the same way. */
export function stepsChanged(original: readonly string[], current: readonly string[]): boolean {
  const a = normaliseSteps(original);
  const b = normaliseSteps(current);
  return a.length !== b.length || a.some((step, i) => step !== b[i]);
}

/**
 * Pasted text becomes steps: one per line (or per blank-line paragraph when
 * the text has them), so a whole method pasted into one field lands as a list.
 */
export function splitPastedSteps(text: string): string[] {
  const paragraphs = text.split(/\n\s*\n/).map((p) => p.trim()).filter(Boolean);
  const lines = paragraphs.length > 1 ? paragraphs : text.split(/\n/);
  return normaliseSteps(lines.map((line) => line.replace(/\s*\n\s*/g, ' ')));
}

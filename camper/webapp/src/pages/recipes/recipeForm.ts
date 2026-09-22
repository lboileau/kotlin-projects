import { parseQuantity } from '../../lib/parseQuantity';
import type { DraftLine, PendingLine } from './LinesEditor';

/** The recipe form's state, shared by the new and edit pages (`RecipeFormFields`). */
export interface RecipeFormValues {
  name: string;
  description: string;
  servings: number;
  webLink: string;
  meal: string;
  theme: string;
  lines: DraftLine[];
  /** The method as typed — normalised (numbering stripped, blanks dropped) on save. */
  steps: string[];
}

/**
 * Shared validation. Returns the error, or the lines to save — which include
 * a complete line still sitting in the add row (typed but never added with
 * +), so it is saved rather than silently dropped; an incomplete one is an
 * error, not a loss.
 */
export function validateRecipeForm(
  values: RecipeFormValues,
  pendingLine: PendingLine | null,
): { error: string } | { lines: DraftLine[] } {
  if (!values.name.trim()) return { error: 'Name is required.' };
  if (!Number.isFinite(values.servings) || values.servings < 1) return { error: 'Servings must be at least 1.' };
  if (values.lines.some((line) => parseQuantity(line.quantity) === null)) {
    return { error: 'Every ingredient needs a valid quantity (e.g. "2" or "1.5").' };
  }
  if (!pendingLine) return { lines: values.lines };
  if (parseQuantity(pendingLine.quantity) === null) {
    return { error: `Give "${pendingLine.ingredient.name}" a quantity and add it, or clear it, before saving.` };
  }
  return { lines: [...values.lines, { clientId: crypto.randomUUID(), ...pendingLine }] };
}

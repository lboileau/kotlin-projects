import type { RecipeFormValues } from './recipeForm';

/**
 * The new-recipe form, saved as it is typed. A hand-typed recipe used to be
 * lost to anything that left the page before Save — the ✕, the browser's
 * Back, a tap on a tab, a reload. It lives in sessionStorage so it survives
 * all of those for the life of the browser tab, and is restored (with a
 * "Discard" option) the next time the form opens. Cleared on a successful
 * save. Chosen over a "Discard changes?" dialog: nothing to answer, and it
 * also covers reloads and crashes, which a dialog can't.
 */
const KEY = 'meal-planner.new-recipe-draft';

export type NewRecipeDraft = RecipeFormValues;

export const EMPTY_DRAFT: NewRecipeDraft = {
  name: '',
  description: '',
  servings: 2,
  webLink: '',
  meal: '',
  theme: '',
  lines: [],
};

function hasContent(draft: NewRecipeDraft): boolean {
  return Boolean(draft.name.trim() || draft.description.trim() || draft.webLink.trim() || draft.lines.length > 0);
}

export function readNewRecipeDraft(): NewRecipeDraft | null {
  try {
    const raw = sessionStorage.getItem(KEY);
    if (!raw) return null;
    const parsed = { ...EMPTY_DRAFT, ...(JSON.parse(raw) as Partial<NewRecipeDraft>) };
    return Array.isArray(parsed.lines) && hasContent(parsed) ? parsed : null;
  } catch {
    return null;
  }
}

export function writeNewRecipeDraft(draft: NewRecipeDraft): void {
  try {
    if (hasContent(draft)) sessionStorage.setItem(KEY, JSON.stringify(draft));
    else sessionStorage.removeItem(KEY);
  } catch {
    // sessionStorage unavailable (private browsing, quota, etc.) — ignore.
  }
}

export function clearNewRecipeDraft(): void {
  try {
    sessionStorage.removeItem(KEY);
  } catch {
    // sessionStorage unavailable — ignore.
  }
}

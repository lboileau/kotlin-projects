import { itemKey, splitDictation } from './splitDictation';

export interface DictationChip {
  /** The item text exactly as it will be sent as the manual item's description. */
  text: string;
  /** itemKey(text) — the React key, the excluded-set key, and the dedup identity. */
  key: string;
  /** True when this item is in the excluded set and will NOT be added. */
  excluded: boolean;
}

export interface DictationSelection {
  /** Every parsed item in order, including excluded ones (they stay on screen). */
  chips: DictationChip[];
  /** The texts that will actually be POSTed, in order. */
  selected: string[];
  /** selected.length — the number in the button label. */
  count: number;
  /** "Add items" (0) | "Add 1 item" | "Add N items". */
  buttonLabel: string;
}

/** Pure. Parse the raw textarea string, apply the excluded set, and derive everything the sheet renders. */
export function buildDictationSelection(
  text: string,
  knownNames: string[],
  excluded: ReadonlySet<string>,
): DictationSelection {
  const chips: DictationChip[] = splitDictation(text, knownNames).map((itemText) => {
    const key = itemKey(itemText);
    return { text: itemText, key, excluded: excluded.has(key) };
  });

  const selected = chips.filter((chip) => !chip.excluded).map((chip) => chip.text);
  const count = selected.length;
  const buttonLabel =
    count === 0 ? 'Add items' : count === 1 ? 'Add 1 item' : `Add ${count} items`;

  return { chips, selected, count, buttonLabel };
}

/**
 * Pure. Whether the quick-add bar should show its several-items mode: the text parses
 * into two or more items and the user hasn't asked for it to be kept as one.
 */
export function isMultiItem(selection: DictationSelection, asOne: boolean): boolean {
  return !asOne && selection.chips.length >= 2;
}

/**
 * Pure. What the quick-add bar adds for its current text. Kept as one item (or parsing
 * to nothing, e.g. just "the"): the text exactly as written. Otherwise the parsed,
 * non-excluded items — so a single dictated "um, some milk" is added as "Milk".
 */
export function quickAddItems(text: string, selection: DictationSelection, asOne: boolean): string[] {
  const raw = text.trim().replace(/\s+/g, ' ');
  if (raw.length === 0) return [];
  if (asOne || selection.chips.length === 0) return [raw];
  return selection.selected;
}

/** Pure. Returns a NEW set with `key` toggled; the input set is never mutated. */
export function toggleExcluded(excluded: ReadonlySet<string>, key: string): Set<string> {
  const next = new Set(excluded);
  if (next.has(key)) {
    next.delete(key);
  } else {
    next.add(key);
  }
  return next;
}

/** Pure. "Added 1 item" | "Added N items". */
export function addedToastMessage(count: number): string {
  return count === 1 ? 'Added 1 item' : `Added ${count} items`;
}

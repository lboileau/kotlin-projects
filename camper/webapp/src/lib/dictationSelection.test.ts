import { describe, it, expect } from 'vitest';
import { itemKey } from './splitDictation';
import {
  addedToastMessage,
  buildDictationSelection,
  isMultiItem,
  quickAddItems,
  toggleExcluded,
} from './dictationSelection';

describe('buildDictationSelection', () => {
  it('parses every item, none excluded, selected in order, count and label match (D1)', () => {
    const result = buildDictationSelection('milk, eggs, bread', [], new Set());
    expect(result.chips.length).toBe(3);
    expect(result.chips.every((chip) => !chip.excluded)).toBe(true);
    expect(result.selected).toEqual(['Milk', 'Eggs', 'Bread']);
    expect(result.count).toBe(3);
    expect(result.buttonLabel).toBe('Add 3 items');
  });

  it('an excluded chip stays on screen but is left out of selected/count/label (D2)', () => {
    const result = buildDictationSelection('milk, eggs, bread', [], new Set(['eggs']));
    expect(result.chips.length).toBe(3);
    const eggsChip = result.chips.find((chip) => chip.text === 'Eggs');
    expect(eggsChip?.excluded).toBe(true);
    expect(result.selected).toEqual(['Milk', 'Bread']);
    expect(result.count).toBe(2);
    expect(result.buttonLabel).toBe('Add 2 items');
  });

  it('singular button label at count 1 (D3)', () => {
    const result = buildDictationSelection('milk', [], new Set());
    expect(result.count).toBe(1);
    expect(result.buttonLabel).toBe('Add 1 item');
  });

  it('empty text yields no chips, no selection, zero-count label (D4)', () => {
    const result = buildDictationSelection('', [], new Set());
    expect(result.chips).toEqual([]);
    expect(result.selected).toEqual([]);
    expect(result.count).toBe(0);
    expect(result.buttonLabel).toBe('Add items');
  });

  it('a fully-excluded chip stays visible but selects nothing (D5)', () => {
    const result = buildDictationSelection('milk', [], new Set(['milk']));
    expect(result.chips.length).toBe(1);
    expect(result.selected).toEqual([]);
    expect(result.count).toBe(0);
    expect(result.buttonLabel).toBe('Add items');
  });

  it('the excluded set is keyed by itemKey, not display text (D6)', () => {
    const result = buildDictationSelection('MILK', [], new Set(['milk']));
    expect(result.chips[0].excluded).toBe(true);
  });

  it('an excluded key matching no current chip is inert (D7)', () => {
    const result = buildDictationSelection('milk, eggs', [], new Set(['kumquat']));
    expect(result.count).toBe(2);
  });

  it('dedup flows through from splitDictation (D8)', () => {
    const result = buildDictationSelection('milk, milk', [], new Set());
    expect(result.chips.length).toBe(1);
  });

  it('knownNames is passed through to the parser (D9)', () => {
    const result = buildDictationSelection('milk eggs', ['Milk', 'Eggs'], new Set());
    expect(result.count).toBe(2);
  });

  it('every chip key is itemKey(chip.text) and all keys are unique (D10)', () => {
    const result = buildDictationSelection('milk, eggs, bread', [], new Set());
    for (const chip of result.chips) {
      expect(chip.key).toBe(itemKey(chip.text));
    }
    const keys = result.chips.map((chip) => chip.key);
    expect(new Set(keys).size).toBe(keys.length);
  });
});

describe('toggleExcluded', () => {
  it('adds a key to an empty set, returning a new, different, unmutated set (D11)', () => {
    const input = new Set<string>();
    const result = toggleExcluded(input, 'milk');
    expect(result).toEqual(new Set(['milk']));
    expect(input.size).toBe(0);
    expect(result).not.toBe(input);
  });

  it('removes an existing key, returning a new, different, unmutated set (D12)', () => {
    const input = new Set(['milk', 'eggs']);
    const result = toggleExcluded(input, 'milk');
    expect(result).toEqual(new Set(['eggs']));
    expect(input).toEqual(new Set(['milk', 'eggs']));
    expect(result).not.toBe(input);
  });
});

describe('addedToastMessage', () => {
  it('singular vs plural toast wording (D13)', () => {
    expect(addedToastMessage(1)).toBe('Added 1 item');
    expect(addedToastMessage(3)).toBe('Added 3 items');
  });
});

describe('isMultiItem', () => {
  it('is true once the text parses into two or more items', () => {
    expect(isMultiItem(buildDictationSelection('milk, eggs', [], new Set()), false)).toBe(true);
  });

  it('is false for a single item, and for empty text', () => {
    expect(isMultiItem(buildDictationSelection('milk', [], new Set()), false)).toBe(false);
    expect(isMultiItem(buildDictationSelection('', [], new Set()), false)).toBe(false);
  });

  it('stays true when chips are excluded — they are still on screen to be added back', () => {
    const excluded = new Set([itemKey('Milk'), itemKey('Eggs')]);
    expect(isMultiItem(buildDictationSelection('milk, eggs', [], excluded), false)).toBe(true);
  });

  it('is false when the user asked to keep the text as one item', () => {
    expect(isMultiItem(buildDictationSelection('salt and pepper', [], new Set()), true)).toBe(false);
  });
});

describe('quickAddItems', () => {
  it('adds the parsed, non-excluded items in order', () => {
    const excluded = new Set([itemKey('Eggs')]);
    const selection = buildDictationSelection('milk, eggs and some bread', [], excluded);
    expect(quickAddItems('milk, eggs and some bread', selection, false)).toEqual(['Milk', 'Bread']);
  });

  it('a single item is added as parsed, fillers gone', () => {
    const selection = buildDictationSelection('um some milk', [], new Set());
    expect(quickAddItems('um some milk', selection, false)).toEqual(['Milk']);
  });

  it('kept as one item: the text exactly as written, whitespace tidied', () => {
    const text = '  salt and   pepper ';
    const selection = buildDictationSelection(text, [], new Set());
    expect(quickAddItems(text, selection, true)).toEqual(['salt and pepper']);
  });

  it('text that parses to nothing is still added as written', () => {
    const selection = buildDictationSelection('the', [], new Set());
    expect(quickAddItems('the', selection, false)).toEqual(['the']);
  });

  it('every chip excluded adds nothing; empty text adds nothing', () => {
    const excluded = new Set([itemKey('Milk'), itemKey('Eggs')]);
    expect(quickAddItems('milk, eggs', buildDictationSelection('milk, eggs', [], excluded), false)).toEqual([]);
    expect(quickAddItems('   ', buildDictationSelection('   ', [], new Set()), false)).toEqual([]);
  });
});

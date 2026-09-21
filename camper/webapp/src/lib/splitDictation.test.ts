import { describe, it, expect } from 'vitest';
import { splitDictation, itemKey } from './splitDictation';

const CATALOGUE = ['Milk', 'Eggs', 'Bread', 'Cheese', 'Avocado', 'Butter', 'Salt', 'Pepper', 'Jam'];

describe('splitDictation', () => {
  it('comma splits (S1)', () => {
    expect(splitDictation('milk, eggs, bread', [])).toEqual(['Milk', 'Eggs', 'Bread']);
  });

  it('newline splits (S2)', () => {
    expect(splitDictation('milk\neggs\nbread', [])).toEqual(['Milk', 'Eggs', 'Bread']);
  });

  it('CRLF splits (S3)', () => {
    expect(splitDictation('milk\r\neggs', [])).toEqual(['Milk', 'Eggs']);
  });

  it('semicolon splits (S4)', () => {
    expect(splitDictation('milk; eggs', [])).toEqual(['Milk', 'Eggs']);
  });

  it('period splits; trailing period yields no empty item (S5)', () => {
    expect(splitDictation('Milk. Eggs. Bread.', [])).toEqual(['Milk', 'Eggs', 'Bread']);
  });

  it('decimal point does NOT split; unexplained unit abandons the run; digit-first capitalising is a no-op (S6)', () => {
    expect(splitDictation('1.5 litres of milk', CATALOGUE)).toEqual(['1.5 litres of milk']);
  });

  it('a real full stop still splits alongside a number (S7)', () => {
    expect(splitDictation('2 litres of milk. Bread', CATALOGUE)).toEqual([
      '2 litres of milk',
      'Bread',
    ]);
  });

  it('a comma is always a separator, never a decimal (S8, decision A2)', () => {
    expect(splitDictation('1,5 litres of milk', CATALOGUE)).toEqual(['1', '5 litres of milk']);
  });

  it('standalone "and" splits (S9)', () => {
    expect(splitDictation('milk and eggs', CATALOGUE)).toEqual(['Milk', 'Eggs']);
  });

  it('protection runs before the "and" split (S10)', () => {
    expect(splitDictation('mac and cheese', [...CATALOGUE, 'Mac and cheese'])).toEqual([
      'Mac and cheese',
    ]);
  });

  it('without the catalogue entry it splits — the handoff\'s paired case (S11)', () => {
    expect(splitDictation('mac and cheese', CATALOGUE)).toEqual(['Mac', 'Cheese']);
  });

  it('protection is per-occurrence, the remaining "and" still splits (S12)', () => {
    expect(splitDictation('salt and pepper and milk', [...CATALOGUE, 'Salt and pepper'])).toEqual([
      'Salt and pepper',
      'Milk',
    ]);
  });

  it('longest-name-first; shortest-first would give [\'Bread\',\'Butter and jam\'] (S13)', () => {
    expect(
      splitDictation('bread and butter and jam', [
        'Bread and butter',
        'Butter and jam',
        'Jam',
      ]),
    ).toEqual(['Bread and butter', 'Jam']);
  });

  it('equal-length "and"-names fall back to the lexicographic tie-break (§3.3, coverage gap)', () => {
    expect(
      splitDictation('bread and jam and jelly', ['Bread and jam', 'Jam and jelly']),
    ).toEqual(['Bread and jam', 'Jelly']);
  });

  it('"&" splits like "and" (S14)', () => {
    expect(splitDictation('salt & pepper', CATALOGUE)).toEqual(['Salt', 'Pepper']);
  });

  it('a name containing "&" is protected too (S15)', () => {
    expect(splitDictation('salt & pepper', [...CATALOGUE, 'Salt & pepper'])).toEqual([
      'Salt & pepper',
    ]);
  });

  it('iOS auto-punctuation: ", and " and the trailing period (S16)', () => {
    expect(splitDictation('Milk, eggs, and bread.', [])).toEqual(['Milk', 'Eggs', 'Bread']);
  });

  it('"and" inside "sand" does not split (S17)', () => {
    expect(splitDictation('sand and gravel', [])).toEqual(['Sand', 'Gravel']);
  });

  it('hyphenated "and" does not split; the run abandons on unknown words (S18)', () => {
    expect(splitDictation('hot-and-sour soup', [])).toEqual(['Hot-and-sour soup']);
  });

  it('separator-less run, fully covered (S19)', () => {
    expect(splitDictation('milk eggs bread', CATALOGUE)).toEqual(['Milk', 'Eggs', 'Bread']);
  });

  it('all-or-nothing: one unknown word leaves the whole segment as one item (S20)', () => {
    expect(splitDictation('milk eggs dragonfruit', CATALOGUE)).toEqual([
      'Milk eggs dragonfruit',
    ]);
  });

  it('empty catalogue (query loading or failed) — never guesses (S21)', () => {
    expect(splitDictation('milk eggs bread', [])).toEqual(['Milk eggs bread']);
  });

  it('trailing quantity word with no name abandons the run (S22)', () => {
    expect(splitDictation('milk two', CATALOGUE)).toEqual(['Milk two']);
  });

  it('a quantity prefix attaches to the name that follows it (S23)', () => {
    expect(splitDictation('milk two eggs', CATALOGUE)).toEqual(['Milk', 'Two eggs']);
  });

  it('a bare digit is recognised as a quantity word, same as a spelled-out number (coverage gap)', () => {
    expect(splitDictation('milk 2 eggs', CATALOGUE)).toEqual(['Milk', '2 eggs']);
  });

  it('quantities + plural tolerance + the handoff\'s stated example; items stay free text (S24)', () => {
    expect(splitDictation('two avocados and a dozen eggs', CATALOGUE)).toEqual([
      'Two avocados',
      'A dozen eggs',
    ]);
  });

  it('"couple" and "of" are quantity words (S25)', () => {
    expect(splitDictation('a couple of apples', [...CATALOGUE, 'Apple'])).toEqual([
      'A couple of apples',
    ]);
  });

  it('catalogue is plural (Eggs), text singular — symmetric plural tolerance (S26)', () => {
    expect(splitDictation('milk egg', CATALOGUE)).toEqual(['Milk', 'Egg']);
  });

  it('"-es" plural (S27)', () => {
    expect(splitDictation('milk tomatoes', [...CATALOGUE, 'Tomato'])).toEqual(['Milk', 'Tomatoes']);
  });

  it('multi-token known name inside a run (S28)', () => {
    expect(splitDictation('milk green beans', [...CATALOGUE, 'Green beans'])).toEqual([
      'Milk',
      'Green beans',
    ]);
  });

  it('longer known name wins over a shorter one sharing the same first token (§3.6 compareCandidates, coverage gap)', () => {
    expect(
      splitDictation('milk green beans', [...CATALOGUE, 'Green', 'Green beans']),
    ).toEqual(['Milk', 'Green beans']);
  });

  it('a multi-token candidate reached only via a plural probe on its first token is rejected (§3.6, coverage gap)', () => {
    expect(
      splitDictation('milk greens beans', [...CATALOGUE, 'Green beans']),
    ).toEqual(['Milk greens beans']);
  });

  it('a partial multi-token name does not match; the run abandons (S29)', () => {
    expect(splitDictation('milk green', [...CATALOGUE, 'Green beans'])).toEqual(['Milk green']);
  });

  it('decision A3: the run split applies to each piece produced by the "and" split (S30)', () => {
    expect(splitDictation('milk eggs and bread butter', CATALOGUE)).toEqual([
      'Milk',
      'Eggs',
      'Bread',
      'Butter',
    ]);
  });

  it('leading filler "also" / "plus" / "and" stripped (S31)', () => {
    expect(splitDictation('also milk, plus bread, and butter', [])).toEqual([
      'Milk',
      'Bread',
      'Butter',
    ]);
  });

  it('filler stripping repeats (S32)', () => {
    expect(splitDictation('also plus milk', [])).toEqual(['Milk']);
  });

  it('"some" is a filler and is stripped (S33, reversed at the owner\'s request)', () => {
    expect(splitDictation('some milk', CATALOGUE)).toEqual(['Milk']);
    expect(splitDictation('some dragonfruit', [])).toEqual(['Dragonfruit']);
  });

  it('spoken fillers and leading phrases are stripped from every item', () => {
    expect(
      splitDictation('um I need some milk, the eggs, and then maybe some bread', []),
    ).toEqual(['Milk', 'Eggs', 'Bread']);
    expect(splitDictation('we also need butter. Please get some dragonfruit', [])).toEqual([
      'Butter',
      'Dragonfruit',
    ]);
  });

  it('a leading phrase matches with the curly apostrophe iOS dictation types', () => {
    expect(splitDictation('don\u2019t forget the milk, don\'t forget eggs', [])).toEqual([
      'Milk',
      'Eggs',
    ]);
  });

  it('a phrase only matches whole words', () => {
    expect(splitDictation('i needles', [])).toEqual(['I needles']);
  });

  it('an item that is nothing but fillers produces nothing', () => {
    expect(splitDictation('um, some, i need', [])).toEqual([]);
  });

  it('a filler inside a separator-less run is skipped, not attached to an item', () => {
    expect(splitDictation('milk some eggs please', CATALOGUE)).toEqual(['Milk', 'Eggs']);
  });

  it('a filler after a quantity does not rescue the run; it stays one item', () => {
    expect(splitDictation('milk two some eggs', CATALOGUE)).toEqual(['Milk two some eggs']);
  });

  it('a filler in the middle of an unknown item is left alone', () => {
    expect(splitDictation('cream of the crop cereal', [])).toEqual(['Cream of the crop cereal']);
  });

  it('known cost, accepted by the owner: an item whose own first word is a filler loses it', () => {
    expect(splitDictation('get well card', [])).toEqual(['Well card']);
    expect(splitDictation('oh henry bars', [])).toEqual(['Henry bars']);
  });

  it('de-duplication (S34)', () => {
    expect(splitDictation('milk, eggs, milk', [])).toEqual(['Milk', 'Eggs']);
  });

  it('dedup is case-insensitive and keeps the FIRST occurrence verbatim (S35)', () => {
    expect(splitDictation('MILK, milk', [])).toEqual(['MILK']);
  });

  it('empty input (S36)', () => {
    expect(splitDictation('', [])).toEqual([]);
  });

  it('whitespace-only input (S37)', () => {
    expect(splitDictation('   ', [])).toEqual([]);
  });

  it('newlines only (S38)', () => {
    expect(splitDictation('\n\n', [])).toEqual([]);
  });

  it('separators only (S39)', () => {
    expect(splitDictation(',,,', [])).toEqual([]);
  });

  it('a lone conjunction produces nothing (S40)', () => {
    expect(splitDictation('and', [])).toEqual([]);
  });

  it('whitespace around separators (S41)', () => {
    expect(splitDictation('milk,    eggs', [])).toEqual(['Milk', 'Eggs']);
  });

  it('internal whitespace collapses; only the first character is capitalised (S42)', () => {
    expect(splitDictation('greek   yoghurt', [])).toEqual(['Greek yoghurt']);
  });

  it('outer trim (S43)', () => {
    expect(splitDictation('  milk  ', [])).toEqual(['Milk']);
  });

  it('protection and matching are case-insensitive; the user\'s own casing is preserved (S44)', () => {
    expect(splitDictation('MAC AND CHEESE', [...CATALOGUE, 'Mac and cheese'])).toEqual([
      'MAC AND CHEESE',
    ]);
  });

  it('blank catalogue entries are ignored, not treated as matching everything (S45)', () => {
    expect(splitDictation('Milk', ['', '   ', 'Milk'])).toEqual(['Milk']);
  });
});

describe('itemKey', () => {
  it('trims, collapses whitespace and lowercases', () => {
    expect(itemKey('  Two  Avocados ')).toBe('two avocados');
  });

  it('is case-insensitive: differently-cased inputs produce the same key', () => {
    expect(itemKey('Milk')).toBe(itemKey('MILK'));
  });
});

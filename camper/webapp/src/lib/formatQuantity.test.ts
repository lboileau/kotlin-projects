import { describe, expect, it } from 'vitest';
import { formatQuantity, splitQuantityRuns } from './formatQuantity';

describe('formatQuantity', () => {
  it('writes the common fractions as glyphs and drops trailing zeros', () => {
    expect(formatQuantity(1 / 3)).toBe('⅓');
    expect(formatQuantity(1.5)).toBe('1½');
    expect(formatQuantity(2)).toBe('2');
    expect(formatQuantity(0.17)).toBe('0.17');
  });
});

describe('splitQuantityRuns', () => {
  it('marks each number in a line, leaving units and words as plain runs', () => {
    expect(splitQuantityRuns('1⅓ bunch')).toEqual([
      { text: '1⅓', isNumber: true },
      { text: ' bunch', isNumber: false },
    ]);
    expect(splitQuantityRuns('1.17 cup + 5⅓ whole')).toEqual([
      { text: '1.17', isNumber: true },
      { text: ' cup + ', isNumber: false },
      { text: '5⅓', isNumber: true },
      { text: ' whole', isNumber: false },
    ]);
    expect(splitQuantityRuns('have ⅓ cup')).toEqual([
      { text: 'have ', isNumber: false },
      { text: '⅓', isNumber: true },
      { text: ' cup', isNumber: false },
    ]);
  });

  it('returns one plain run for a line with no number', () => {
    expect(splitQuantityRuns('some')).toEqual([{ text: 'some', isNumber: false }]);
    expect(splitQuantityRuns('')).toEqual([]);
  });
});

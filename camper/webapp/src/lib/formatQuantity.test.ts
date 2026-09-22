import { describe, expect, it } from 'vitest';
import { formatQuantity, formatShoppingQuantity, splitQuantityRuns } from './formatQuantity';

describe('formatQuantity', () => {
  it('writes the common fractions as glyphs and drops trailing zeros', () => {
    expect(formatQuantity(1 / 3)).toBe('⅓');
    expect(formatQuantity(1.5)).toBe('1½');
    expect(formatQuantity(2)).toBe('2');
    expect(formatQuantity(0.3)).toBe('0.3');
    expect(formatQuantity(1.17)).toBe('1⅙');
  });

  it('knows eighths and sixths too, so a typed "1/8 tsp" is not "0.13"', () => {
    expect(formatQuantity(1 / 8)).toBe('⅛');
    expect(formatQuantity(5 / 6)).toBe('⅚');
    expect(formatQuantity(2.875)).toBe('2⅞');
  });
});

describe('formatShoppingQuantity', () => {
  it('rounds up to the next quarter', () => {
    expect(formatShoppingQuantity(1.17)).toBe('1¼');
    expect(formatShoppingQuantity(0.17)).toBe('¼');
    expect(formatShoppingQuantity(0.3)).toBe('½');
    expect(formatShoppingQuantity(0.69)).toBe('¾');
    expect(formatShoppingQuantity(10.92)).toBe('11');
    expect(formatShoppingQuantity(1 / 3)).toBe('½');
  });

  it('keeps an amount that already is a quarter, including float noise around it', () => {
    expect(formatShoppingQuantity(2)).toBe('2');
    expect(formatShoppingQuantity(2.01)).toBe('2');
    expect(formatShoppingQuantity(1.5)).toBe('1½');
    expect(formatShoppingQuantity(0.75)).toBe('¾');
    expect(formatShoppingQuantity(0.2500001)).toBe('¼');
  });

  it('only ever writes quarters, halves and three-quarters', () => {
    for (let q = 0.01; q < 3; q += 0.07) {
      expect(formatShoppingQuantity(q)).toMatch(/^\d*[¼½¾]?$/);
    }
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
      { text: '⅓', isNumber: true, isLoneFraction: true },
      { text: ' cup', isNumber: false },
    ]);
  });

  it('flags only a number that is nothing but a fraction glyph', () => {
    expect(splitQuantityRuns('½ tsp')[0]).toEqual({ text: '½', isNumber: true, isLoneFraction: true });
    expect(splitQuantityRuns('1½ tsp')[0]).toEqual({ text: '1½', isNumber: true });
    expect(splitQuantityRuns('0.5 tsp')[0]).toEqual({ text: '0.5', isNumber: true });
  });

  it('returns one plain run for a line with no number', () => {
    expect(splitQuantityRuns('some')).toEqual([{ text: 'some', isNumber: false }]);
    expect(splitQuantityRuns('')).toEqual([]);
  });
});

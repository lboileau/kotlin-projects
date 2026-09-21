import { describe, it, expect } from 'vitest';
import { parseQuantity, parseQuantityOrZero } from './parseQuantity';

describe('parseQuantity', () => {
  it('reads decimals, fractions, mixed numbers and glyphs', () => {
    expect(parseQuantity('2')).toBe(2);
    expect(parseQuantity('1.5')).toBe(1.5);
    expect(parseQuantity('1,5')).toBe(1.5);
    expect(parseQuantity('1/2')).toBe(0.5);
    expect(parseQuantity('1 1/2')).toBe(1.5);
    expect(parseQuantity('1½')).toBe(1.5);
    expect(parseQuantity('.5')).toBe(0.5);
  });

  it('rejects zero, negatives and words', () => {
    expect(parseQuantity('0')).toBeNull();
    expect(parseQuantity('-1')).toBeNull();
    expect(parseQuantity('two')).toBeNull();
    expect(parseQuantity('1.')).toBeNull();
    expect(parseQuantity('')).toBeNull();
  });

  it('reads a decimal typed for a third as the third', () => {
    expect(parseQuantity('0.33')).toBe(0.333);
    expect(parseQuantity('0,333')).toBe(0.333);
    expect(parseQuantity('.33')).toBe(0.333);
    expect(parseQuantity('0.67')).toBe(0.667);
    expect(parseQuantity('0.66')).toBe(0.667);
    expect(parseQuantity('2.33')).toBe(2.333);
    // Close, but not a third.
    expect(parseQuantity('0.3')).toBe(0.3);
    expect(parseQuantity('0.35')).toBe(0.35);
  });
});

describe('parseQuantityOrZero', () => {
  it('reads blank and zero as none', () => {
    expect(parseQuantityOrZero('')).toBe(0);
    expect(parseQuantityOrZero('  ')).toBe(0);
    expect(parseQuantityOrZero('0')).toBe(0);
    expect(parseQuantityOrZero('0,0')).toBe(0);
  });

  it('reads amounts as parseQuantity does, and rejects the rest', () => {
    expect(parseQuantityOrZero('2')).toBe(2);
    expect(parseQuantityOrZero('1/2')).toBe(0.5);
    expect(parseQuantityOrZero('1½')).toBe(1.5);
    expect(parseQuantityOrZero('two')).toBeNull();
    expect(parseQuantityOrZero('-1')).toBeNull();
  });
});

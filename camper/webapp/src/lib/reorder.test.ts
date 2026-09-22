import { describe, expect, it } from 'vitest';
import { dropIndex, moveItem, shiftFor } from './reorder';

describe('moveItem', () => {
  it('moves an item down and up', () => {
    expect(moveItem(['a', 'b', 'c', 'd'], 0, 2)).toEqual(['b', 'c', 'a', 'd']);
    expect(moveItem(['a', 'b', 'c', 'd'], 3, 1)).toEqual(['a', 'd', 'b', 'c']);
  });

  it('returns a copy when nothing moves or the indexes are out of range', () => {
    const items = ['a', 'b'];
    expect(moveItem(items, 1, 1)).toEqual(items);
    expect(moveItem(items, 1, 1)).not.toBe(items);
    expect(moveItem(items, 0, 5)).toEqual(items);
    expect(moveItem(items, -1, 0)).toEqual(items);
  });
});

describe('dropIndex', () => {
  // Rows 60px tall, centres at 30, 90, 150, 210.
  const centres = [30, 90, 150, 210];

  it('keeps the index while the pointer is still over the dragged row', () => {
    expect(dropIndex(centres, 1, 90)).toBe(1);
    expect(dropIndex(centres, 1, 100)).toBe(1);
  });

  it('moves down once the pointer passes the next row centre, and up likewise', () => {
    expect(dropIndex(centres, 1, 151)).toBe(2);
    expect(dropIndex(centres, 1, 211)).toBe(3);
    expect(dropIndex(centres, 2, 29)).toBe(0);
    expect(dropIndex(centres, 2, 31)).toBe(1);
  });

  it('clamps above and below the list', () => {
    expect(dropIndex(centres, 0, -500)).toBe(0);
    expect(dropIndex(centres, 3, 9999)).toBe(3);
    expect(dropIndex([], 0, 10)).toBe(0);
  });
});

describe('shiftFor', () => {
  it('opens the gap between the origin and the target', () => {
    // dragging row 0 down to 2: rows 1 and 2 move up
    expect([0, 1, 2, 3].map((i) => shiftFor(i, 0, 2, 60))).toEqual([0, -60, -60, 0]);
    // dragging row 3 up to 1: rows 1 and 2 move down
    expect([0, 1, 2, 3].map((i) => shiftFor(i, 3, 1, 60))).toEqual([0, 60, 60, 0]);
  });

  it('shifts nothing when the target is the origin', () => {
    expect([0, 1, 2].map((i) => shiftFor(i, 1, 1, 60))).toEqual([0, 0, 0]);
  });
});

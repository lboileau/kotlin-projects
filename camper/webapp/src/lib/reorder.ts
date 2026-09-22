/** Pure helpers behind drag-to-reorder lists (the steps editor). */

/** The list with the item at `from` moved to `to`; unchanged when they are equal or out of range. */
export function moveItem<T>(items: readonly T[], from: number, to: number): T[] {
  if (from === to || from < 0 || to < 0 || from >= items.length || to >= items.length) return items.slice();
  const next = items.slice();
  const [moved] = next.splice(from, 1);
  next.splice(to, 0, moved);
  return next;
}

/**
 * Where the item being dragged (currently at `from`) should land, given the
 * vertical centre of every row as laid out before the drag and the pointer's
 * current y: the number of *other* rows whose centre the pointer is below.
 * Above every row → 0; below every row → last index.
 */
export function dropIndex(rowCentres: readonly number[], from: number, y: number): number {
  let index = 0;
  rowCentres.forEach((centre, i) => {
    if (i !== from && y > centre) index += 1;
  });
  return Math.min(Math.max(index, 0), Math.max(rowCentres.length - 1, 0));
}

/**
 * How far (in px) each row shifts while the row at `from` is being dragged
 * towards `to`, so the gap opens where it will land: rows between the two
 * move by the dragged row's height, the rest stay. The dragged row itself
 * follows the pointer and is not in this list's business.
 */
export function shiftFor(index: number, from: number, to: number, draggedHeight: number): number {
  if (index === from || from === to) return 0;
  if (from < to && index > from && index <= to) return -draggedHeight;
  if (from > to && index >= to && index < from) return draggedHeight;
  return 0;
}

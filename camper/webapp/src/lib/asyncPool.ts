// A tiny bounded-concurrency runner, in the shape of `Promise.allSettled`.
// Used by the recipe review "Accept all" flow so ingredient creation and
// line resolution don't fire dozens of requests at once, while still
// reporting each item's own success or failure (a few failures shouldn't
// sink the rest).

export interface PoolResult<T> {
  index: number;
  status: 'fulfilled' | 'rejected';
  value?: T;
  reason?: unknown;
}

export async function asyncPool<Item, T>(
  concurrency: number,
  items: Item[],
  worker: (item: Item, index: number) => Promise<T>,
  onSettle?: (result: PoolResult<T>) => void,
): Promise<PoolResult<T>[]> {
  const results: PoolResult<T>[] = new Array(items.length);
  let cursor = 0;

  async function runNext(): Promise<void> {
    const index = cursor++;
    if (index >= items.length) return;

    let result: PoolResult<T>;
    try {
      const value = await worker(items[index], index);
      result = { index, status: 'fulfilled', value };
    } catch (reason) {
      result = { index, status: 'rejected', reason };
    }
    results[index] = result;
    try {
      onSettle?.(result);
    } catch {
      // A throwing callback must not abort the pool (and skip the caller's
      // own trailing cleanup) — it already has its own result to inspect.
    }

    return runNext();
  }

  const workerCount = Math.min(concurrency, items.length);
  await Promise.all(Array.from({ length: workerCount }, () => runNext()));

  return results;
}

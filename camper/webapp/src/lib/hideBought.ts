const STORAGE_KEY = 'meal-planner.shopping-hide-bought';

/** Whether the shopping list shows only what is still to buy — remembered per device, across plans and visits. */
export function getHideBought(): boolean {
  try {
    return localStorage.getItem(STORAGE_KEY) === '1';
  } catch {
    return false;
  }
}

export function setHideBought(hide: boolean): void {
  try {
    if (hide) localStorage.setItem(STORAGE_KEY, '1');
    else localStorage.removeItem(STORAGE_KEY);
  } catch {
    // localStorage unavailable (private browsing, quota, etc.) — ignore.
  }
}

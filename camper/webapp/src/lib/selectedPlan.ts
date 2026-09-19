const STORAGE_KEY = 'meal-planner.selected-plan-id';

/** Last-selected plan id, remembered per device so the Shopping tab knows where to go. */
export function getSelectedPlanId(): string | null {
  try {
    return localStorage.getItem(STORAGE_KEY);
  } catch {
    return null;
  }
}

export function setSelectedPlanId(planId: string): void {
  try {
    localStorage.setItem(STORAGE_KEY, planId);
  } catch {
    // localStorage unavailable (private browsing, quota, etc.) — ignore.
  }
}

export function clearSelectedPlanId(): void {
  try {
    localStorage.removeItem(STORAGE_KEY);
  } catch {
    // localStorage unavailable — ignore.
  }
}

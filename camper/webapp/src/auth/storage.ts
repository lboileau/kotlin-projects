import type { AuthUser } from '../api/auth';

const STORAGE_KEY = 'meal-planner.auth-user';

export function readStoredUser(): AuthUser | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const parsed: unknown = JSON.parse(raw);
    if (parsed && typeof parsed === 'object' && 'id' in parsed && 'email' in parsed) {
      return parsed as AuthUser;
    }
    return null;
  } catch {
    return null;
  }
}

export function writeStoredUser(user: AuthUser | null): void {
  try {
    if (user) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(user));
    } else {
      localStorage.removeItem(STORAGE_KEY);
    }
  } catch {
    // localStorage unavailable (private browsing, quota, etc.) — ignore.
  }
}

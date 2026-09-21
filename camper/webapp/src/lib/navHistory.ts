/**
 * What the app remembers about where the user has been, beyond what the
 * History API will tell it: the page behind each history entry, keyed by the
 * entry's index (see `historyIndex.ts` for why the index and not
 * `location.key`). The History API can go back but cannot say what is back
 * there, and the header's back button needs to know, both to decide whether
 * to follow history and to name where it goes (`components/useBack.ts`).
 *
 * It lives in sessionStorage so it survives a reload of the tab, and it is
 * best effort: with storage unavailable the back button goes to the screen's
 * parent, which is always correct.
 */
const ENTRIES_KEY = 'meal-planner.nav-entries';
// Entries below the current index are all that is ever read; a long session
// only needs the recent ones.
const MAX_ENTRIES = 50;

export interface NavEntry {
  /** Pathname of the page, without any sheet child route on top of it. */
  pagePath: string;
  /** True when the entry's URL is a sheet open over `pagePath`. */
  isSheet: boolean;
}

function read<T>(key: string, fallback: T): T {
  try {
    const raw = sessionStorage.getItem(key);
    return raw ? (JSON.parse(raw) as T) : fallback;
  } catch {
    return fallback;
  }
}

function write(key: string, value: unknown): void {
  try {
    sessionStorage.setItem(key, JSON.stringify(value));
  } catch {
    // sessionStorage unavailable (private browsing, quota, etc.) — ignore.
  }
}

/**
 * Scroll positions, in memory only: by history entry (`location.key`) for
 * Back/Forward, and by page path for returning to a tab. Read and written by
 * `AppShell`.
 */
export const scrollByEntry = new Map<string, number>();
export const scrollByPage = new Map<string, number>();

let entries: Record<number, NavEntry> = read(ENTRIES_KEY, {});

export function recordNavEntry(index: number, entry: NavEntry): void {
  const next: Record<number, NavEntry> = {};
  for (const [key, value] of Object.entries(entries)) {
    const i = Number(key);
    // Anything past the current index is a forward entry a push has just
    // discarded; anything far behind it will never be read again.
    if (i < index && i >= index - MAX_ENTRIES) next[i] = value;
  }
  next[index] = entry;
  entries = next;
  write(ENTRIES_KEY, entries);
}

export function getNavEntry(index: number): NavEntry | null {
  return entries[index] ?? null;
}

/** On sign-out: the places remembered belong to the account that was signed in. */
export function clearNavMemory(): void {
  entries = {};
  scrollByEntry.clear();
  scrollByPage.clear();
  try {
    sessionStorage.removeItem(ENTRIES_KEY);
  } catch {
    // sessionStorage unavailable — ignore.
  }
}

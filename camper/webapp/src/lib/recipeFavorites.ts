/**
 * Everything about favourites that is worth testing, as pure functions: the
 * recipe list's `Show` filter predicate, the optimistic cache edits the
 * favourite toggle applies (and rolls back), and the wording of the recipe
 * page's "who favourited this" line. There is deliberately no DOM test
 * runner in this app, so logic lives here and the components that use it
 * stay thin shells a reviewer can check by reading.
 */

/** The recipe list's single-choice `Show` dropdown. `all` is the absent `?show=` param. */
export type RecipeShowFilter = 'all' | 'mine' | 'favourites' | 'my-favourites';

/** Anything with the fields the filter and the cache edits need — not `RecipeResponse` itself, so tests can build tiny fixtures. */
export interface FavouritableRecipe {
  id: string;
  createdBy: string;
  favoriteCount: number;
  favoritedByMe: boolean;
}

const SHOW_FILTERS: readonly RecipeShowFilter[] = ['all', 'mine', 'favourites', 'my-favourites'];

/**
 * Reads the `show` search param. Anything unrecognised — absent, the dead
 * `?mine=1` value, a hand-typed string — is `all`, so a stale or mangled URL
 * shows the whole library rather than nothing.
 */
export function parseShowFilter(raw: string | null): RecipeShowFilter {
  return SHOW_FILTERS.includes(raw as RecipeShowFilter) ? (raw as RecipeShowFilter) : 'all';
}

/**
 * The Show dropdown's predicate. ANDs with the meal chips and the search
 * box, which the page applies separately.
 *   all           — everything
 *   mine          — created by the signed-in user (no user: nothing)
 *   favourites    — anyone has favourited it (favoriteCount > 0)
 *   my-favourites — the signed-in user has favourited it (favoritedByMe)
 */
export function matchesShowFilter(
  recipe: FavouritableRecipe,
  filter: RecipeShowFilter,
  userId: string | undefined,
): boolean {
  switch (filter) {
    case 'mine':
      return Boolean(userId) && recipe.createdBy === userId;
    case 'favourites':
      return recipe.favoriteCount > 0;
    case 'my-favourites':
      // `favoritedByMe` is computed by the server for the X-User-Id caller,
      // so it is already "me" — but with nobody signed in there is no me.
      return Boolean(userId) && recipe.favoritedByMe;
    case 'all':
    default:
      return true;
  }
}

/**
 * The whole optimistic edit, as one guarded primitive: flip `favoritedByMe`
 * to `favorited` and move the count by one — but only when that is an actual
 * change. Applying it twice, or applying it to a row the server has already
 * brought to that state, returns the identical object, which is what makes
 * the rollback safe to run against the CURRENT cache rather than a snapshot
 * (see webapp/CLAUDE.md, "Rollbacks are targeted").
 *
 * The count clamps at zero so a stale cache can never render `-1`.
 */
export function setFavorite<T extends FavouritableRecipe>(recipe: T, favorited: boolean): T {
  if (recipe.favoritedByMe === favorited) return recipe;
  return {
    ...recipe,
    favoritedByMe: favorited,
    favoriteCount: favorited ? recipe.favoriteCount + 1 : Math.max(0, recipe.favoriteCount - 1),
  };
}

/** `setFavorite` mapped over a cached list, leaving every other row identically referenced. */
export function applyFavoriteToList<T extends FavouritableRecipe>(
  list: T[] | undefined,
  recipeId: string,
  favorited: boolean,
): T[] | undefined {
  if (!list) return list;
  return list.map((recipe) => (recipe.id === recipeId ? setFavorite(recipe, favorited) : recipe));
}

/** `setFavorite` on a cached detail, when it is the recipe in question. */
export function applyFavoriteToDetail<T extends FavouritableRecipe>(
  detail: T | undefined,
  recipeId: string,
  favorited: boolean,
): T | undefined {
  if (!detail || detail.id !== recipeId) return detail;
  return setFavorite(detail, favorited);
}

/** One row of `GET /api/recipes/{id}/favorites`, reduced to what the wording needs. */
export interface FavouritePerson {
  userId: string;
  /** Their username, already falling back to their email server-side. */
  username: string;
}

/**
 * The same guarded primitive as `setFavorite`, for the cached list of people
 * behind the `/recipes/:id/favourites` sheet (and the who-line above it):
 * add or remove the viewer's own row so tapping the heart shows "You" at
 * once rather than after a refetch. Guarded the same way — adding someone
 * already there, or removing someone who isn't, returns the identical array,
 * so a rollback against the CURRENT cache can never duplicate or over-remove.
 *
 * The list is oldest first, so the viewer's new row goes on the end.
 *
 * An `undefined` cache (the query never ran — it is only enabled once the
 * count is above zero) is seeded on an add, which is the 0 → 1 case: the
 * viewer is then genuinely the only person on the list, and seeding is what
 * makes that first tap read "You" instead of "1 person" until the fetch
 * lands. A remove leaves `undefined` alone: there is nothing to remove, and
 * inventing an empty list would claim knowledge we don't have.
 */
export function applyFavoriteToPeople<T extends FavouritePerson>(
  people: T[] | undefined,
  person: T,
  favorited: boolean,
): T[] | undefined {
  if (favorited) {
    if (!people) return [person];
    return people.some((p) => p.userId === person.userId) ? people : [...people, person];
  }
  if (!people) return people;
  return people.some((p) => p.userId === person.userId)
    ? people.filter((p) => p.userId !== person.userId)
    : people;
}

/** "1 favourite" / "3 favourites, including you" — the accessible text for a bare `♥ N`. */
export function favouritesCountLabel(count: number, includingMe = false): string {
  return `${count} favourite${count === 1 ? '' : 's'}${includingMe ? ', including you' : ''}`;
}

/** "1 person" / "3 people" — the count on its own, when no names are available. */
function countOnlyText(count: number): string {
  return `${count} ${count === 1 ? 'person' : 'people'}`;
}

/** What the recipe page's who-line shows, and what a screen reader announces for it. */
export interface FavouritedBySummary {
  /**
   * The visible text after the heart: `3 · You, Alice and 1 other`, or the
   * count alone (`3 people`) when no names are available.
   */
  text: string;
  /** The link's accessible name: `3 favourites · You, Alice and 1 other`, or `3 favourites`. */
  label: string;
}

/** At most this many names before the rest become "and N others". */
const MAX_NAMES = 2;

/**
 * Display names in the order the line reads them: the viewer first as "You"
 * (only when they are actually on the list), then everyone else in the order
 * the server gave, which is oldest first. Blank names are dropped rather
 * than rendered as a gap — they then simply count towards "and N others".
 */
function orderFavouriteNames(people: readonly FavouritePerson[], viewerId: string | undefined): string[] {
  const viewerIsHere = Boolean(viewerId) && people.some((p) => p.userId === viewerId);
  const others = people
    .filter((p) => p.userId !== viewerId)
    .map((p) => p.username?.trim())
    .filter((name): name is string => Boolean(name));
  return viewerIsHere ? ['You', ...others] : others;
}

/** `A`, `A and B`, `A, B and 1 other`, `A, B and 4 others`. */
function joinNames(shown: readonly string[], others: number): string {
  if (others > 0) {
    return `${shown.join(', ')} and ${others} other${others === 1 ? '' : 's'}`;
  }
  return shown.length === 2 ? `${shown[0]} and ${shown[1]}` : shown[0];
}

/**
 * The recipe page's "favourited by" line, as one pure function.
 *
 * `count` is authoritative — it comes from the recipe itself, which the
 * optimistic toggle keeps instantly true — while `people` comes from a
 * separate query that may be loading, may have failed, and may be a moment
 * behind. So the count drives the arithmetic: any shortfall between it and
 * the names in hand becomes "and N others", and with NO names at all (still
 * loading, failed, or a list that somehow came back empty) the line falls
 * back to the count alone. It never renders a broken or empty "who".
 */
export function formatFavouritedBy(
  count: number,
  people: readonly FavouritePerson[] | undefined,
  viewerId: string | undefined,
): FavouritedBySummary {
  const label = favouritesCountLabel(count);
  const names = count > 0 ? orderFavouriteNames(people ?? [], viewerId) : [];
  if (names.length === 0) {
    return { text: countOnlyText(count), label };
  }
  const shown = names.slice(0, MAX_NAMES);
  const others = Math.max(0, count - shown.length);
  const who = joinNames(shown, others);
  return { text: `${count} · ${who}`, label: `${label} · ${who}` };
}
